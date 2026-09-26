package com.backendprinciple.playground.lab;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Explains a single line of code without any AI: it recognises annotations, keywords, well-known
 * types, imports, YAML/properties keys, Maven tags, SQL and Dockerfile instructions using the
 * knowledge base in {@code lab/knowledge.json}. Fast, free and deterministic - no API keys needed.
 */
@Component
public class LineExplainer {

    public record Note(String term, String text) {
    }

    public record Explanation(int lineNumber, String code, String kind, String summary, String context,
                              List<Note> notes) {
    }

    record Knowledge(Map<String, String> annotations, Map<String, String> keywords, Map<String, String> symbols,
                     Map<String, String> imports, Map<String, String> yaml, Map<String, String> xml,
                     Map<String, String> artifacts, Map<String, String> sql, Map<String, String> docker) {
    }

    private static final Pattern ANNOTATION = Pattern.compile("@([A-Z][A-Za-z0-9]*)");
    private static final Pattern WORD = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern TYPE_DECL = Pattern.compile(
            "^\\s*(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed)\\s+)*(class|interface|record|enum|@interface)\\s+([A-Za-z_]\\w*)");
    private static final Pattern METHOD_DECL = Pattern.compile(
            "^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s+)*(?:(?:public|protected|private|static|final|abstract|synchronized|default)\\s+)*"
                    + "(?:<[^>]+>\\s+)?[\\w.<>\\[\\], ?]+\\s+([a-z_]\\w*)\\s*\\([^;]*$");
    private static final Pattern FIELD_DECL = Pattern.compile(
            "^\\s*(?:(?:private|protected|public|static|final|volatile|transient)\\s+)+[\\w.<>\\[\\], ?]+\\s+\\w+\\s*(=.*)?;\\s*$");
    private static final Pattern CONSTRUCTOR_DECL = Pattern.compile(
            "^\\s*(?:(?:public|protected|private)\\s+)?([A-Z]\\w*)\\s*\\([^;]*\\)\\s*(?:throws [\\w., ]+)?\\{?\\s*$");
    private static final Pattern THIS_ASSIGN = Pattern.compile("^\\s*this\\.(\\w+)\\s*=\\s*(\\w+)\\s*;\\s*$");
    private static final Pattern LOCAL_VAR = Pattern.compile(
            "^\\s*(?:final\\s+)?((?:[A-Z][\\w.]*(?:<[^=]*>)?(?:\\[\\])*)|var|int|long|double|boolean|char|byte|short|float)\\s+([a-z]\\w*)\\s*=\\s*(.+);\\s*$");
    private static final Pattern CALL = Pattern.compile("^\\s*([a-z]\\w*)\\.(\\w+)\\s*\\(");
    private static final Pattern DERIVED_QUERY = Pattern.compile("\\b(find|read|get|query|count|exists|delete|stream)(?:All|First\\d*|Top\\d*|Distinct)?By(\\w+?)\\s*\\(");
    private static final Pattern CONTROL = Pattern.compile("^\\s*(if|for|while|switch|catch|return|new|throw|else|try|do)\\b");
    private static final Pattern XML_TAG = Pattern.compile("<\\s*/?\\s*([A-Za-z][\\w.-]*)");
    private static final Pattern XML_VALUE = Pattern.compile("<artifactId>\\s*([^<\\s]+)\\s*</artifactId>");
    private static final Set<String> KEYWORD_PRIORITY = new LinkedHashSet<>(List.of(
            "package", "import", "class", "interface", "record", "enum", "extends", "implements", "throws", "return",
            "new", "throw", "try", "catch", "finally", "if", "else", "for", "while", "switch", "static", "final",
            "private", "protected", "public", "abstract", "void", "this", "super", "var", "instanceof", "synchronized",
            "volatile", "default", "null"));

    private final Knowledge kb;

