package com.backendprinciple.playground.lab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A light, regex-based model of one Java source file: its imports, the main type, its fields and the
 * line range of every method. Good enough to answer "which method am I in?", "where is this import
 * used?" and "what does this method do, step by step?" - without a real parser.
 */
final class JavaFileModel {

    static final Pattern TYPE_DECL = Pattern.compile(
            "^\\s*(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed)\\s+)*(class|interface|record|enum|@interface)\\s+([A-Za-z_]\\w*)");
    static final Pattern METHOD_DECL = Pattern.compile(
            "^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s+)*(?:(?:public|protected|private|static|final|abstract|synchronized|default)\\s+)*"
                    + "(?:<[^>]+>\\s+)?([\\w.<>\\[\\], ?]+)\\s+([a-z_]\\w*)\\s*\\(([^;]*);?\\s*$");
    static final Pattern CONSTRUCTOR_DECL = Pattern.compile(
            "^\\s*(?:(?:public|protected|private)\\s+)?([A-Z]\\w*)\\s*\\(([^;]*)\\)\\s*(?:throws [\\w., ]+)?\\{?\\s*$");
    static final Pattern FIELD_DECL = Pattern.compile(
            "^\\s*((?:(?:private|protected|public|static|final|volatile|transient)\\s+)+)([\\w.<>\\[\\], ?]+?)\\s+(\\w+)\\s*(=.*)?;\\s*$");
    static final Pattern CONTROL = Pattern.compile("^\\s*(if|for|while|switch|catch|return|new|throw|else|try|do)\\b");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(static\\s+)?([\\w.]+)(\\.\\*)?\\s*;");
    private static final Pattern MAPPING = Pattern.compile(
            "@(Get|Post|Put|Patch|Delete|Request)Mapping\\s*(?:\\(\\s*(?:(?:value|path)\\s*=\\s*)?\"([^\"]*)\")?");
    private static final Pattern FIELD_CALL = Pattern.compile("\\b([a-z]\\w*)\\.(\\w+)\\s*\\(");

    record Method(String name, String returnType, String params, int startLine, int endLine,
                  List<String> annotations, boolean constructor, boolean abstractMethod) {
        /** HTTP verb + path when this is a controller endpoint, e.g. "GET /api/tasks/{id}". */
        String endpoint(String basePath) {
            for (String a : annotations) {
                Matcher m = MAPPING.matcher(a);
                if (m.find() && !m.group(1).equals("Request")) {
                    String path = m.group(2) == null ? "" : m.group(2);
                    return m.group(1).toUpperCase(java.util.Locale.ROOT) + " " + joinPath(basePath, path);
                }
            }
            return null;
        }

        boolean has(String annotation) {
            return annotations.stream().anyMatch(a -> a.startsWith("@" + annotation));
        }
    }

    final List<String> lines;
    final FileLayer layer;
    final Map<String, Integer> imports = new LinkedHashMap<>();   // simple name -> line
    final Map<String, String> importTargets = new LinkedHashMap<>(); // simple name -> fully qualified
    final Map<String, String> fieldTypes = new LinkedHashMap<>();   // field name -> type
    final List<Method> methods = new ArrayList<>();
    String typeKind = "";
    String typeName = "";
    int typeLine;
    int typeEndLine;
    List<String> typeAnnotations = List.of();
    String basePath = "";
    private int[] depth;

    private JavaFileModel(List<String> lines, FileLayer layer) {
        this.lines = lines;
        this.layer = layer;
    }

    static JavaFileModel parse(List<String> lines, FileLayer layer) {
        JavaFileModel m = new JavaFileModel(lines, layer);
        m.scan();
        return m;
    }

