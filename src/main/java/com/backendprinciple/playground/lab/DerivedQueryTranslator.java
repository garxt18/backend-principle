package com.backendprinciple.playground.lab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Explains Spring Data "derived query" method names by translating them to the SQL Spring generates,
 * e.g. {@code findByTitleContainingIgnoreCase} -> {@code SELECT * ... WHERE lower(title) LIKE lower('%' || ? || '%')}.
 * Covers the common keywords; it is a teaching aid, not a full parser.
 */
public final class DerivedQueryTranslator {

    /** Longest keywords first so "GreaterThanEqual" wins over "GreaterThan". */
    private static final Map<String, String> OPERATORS = new LinkedHashMap<>();

    static {
        OPERATORS.put("GreaterThanEqual", "%s >= ?");
        OPERATORS.put("LessThanEqual", "%s <= ?");
        OPERATORS.put("GreaterThan", "%s > ?");
        OPERATORS.put("LessThan", "%s < ?");
        OPERATORS.put("Between", "%s BETWEEN ? AND ?");
        OPERATORS.put("IsNotNull", "%s IS NOT NULL");
        OPERATORS.put("NotNull", "%s IS NOT NULL");
        OPERATORS.put("IsNull", "%s IS NULL");
        OPERATORS.put("Null", "%s IS NULL");
        OPERATORS.put("NotIn", "%s NOT IN (?)");
        OPERATORS.put("In", "%s IN (?)");
        OPERATORS.put("NotContaining", "%s NOT LIKE '%%' || ? || '%%'");
        OPERATORS.put("Containing", "%s LIKE '%%' || ? || '%%'");
        OPERATORS.put("StartingWith", "%s LIKE ? || '%%'");
        OPERATORS.put("EndingWith", "%s LIKE '%%' || ?");
        OPERATORS.put("NotLike", "%s NOT LIKE ?");
        OPERATORS.put("Like", "%s LIKE ?");
        OPERATORS.put("Before", "%s < ?");
        OPERATORS.put("After", "%s > ?");
        OPERATORS.put("IsTrue", "%s = TRUE");
        OPERATORS.put("True", "%s = TRUE");
        OPERATORS.put("IsFalse", "%s = FALSE");
        OPERATORS.put("False", "%s = FALSE");
        OPERATORS.put("Not", "%s <> ?");
        OPERATORS.put("Is", "%s = ?");
        OPERATORS.put("Equals", "%s = ?");
    }

    private DerivedQueryTranslator() {
    }

    public static String describe(String verb, String criteria) {
        String orderBy = null;
        int orderIdx = criteria.indexOf("OrderBy");
        if (orderIdx >= 0) {
            orderBy = criteria.substring(orderIdx + "OrderBy".length());
            criteria = criteria.substring(0, orderIdx);
        }
        List<String> conditions = new ArrayList<>();
        for (String orPart : criteria.split("Or(?=[A-Z])")) {
            List<String> ands = new ArrayList<>();
            for (String part : orPart.split("And(?=[A-Z])")) {
                if (!part.isEmpty()) {
                    ands.add(condition(part));
                }
            }
            conditions.add(String.join(" AND ", ands));
        }
        String where = String.join(" OR ", conditions);
        String select = switch (verb) {
            case "count" -> "SELECT COUNT(*)";
            case "exists" -> "SELECT 1 ... LIMIT 1 (true if a row exists)";
            case "delete" -> "DELETE";
            default -> "SELECT *";
        };
        String sql = select + " FROM <table> WHERE " + where
                + (orderBy == null || orderBy.isEmpty() ? "" : " ORDER BY " + orderClause(orderBy));
        return "Spring Data reads this method name and generates the query for you: " + sql
                + ". No implementation needed - misspell a property name and the app fails at startup.";
    }

    private static String condition(String part) {
        boolean ignoreCase = part.endsWith("IgnoreCase") || part.endsWith("AllIgnoreCase");
        if (ignoreCase) {
            part = part.replaceAll("(All)?IgnoreCase$", "");
        }
        for (var op : OPERATORS.entrySet()) {
            if (part.endsWith(op.getKey()) && part.length() > op.getKey().length()) {
                String column = column(part.substring(0, part.length() - op.getKey().length()));
                String c = op.getValue().formatted(ignoreCase ? "lower(" + column + ")" : column);
                return ignoreCase ? c.replace("?", "lower(?)") : c;
            }
        }
        String column = column(part);
        return ignoreCase ? "lower(" + column + ") = lower(?)" : column + " = ?";
    }

    private static String orderClause(String orderBy) {
        String direction = orderBy.endsWith("Desc") ? " DESC" : "";
        String prop = orderBy.replaceAll("(Asc|Desc)$", "");
        return column(prop) + direction;
    }

    /** "DueDate" -> "due_date" (Spring Boot's default physical naming strategy). */
    static String column(String property) {
        return property.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