    public LineExplainer(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("lab/knowledge.json").getInputStream()) {
            this.kb = objectMapper.readValue(in, Knowledge.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load lab/knowledge.json", e);
        }
    }

    /** @param lineNumber 1-based */
    public Explanation explain(List<String> lines, int lineNumber, String language) {
        String code = lines.get(lineNumber - 1);
        String trimmed = code.strip();
        return switch (language) {
            case "java", "kotlin" -> explainJava(lines, lineNumber, code, trimmed);
            case "yaml" -> explainYaml(lines, lineNumber, code, trimmed);
            case "properties" -> explainProperties(lineNumber, code, trimmed);
            case "xml" -> explainXml(lineNumber, code, trimmed);
            case "sql" -> explainSql(lineNumber, code, trimmed);
            case "dockerfile" -> explainDocker(lineNumber, code, trimmed);
            default -> generic(lineNumber, code, trimmed);
        };
    }

    // ---------------------------------------------------------------- Java

    private Explanation explainJava(List<String> lines, int n, String code, String t) {
        String context = javaContext(lines, n);
        if (t.isEmpty()) {
            return new Explanation(n, code, "blank", "Blank line - separates logical blocks so code is easier to read.", context, List.of());
        }
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
            String summary = t.startsWith("/**") ? "Javadoc comment - documentation that IDEs show on hover and tools turn into HTML docs."
                    : "Comment - ignored by the compiler. Good comments explain WHY, not what.";
            return new Explanation(n, code, "comment", summary, context, List.of());
        }
        if (t.matches("[})\\];,]+")) {
            return new Explanation(n, code, "close", "Closes the block/statement opened above" + (context.isEmpty() ? "." : " (" + context + ")."), context, List.of());
        }

        List<Note> notes = new ArrayList<>();
        String kind;
        String summary;

        if (t.startsWith("package ")) {
            kind = "package";
            String pkg = t.substring(8).replace(";", "").strip();
            summary = "This file belongs to package " + pkg + " - so it must live in the folder " + pkg.replace('.', '/') + "/.";
            addKeyword(notes, "package");
        } else if (t.startsWith("import ")) {
            kind = "import";
            String target = t.replaceFirst("^import\\s+(static\\s+)?", "").replace(";", "").strip();
            summary = (t.startsWith("import static") ? "Static import of " : "Imports ") + target
                    + " so this file can use it by its short name.";
            String lib = longestPrefix(kb.imports(), target);
            if (lib != null) {
                notes.add(new Note(libraryName(target), lib));
            }
        } else {
            Matcher type = TYPE_DECL.matcher(t);
            Matcher method = METHOD_DECL.matcher(t);
            Matcher ctor = CONSTRUCTOR_DECL.matcher(t);
            Matcher thisAssign = THIS_ASSIGN.matcher(t);
            Matcher local = LOCAL_VAR.matcher(t);
            Matcher call = CALL.matcher(t);
            String enclosingType = enclosingTypeName(lines, n);
            if (t.startsWith("@") && !t.contains("(") && t.split("\\s+").length == 1
                    || t.matches("@\\w+(\\(.*\\))?")) {
                kind = "annotation";
                summary = "Annotation - metadata that Spring/JPA/JUnit read to add behaviour to the element below it.";
            } else if (type.find()) {
                kind = "type";
                summary = "Declares the " + type.group(1) + " " + type.group(2) + ".";
            } else if (ctor.matches() && ctor.group(1).equals(enclosingType)) {
                kind = "constructor";
                summary = t.contains("()") ? "No-argument constructor of " + enclosingType + " (JPA and frameworks use it to create empty objects)."
                        : "Constructor of " + enclosingType + ". When Spring creates this bean it passes the required beans in here "
                        + "- constructor injection: dependencies are explicit, can be final, and tests can pass mocks directly.";
            } else if (thisAssign.matches()) {
                kind = "assignment";
                summary = "Stores the parameter " + thisAssign.group(2) + " in the field " + thisAssign.group(1)
                        + ". 'this.' distinguishes the field from the parameter with the same name.";
            } else if (!CONTROL.matcher(t).find() && method.find() && !t.contains("=")) {
                kind = "method";
                String name = method.group(1);
                summary = t.endsWith(";") ? "Method signature " + name + "(...) without a body (an interface/abstract method)."
                        : "Starts the method " + name + "(...). Its body runs between the braces.";
                if (lines.stream().anyMatch(l -> l.contains("interface ")) && t.endsWith(";")) {
                    summary = "Declares " + name + "(...) - in a Spring Data repository, Spring derives the SQL from this method name.";
                }
            } else if (FIELD_DECL.matcher(t).matches()) {
                kind = "field";
                summary = "Declares a field - state that each object of this class carries.";
            } else if (t.startsWith("return")) {
                kind = "return";
                summary = "Returns a value to the caller and ends the method.";
            } else if (local.matches()) {
                kind = "variable";
                summary = "Declares the local variable '" + local.group(2) + "' of type " + local.group(1)
                        + " and initialises it. It only exists until the end of this block.";
            } else if (call.find()) {
                kind = "call";
                summary = "Calls " + call.group(2) + "(...) on '" + call.group(1) + "'.";
            } else if (t.contains("->")) {
                kind = "lambda";
                summary = "Uses a lambda (->): a short, inline implementation of a functional interface.";
            } else {
                kind = "statement";
                summary = "A statement - one step of the method's logic.";
            }
        }