    private void scan() {
        depth = new int[lines.size() + 1];
        for (int i = 0; i < lines.size(); i++) {
            int d = depth[i];
            for (char c : LineExplainer.stripStrings(lines.get(i)).toCharArray()) {
                d += c == '{' ? 1 : c == '}' ? -1 : 0;
            }
            depth[i + 1] = d;
        }
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String l = LineExplainer.stripStrings(raw);
            Matcher imp = IMPORT.matcher(raw);
            if (imp.find()) {
                String target = imp.group(2);
                String simple = imp.group(3) != null ? null : target.substring(target.lastIndexOf('.') + 1);
                if (simple != null) {
                    imports.put(simple, i + 1);
                    importTargets.put(simple, target);
                }
                continue;
            }
            Matcher type = TYPE_DECL.matcher(l);
            if (typeName.isEmpty() && type.find()) {
                typeKind = type.group(1);
                typeName = type.group(2);
                typeLine = i + 1;
                typeAnnotations = annotationsAbove(i);
                typeEndLine = blockEnd(i);
                for (String a : typeAnnotations) {
                    Matcher mm = MAPPING.matcher(a);
                    if (mm.find() && mm.group(1).equals("Request") && mm.group(2) != null) {
                        basePath = mm.group(2);
                    }
                }
                continue;
            }
            if (typeName.isEmpty()) {
                continue;
            }
            Matcher field = FIELD_DECL.matcher(l);
            if (field.matches()) {
                fieldTypes.put(field.group(3), field.group(2).strip());
            } else if (depth[i] == 1 && l.strip().matches("[\\w<>\\[\\], ?.]+\\s+\\w+\\s*;")) {
                // package-private field such as "String name;"
                String[] parts = l.strip().replace(";", "").split("\\s+");
                fieldTypes.put(parts[parts.length - 1], String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1)));
            }
            Matcher ctor = CONSTRUCTOR_DECL.matcher(l);
            Matcher method = METHOD_DECL.matcher(l);
            if (ctor.matches() && ctor.group(1).equals(typeName)) {
                methods.add(new Method(typeName, "", paramsOf(i, typeName), i + 1, blockEnd(i), annotationsAbove(i), true,
                        false));
            } else if (!CONTROL.matcher(l).find() && !assignsBeforeCall(l) && method.find()
                    && !method.group(1).strip().equals("new") && (!l.strip().endsWith(";") || depth[i] == 1)) {
                boolean abstractMethod = l.strip().endsWith(";");
                methods.add(new Method(method.group(2), method.group(1).strip(), paramsOf(i, method.group(2)), i + 1,
                        abstractMethod ? i + 1 : blockEnd(i), annotationsAbove(i), false, abstractMethod));
            }
        }
    }

    /** The text between the parentheses of name(...), even when the parameter list spans several lines. */
    private String paramsOf(int i, String name) {
        StringBuilder sig = new StringBuilder();
        for (int j = i; j < lines.size() && j < i + 12; j++) {
            String l = LineExplainer.stripStrings(lines.get(j));
            sig.append(' ').append(l.strip());
            if (l.contains("{") || l.strip().endsWith(";")) {
                break;
            }
        }
        String text = sig.toString();
        int start = text.indexOf(name + "(");
        start = start < 0 ? text.indexOf('(') : start + name.length();
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int k = start; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return text.substring(start + 1, k).strip();
            }
        }
        return text.substring(start + 1).strip();
    }

    /** "Foo x = bar(" is a variable, not a method; but "@GetMapping(value = ...) Foo x(" is a method. */
    private static boolean assignsBeforeCall(String l) {
        int eq = l.indexOf('=');
        int paren = l.indexOf('(');
        return eq >= 0 && (paren < 0 || eq < paren);
    }

    /** Annotation lines directly above line index i (0-based), top to bottom. */
    List<String> annotationsAbove(int i) {
        List<String> result = new ArrayList<>();
        for (int j = i - 1; j >= 0; j--) {
            String t = lines.get(j).strip();
            if (t.startsWith("@")) {
                result.addFirst(t);
            } else if (!(t.startsWith(")") || t.startsWith("\"") || t.endsWith(",") && !result.isEmpty())) {
                break;
            }
        }
        // annotations written on the same line as the declaration
        Matcher inline = Pattern.compile("@\\w+(?:\\([^)]*\\))?").matcher(lines.get(i));
        while (inline.find()) {
            result.add(inline.group());
        }
        return result;
    }

    /** 1-based line of the brace that closes the block opened on (or right after) line index i. */
    int blockEnd(int i) {
        int depth = 0;
        boolean opened = false;
        for (int j = i; j < lines.size(); j++) {
            String l = LineExplainer.stripStrings(lines.get(j));
            for (char c : l.toCharArray()) {
                if (c == '{') {
                    depth++;
                    opened = true;
                } else if (c == '}') {
                    depth--;
                    if (opened && depth == 0) {
                        return j + 1;
                    }
                }
            }
            if (!opened && l.strip().endsWith(";")) {
                return j + 1;
            }
        }
        return lines.size();
    }

    /** Brace depth at the start of line index i (0 = file level, 1 = inside the type). */
    int depthAt(int i) {
        return depth[Math.max(0, Math.min(i, depth.length - 1))];
    }

    Method methodAt(int line) {
        Method best = null;
        for (Method m : methods) {
            if (m.startLine() <= line && line <= m.endLine() && (best == null || m.startLine() > best.startLine())) {
                best = m;
            }
        }
        return best;
    }

    Method methodStartingAt(int line) {
        return methods.stream().filter(m -> m.startLine() == line).findFirst().orElse(null);
    }

    Method methodEndingAt(int line) {
        return methods.stream().filter(m -> m.endLine() == line && m.startLine() != line).findFirst().orElse(null);
    }

    /** First line after {@code afterLine} that uses {@code word} as a whole word (outside strings/comments). */
    int firstUse(String word, int afterLine) {
        Pattern p = Pattern.compile("(?<![\\w.])" + Pattern.quote(word) + "\\b");
        for (int j = afterLine; j < lines.size(); j++) {
            String l = LineExplainer.stripStrings(lines.get(j)).strip();
            if (l.startsWith("import ") || l.startsWith("*") || l.startsWith("/*")) {
                continue;
            }
            if (p.matcher(l).find()) {
                return j + 1;
            }
        }
        return -1;
    }

    boolean isEntity() {
        return typeAnnotations.stream().anyMatch(a -> a.startsWith("@Entity") || a.startsWith("@Embeddable")
                || a.startsWith("@MappedSuperclass"));
    }

    /** Spring creates exactly one instance of these classes and injects their constructor arguments. */
    boolean isBean() {
        return typeAnnotations.stream().anyMatch(a -> a.matches(
                "@(Service|Component|Repository|RestController|Controller|Configuration|RestControllerAdvice|ControllerAdvice)\\b.*"));
    }

    /** True while line n (1-based) is still part of method's multi-line parameter list. */
    boolean inSignature(Method method, int n) {
        if (method == null || n <= method.startLine()) {
            return false;
        }
        for (int j = method.startLine() - 1; j < n - 1; j++) {
            if (lines.get(j).contains("{")) {
                return false;
            }
        }
        return true;
    }

    boolean isController() {
        return typeAnnotations.stream().anyMatch(a -> a.startsWith("@RestController") || a.startsWith("@Controller"));
    }

    boolean isTest() {
        return layer == FileLayer.TEST || methods.stream().anyMatch(m -> m.has("Test"));
    }

    /**
     * Plain-English steps of a method body, e.g. "loads the Task with tasks.findById(...)",
     * "throws ApiException", "returns TaskResponse.from(task)". At most {@code max} steps.
     */
    List<String> steps(Method m, int max) {
        List<String> steps = new ArrayList<>();
        if (m.abstractMethod()) {
            return steps;
        }
        for (int j = m.startLine(); j < m.endLine() - 1 && steps.size() < max; j++) {
            String t = lines.get(j).strip();
            if (t.isEmpty() || t.startsWith("//") || t.equals("{") || t.equals("}")) {
                continue;
            }
            String step = describeStatement(t);
            if (step != null && !steps.contains(step)) {
                steps.add(step);
            }
        }
        return steps;
    }

    private String describeStatement(String t) {
        if (t.startsWith("throw ")) {
            Matcher ex = Pattern.compile("new\\s+(\\w+)|(\\w+)\\.\\w+\\(").matcher(t);
            return ex.find() ? "stops with " + (ex.group(1) != null ? ex.group(1) : ex.group(2)) + " when something is wrong"
                    : "throws an exception";
        }
        if (t.startsWith("return")) {
            String what = t.replaceFirst("^return\\s*", "").replaceAll(";$", "");
            return what.isEmpty() ? "returns" : "returns " + shorten(what, 60);
        }
        if (t.startsWith("if ") || t.startsWith("if(")) {
            return "checks " + shorten(t.replaceFirst("^if\\s*", "").replaceAll("\\{$", "").strip(), 60);
        }
        if (t.startsWith("for ") || t.startsWith("for(") || t.startsWith("while")) {
            return "loops " + shorten(t.replaceFirst("^(for|while)\\s*", "").replaceAll("\\{$", "").strip(), 60);
        }
        Matcher call = FIELD_CALL.matcher(t);
        while (call.find()) {
            String target = call.group(1);
            if (fieldTypes.containsKey(target) || target.equals("this")) {
                return "calls " + target + "." + call.group(2) + "(...)"
                        + (fieldTypes.containsKey(target) ? " on the " + fieldTypes.get(target) : "");
            }
        }
        Matcher assign = Pattern.compile("^(?:this\\.)?(\\w+)\\s*=\\s*(.+);$").matcher(t);
        if (assign.find() && (fieldTypes.containsKey(assign.group(1)) || t.startsWith("this."))) {
            return "sets " + assign.group(1);
        }
        return null;
    }

    static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    static String joinPath(String base, String path) {
        String b = base == null ? "" : base;
        if (b.endsWith("/") && path.startsWith("/")) {
            return b + path.substring(1);
        }
        String joined = b + (path.isEmpty() || path.startsWith("/") || b.isEmpty() ? "" : "/") + path;
        return joined.isEmpty() ? "/" : joined;
    }
}