        Matcher ann = ANNOTATION.matcher(t);
        while (ann.find()) {
            String name = ann.group(1);
            String text = kb.annotations().get(name);
            notes.add(new Note("@" + name, text != null ? text
                    : "Annotation @" + name + ". Hover it in your IDE or search its docs to see what it does."));
        }
        Matcher derived = DERIVED_QUERY.matcher(t);
        if (derived.find()) {
            notes.add(new Note("derived query", DerivedQueryTranslator.describe(derived.group(1), derived.group(2))));
        }
        if (t.contains("::")) {
            notes.add(new Note("::", "Method reference - a shorthand lambda that calls an existing method, e.g. TaskResponse::from."));
        }
        if (t.contains("->") && !kind.equals("lambda")) {
            notes.add(new Note("->", "Lambda expression: an inline function passed as a value."));
        }
        Set<String> words = wordsOutsideStrings(t);
        for (String kw : KEYWORD_PRIORITY) {
            if (words.contains(kw) && !kind.equals("import") && !kind.equals("package")) {
                addKeyword(notes, kw);
            }
        }
        for (String w : words) {
            String s = kb.symbols().get(w);
            if (s != null && notes.stream().noneMatch(x -> x.term().equals(w))) {
                notes.add(new Note(w, s));
            }
        }
        return new Explanation(n, code, kind, summary, context, limit(notes, 8));
    }

    private void addKeyword(List<Note> notes, String kw) {
        String text = kb.keywords().get(kw);
        if (text != null && notes.stream().noneMatch(x -> x.term().equals(kw))) {
            notes.add(new Note(kw, text));
        }
    }

    /** Name of the nearest type declared above this line (good enough for constructor detection). */
    private static String enclosingTypeName(List<String> lines, int n) {
        for (int i = n - 1; i >= 0; i--) {
            Matcher m = TYPE_DECL.matcher(lines.get(i));
            if (m.find()) {
                return m.group(2);
            }
        }
        return "";
    }

    /** Walks upwards counting braces to find the enclosing method and type, e.g. "in method create() of class TaskService". */
    static String javaContext(List<String> lines, int n) {
        String method = null;
        String type = null;
        int depth = 0;
        for (int i = n - 2; i >= 0 && type == null; i--) {
            String l = stripStrings(lines.get(i));
            for (int c = l.length() - 1; c >= 0; c--) {
                char ch = l.charAt(c);
                if (ch == '}') {
                    depth++;
                } else if (ch == '{') {
                    depth--;
                }
            }
            if (depth < 0) {
                Matcher tm = TYPE_DECL.matcher(l);
                Matcher mm = METHOD_DECL.matcher(l);
                if (tm.find()) {
                    type = tm.group(1) + " " + tm.group(2);
                } else if (method == null && !CONTROL.matcher(l).find() && mm.find()) {
                    method = mm.group(1) + "()";
                } else if (method == null && CONSTRUCTOR_DECL.matcher(l).matches()) {
                    Matcher cm = CONSTRUCTOR_DECL.matcher(l);
                    cm.matches();
                    method = "the constructor " + cm.group(1) + "()";
                } else if (method == null) {
                    // opening brace on its own line: the signature is on a line above
                    for (int j = i; j >= Math.max(0, i - 3); j--) {
                        Matcher m2 = METHOD_DECL.matcher(lines.get(j));
                        if (!CONTROL.matcher(lines.get(j)).find() && m2.find()) {
                            method = m2.group(1) + "()";
                            break;
                        }
                    }
                }
                depth = 0;
            }
        }
        if (method != null && type != null) {
            return "inside " + method + " of " + type;
        }
        return type != null ? "inside " + type : "";
    }

    // ---------------------------------------------------------------- YAML / properties

    private Explanation explainYaml(List<String> lines, int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("#")) {
            return new Explanation(n, code, t.isEmpty() ? "blank" : "comment",
                    t.isEmpty() ? "Blank line." : "YAML comment - ignored by Spring.", "", List.of());
        }
        String key = yamlPath(lines, n);
        List<Note> notes = new ArrayList<>();
        String exact = key == null ? null : kb.yaml().get(key);
        if (exact != null) {
            notes.add(new Note(key, exact));
        } else if (key != null) {
            String prefix = longestPrefix(kb.yaml(), key);
            if (prefix != null) {
                notes.add(new Note(key, prefix));
            }
        }
        if (t.contains("${")) {
            notes.add(new Note("${...}", "Placeholder: the value comes from an environment variable or another property; the part after ':' is the default."));
        }
        String summary = key == null ? "YAML line." : t.endsWith(":")
                ? "Starts the section '" + key + "'. Indented lines below belong to it (indentation matters in YAML)."
                : "Sets the property " + key + ". In code you could read it with @Value(\"${" + key + "}\").";
        return new Explanation(n, code, "config", summary, "", notes);
    }

    /** Reconstructs the dotted key (spring.datasource.url) from indentation of the lines above. */
    static String yamlPath(List<String> lines, int n) {
        String line = lines.get(n - 1);
        String t = line.strip();
        if (t.startsWith("- ")) {
            t = t.substring(2);
        }
        int colon = t.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        parts.add(t.substring(0, colon).strip());
        int indent = indentOf(line);
        for (int i = n - 2; i >= 0 && indent > 0; i--) {
            String prev = lines.get(i);
            String pt = prev.strip();
            if (pt.isEmpty() || pt.startsWith("#") || pt.startsWith("- ")) {
                continue;
            }
            int pi = indentOf(prev);
            if (pi < indent && pt.endsWith(":") || pi < indent && pt.contains(":")) {
                parts.addFirst(pt.substring(0, pt.indexOf(':')).strip());
                indent = pi;
            }
        }
        return String.join(".", parts);
    }

    private static int indentOf(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private Explanation explainProperties(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("#")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "Comment.", "", List.of());
        }
        int eq = t.indexOf('=');
        String key = eq > 0 ? t.substring(0, eq).strip() : t;
        List<Note> notes = new ArrayList<>();
        String text = kb.yaml().getOrDefault(key, longestPrefix(kb.yaml(), key));
        if (text != null) {
            notes.add(new Note(key, text));
        }
        return new Explanation(n, code, "config", "Sets the property " + key + ".", "", notes);
    }

    // ---------------------------------------------------------------- XML (Maven)

    private Explanation explainXml(int n, String code, String t) {
        List<Note> notes = new ArrayList<>();
        Matcher tag = XML_TAG.matcher(t);
        Set<String> seen = new LinkedHashSet<>();
        while (tag.find()) {
            seen.add(tag.group(1));
        }
        for (String s : seen) {
            String text = kb.xml().get(s);
            if (text != null) {
                notes.add(new Note("<" + s + ">", text));
            }
        }
        Matcher artifact = XML_VALUE.matcher(t);
        if (artifact.find()) {
            String a = artifact.group(1);
            String text = kb.artifacts().get(a);
            if (text != null) {
                notes.addFirst(new Note(a, text));
            }
        }
        String summary = t.isEmpty() ? "Blank line." : t.startsWith("<!--") ? "XML comment."
                : seen.isEmpty() ? "XML content." : "Maven build configuration: <" + seen.iterator().next() + ">.";
        return new Explanation(n, code, "build", summary, "", notes);
    }

    // ---------------------------------------------------------------- SQL / Docker / other

    private Explanation explainSql(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("--")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "SQL comment.", "", List.of());
        }
        String upper = t.toUpperCase(Locale.ROOT);
        List<Note> notes = new ArrayList<>();
        kb.sql().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> -e.getKey().length()))
                .filter(e -> Pattern.compile("\\b" + Pattern.quote(e.getKey()) + "\\b").matcher(upper).find())
                .filter(e -> notes.stream().noneMatch(x -> x.term().contains(e.getKey())))
                .forEach(e -> notes.add(new Note(e.getKey(), e.getValue())));
        String summary = upper.startsWith("CREATE TABLE") ? "Creates a table."
                : upper.matches("^\\w+\\s+\\w+.*") && !upper.startsWith("CREATE") && !upper.startsWith("INSERT")
                ? "Defines a column (name, type and constraints) or a table constraint." : "SQL statement.";
        return new Explanation(n, code, "sql", summary, "", limit(notes, 6));
    }

    private Explanation explainDocker(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("#")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "Dockerfile comment.", "", List.of());
        }
        String instruction = t.split("\\s+")[0].toUpperCase(Locale.ROOT);
        String text = kb.docker().get(instruction);
        return new Explanation(n, code, "docker", "Dockerfile instruction " + instruction + ".", "",
                text == null ? List.of() : List.of(new Note(instruction, text)));
    }

    private Explanation generic(int n, String code, String t) {
        return new Explanation(n, code, "text", t.isEmpty() ? "Blank line." : "Type this line exactly as shown.", "", List.of());
    }

    // ---------------------------------------------------------------- helpers

    private static String longestPrefix(Map<String, String> map, String key) {
        String best = null;
        for (String k : map.keySet()) {
            if ((key.equals(k) || key.startsWith(k + ".")) && (best == null || k.length() > best.length())) {
                best = k;
            }
        }
        return best == null ? null : map.get(best);
    }

    private static String libraryName(String importTarget) {
        int lastDot = importTarget.lastIndexOf('.');
        return lastDot > 0 ? importTarget.substring(lastDot + 1) : importTarget;
    }

    private static Set<String> wordsOutsideStrings(String line) {
        Set<String> words = new LinkedHashSet<>();
        Matcher m = WORD.matcher(stripStrings(line));
        while (m.find()) {
            words.add(m.group(1));
        }
        return words;
    }

    static String stripStrings(String line) {
        return line.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"").replaceAll("//.*$", "");
    }

    private static List<Note> limit(List<Note> notes, int max) {
        Map<String, Note> unique = new LinkedHashMap<>();
        notes.forEach(note -> unique.putIfAbsent(note.term(), note));
        return unique.values().stream().limit(max).toList();
    }
}
