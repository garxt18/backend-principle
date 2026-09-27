package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.lab.JavaFileModel.Method;
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
 * Explains a single line of code without any AI. For every line it answers two questions:
 * <ul>
 *   <li><b>summary</b> - what does this line do?</li>
 *   <li><b>why</b> - why do I have to type it here, and what breaks without it?</li>
 * </ul>
 * It combines a light model of the whole file ({@link JavaFileModel}: which method we are in, where
 * an import is used, what a method does step by step) with the knowledge base in
 * {@code lab/knowledge.json}. Fast, free and deterministic - no API keys needed.
 */
@Component
public class LineExplainer {

    public record Note(String term, String text) {
    }

    public record Explanation(int lineNumber, String code, String kind, String summary, String why, String context,
                              List<Note> notes) {
    }

    /** A member of the file for the "what you are about to build" outline. */
    public record OutlineItem(int line, String kind, String name, String summary) {
    }

    public record Outline(String purpose, List<OutlineItem> items) {
    }

    record Knowledge(Map<String, String> annotations, Map<String, String> annotationWhy, Map<String, String> keywords,
                     Map<String, String> symbols, Map<String, String> methods, Map<String, String> imports,
                     Map<String, String> yaml, Map<String, String> xml, Map<String, String> artifacts,
                     Map<String, String> sql, Map<String, String> docker) {
    }

    private static final Pattern ANNOTATION = Pattern.compile("@([A-Z][A-Za-z0-9]*)");
    private static final Pattern WORD = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Pattern THIS_ASSIGN = Pattern.compile("^\\s*this\\.(\\w+)\\s*=\\s*(.+?)\\s*;\\s*$");
    private static final Pattern LOCAL_VAR = Pattern.compile(
            "^\\s*(?:final\\s+)?((?:[A-Z][\\w.]*(?:<[^=]*>)?(?:\\[\\])*)|var|int|long|double|boolean|char|byte|short|float)\\s+([a-z]\\w*)\\s*=\\s*(.+?);?\\s*$");
    private static final Pattern CALL = Pattern.compile("^\\s*([a-z]\\w*)\\.(\\w+)\\s*\\(");
    private static final Pattern ANY_CALL = Pattern.compile("\\.?\\b([a-z]\\w*)\\s*\\(");
    private static final Pattern DERIVED_QUERY = Pattern.compile("\\b(find|read|get|query|count|exists|delete|stream)(?:All|First\\d*|Top\\d*|Distinct)?By(\\w+?)\\s*\\(");
    private static final Pattern XML_TAG = Pattern.compile("<\\s*/?\\s*([A-Za-z][\\w.-]*)");
    private static final Pattern XML_VALUE = Pattern.compile("<artifactId>\\s*([^<\\s]+)\\s*</artifactId>");
    private static final Pattern ENUM_CONSTANT = Pattern.compile("^([A-Z][A-Z0-9_]*)\\s*(\\(.*\\))?\\s*[,;]?\\s*$");
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

    public Explanation explain(List<String> lines, int lineNumber, String language) {
        return explain(lines, lineNumber, language, FileLayer.OTHER);
    }

    /** @param lineNumber 1-based */
    public Explanation explain(List<String> lines, int lineNumber, String language, FileLayer layer) {
        String code = lines.get(lineNumber - 1);
        String trimmed = code.strip();
        return switch (language) {
            case "java", "kotlin" -> explainJava(JavaFileModel.parse(lines, layer), lineNumber, code, trimmed);
            case "yaml" -> explainYaml(lines, lineNumber, code, trimmed);
            case "properties" -> explainProperties(lineNumber, code, trimmed);
            case "xml" -> explainXml(lineNumber, code, trimmed);
            case "sql" -> explainSql(lineNumber, code, trimmed);
            case "dockerfile" -> explainDocker(lineNumber, code, trimmed);
            case "typescript", "javascript" -> explainScript(lines, lineNumber, code, trimmed);
            case "css" -> explainCss(lineNumber, code, trimmed);
            case "html" -> explainHtml(lineNumber, code, trimmed);
            default -> generic(lineNumber, code, trimmed);
        };
    }

    // ================================================================ Java

    private Explanation explainJava(JavaFileModel m, int n, String code, String t) {
        String context = context(m, n);
        if (t.isEmpty()) {
            return new Explanation(n, code, "blank", "Blank line.",
                    "Blank lines separate logical blocks so the code is easier to scan. The compiler ignores them.", context, List.of());
        }
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
            boolean javadoc = t.startsWith("/**") || t.startsWith("*") && !t.startsWith("*/");
            return new Explanation(n, code, "comment",
                    javadoc ? "Javadoc - documentation for the element below; IDEs show it on hover."
                            : "Comment - ignored by the compiler.",
                    "Comments are for the next human (often future you). Good ones explain WHY the code is like this, "
                            + "not what it does. Typing it makes you read the intent once more.", context, List.of());
        }
        if (t.matches("[})\\];,]+")) {
            return closing(m, n, code, context);
        }

        List<Note> notes = new ArrayList<>();
        String kind;
        String summary;
        String why;
        Method starting = m.methodStartingAt(n);
        Matcher thisAssign = THIS_ASSIGN.matcher(t);
        Matcher local = LOCAL_VAR.matcher(t);
        Matcher call = CALL.matcher(t);
        Matcher field = JavaFileModel.FIELD_DECL.matcher(t);
        boolean annotationOnly = t.startsWith("@") && (t.matches("@[\\w.]+(\\(.*\\))?") || !t.contains("(") && t.split("\\s+").length == 1);

        if (t.startsWith("package ")) {
            kind = "package";
            String pkg = t.substring(8).replace(";", "").strip();
            summary = "This file belongs to package " + pkg + " - so it must live in the folder " + pkg.replace('.', '/') + "/.";
            why = "Java requires the package line to match the folder. Spring's component scan starts at the package "
                    + "of the @SpringBootApplication class and only finds classes in that package or below it.";
            addKeyword(notes, "package");
        } else if (t.startsWith("import ")) {
            kind = "import";
            String target = t.replaceFirst("^import\\s+(static\\s+)?", "").replace(";", "").strip();
            summary = (t.startsWith("import static") ? "Static import of " : "Imports ") + target
                    + " so this file can use it by its short name.";
            why = importWhy(m, target, n);
            String lib = longestPrefix(kb.imports(), target);
            if (lib != null) {
                notes.add(new Note(libraryName(target), lib));
            }
        } else if (annotationOnly) {
            kind = "annotation";
            String first = firstAnnotation(t);
            String target = describeTarget(m, n);
            String known = kb.annotations().get(first);
            summary = known != null ? known : "Annotation @" + first + " - metadata a framework reads to add behaviour.";
            String endpoint = starting == null ? endpointFor(m, n) : null;
            if (endpoint != null) {
                summary = "Maps HTTP " + endpoint + " to " + target + ". " + summary;
            }
            why = "Applies to " + target + ". " + kb.annotationWhy().getOrDefault(first,
                    "Without it this behaviour is simply not applied - frameworks only react to annotations they find.");
        } else if (m.typeLine == n) {
            kind = "type";
            summary = typeSummary(m, t);
            why = typeWhy(m, t);
        } else if (starting != null && starting.constructor()) {
            kind = "constructor";
            summary = constructorSummary(m, starting);
            why = constructorWhy(m, starting);
        } else if (starting != null) {
            kind = "method";
            summary = methodSummary(m, starting);
            why = methodWhy(m, starting);
        } else if (m.inSignature(m.methodAt(n), n)) {
            kind = "parameter";
            Method method = m.methodAt(n);
            String param = t.replaceAll("@\\w+(\\([^)]*\\))?", "").replaceAll("[,)]\\s*\\{?\\s*$", "").strip();
            summary = "Another parameter of " + method.name() + "(): " + param + ".";
            why = t.contains("@RequestParam") ? "@RequestParam fills it from the query string (?name=value); required = false makes it optional."
                    : t.contains("@PathVariable") ? "@PathVariable fills it from the {placeholder} in the URL."
                    : t.contains("@RequestBody") ? "@RequestBody turns the JSON body into this Java object."
                    : t.contains("Pageable") ? "Spring builds a Pageable from ?page=0&size=20&sort=field,desc - paging for free."
                    : "Long parameter lists are split over lines to stay readable.";
        } else if (thisAssign.matches()) {
            kind = "assignment";
            String f = thisAssign.group(1);
            summary = "Stores " + thisAssign.group(2) + " in the field '" + f + "'. 'this.' means \"the field of this object\" "
                    + "- it tells it apart from the parameter with the same name.";
            String ft = m.fieldTypes.getOrDefault(f, "");
            boolean dependency = m.isBean() && ft.matches("[A-Z]\\w*(Service|Repository|Client|Encoder|Mapper|Properties|Template|Clock|Manager|Provider|Resolver|Converter|Publisher)");
            why = dependency ? "Without this line the field stays null, and the first " + f + ".something() call later throws a NullPointerException."
                    : "Without this line the value passed in is lost - the field keeps its default (null/0/false).";
        } else if (field.matches() && m.depthAt(n - 1) == 1) {
            kind = "field";
            summary = fieldSummary(m, field);
            why = fieldWhy(m, field);
        } else if (m.typeKind.equals("enum") && m.depthAt(n - 1) == 1 && ENUM_CONSTANT.matcher(t).matches()) {
            kind = "enum-constant";
            summary = "Enum constant " + t.replaceAll("[,;(].*", "") + " - one of the fixed values of " + m.typeName + ".";
            why = "An enum lists every allowed value, so the compiler rejects typos and invalid states (a String could hold anything).";
        } else if (m.typeKind.equals("record") && n > m.typeLine && m.depthAt(n - 1) == 0) {
            kind = "record-component";
            summary = "A component of the record " + m.typeName + " - it becomes a private final field, a constructor parameter "
                    + "and an accessor method with the same name.";
            why = "Records are perfect DTOs: immutable, and Jackson turns each component into a JSON property.";
        } else if (t.startsWith("return")) {
            kind = "return";
            String what = t.replaceFirst("^return\\s*", "").replaceAll(";$", "");
            summary = what.isEmpty() ? "Ends the method here." : "Hands " + JavaFileModel.shorten(what, 80) + " back to whoever called this method.";
            why = join(returnWhy(m, n), keyCall(m, t));
        } else if (t.startsWith("throw ")) {
            kind = "throw";
            Matcher ex = Pattern.compile("new\\s+(\\w+)|(\\w+)\\.\\w+\\(").matcher(t);
            String type = ex.find() ? (ex.group(1) != null ? ex.group(1) : ex.group(2)) : "an exception";
            summary = "Stops the method immediately by throwing " + type + ". No line after this runs.";
            why = "Failing fast keeps bad data out of the database. A global @RestControllerAdvice (or @ResponseStatus) turns the "
                    + "exception into a proper HTTP error instead of a 500.";
        } else if (t.matches("^(\\}\\s*)?(if|else|for|while|switch|try|catch|finally|do)\\b.*")) {
            kind = "control";
            String[] sw = controlSummary(t);
            summary = sw[0];
            why = sw[1];
        } else if (local.matches()) {
            kind = "variable";
            String name = local.group(2);
            summary = "Creates the local variable '" + name + "' (" + local.group(1) + ") from " + JavaFileModel.shorten(local.group(3), 70) + ".";
            int use = m.firstUse(name, n);
            why = join(keyCall(m, t), use > 0 ? "Line " + use + " uses '" + name + "' - naming the intermediate result keeps that line short and readable."
                    : "Keeps the result so the following code can use it. It only exists until the closing } of this block.");
        } else if (t.matches("^[a-z]\\w*\\s*=[^=].*;$")) {
            kind = "assignment";
            String var = t.substring(0, t.indexOf('=')).strip();
            summary = "Puts the result of " + JavaFileModel.shorten(t.substring(t.indexOf('=') + 1).replaceAll(";$", "").strip(), 70)
                    + " into '" + var + "'.";
            why = join(keyCall(m, t), "'" + var + "' was declared earlier; each branch fills it so the code after the if/else can use it.");
        } else if (m.depthAt(n - 1) > 1 && t.matches("^[A-Z][\\w<>\\[\\], ?.]*\\s+[a-z]\\w*\\s*;$")) {
            kind = "variable";
            String var = t.replaceAll(".*\\s+([a-z]\\w*)\\s*;$", "$1");
            summary = "Declares the local variable '" + var + "' without a value yet.";
            why = "The value is decided later (usually by the if/else below). Java checks at compile time that it is assigned before it is read.";
        } else if (call.find()) {
            kind = "call";
            String target = call.group(1);
            String method = call.group(2);
            String type = m.fieldTypes.get(target);
            summary = "Calls " + method + "(...) on " + (type != null ? "the injected " + type + " '" + target + "'" : "'" + target + "'") + ".";
            String key = keyCall(m, t);
            why = key != null ? key : "One step of " + (context.isEmpty() ? "this block" : context.replace("inside ", "")) + ".";
        } else if (t.startsWith(".")) {
            kind = "chain";
            summary = "Continues the method chain from the line above: " + JavaFileModel.shorten(t, 60);
            why = "Fluent APIs (streams, builders, security config, MockMvc) put one call per line so each step is readable.";
        } else if (t.contains("->")) {
            kind = "lambda";
            summary = "A lambda (->): a small inline function passed as a value.";
            why = "Lambdas replace whole anonymous classes when an API (stream, Optional, security DSL) asks for behaviour.";
        } else {
            kind = "statement";
            summary = "A statement - one step of " + (context.isEmpty() ? "this block" : context.replace("inside ", "")) + ".";
            why = "Each statement ends with ';'. Read it left to right: what is computed, and where the result goes.";
        }

        Matcher ann = ANNOTATION.matcher(stripStrings(t));
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
        if (!kind.equals("import") && !kind.equals("package")) {
            Matcher calls = ANY_CALL.matcher(stripStrings(t));
            while (calls.find()) {
                String name = calls.group(1);
                String text = kb.methods().get(name);
                if (text != null) {
                    notes.add(new Note(name, text));
                }
            }
        }
        if (t.contains("::")) {
            notes.add(new Note("::", "Method reference - a shorthand lambda that calls an existing method, e.g. TaskResponse::from."));
        }
        if (t.contains("->") && !kind.equals("lambda")) {
            notes.add(new Note("->", "Lambda expression: an inline function passed as a value."));
        }
        Set<String> words = wordsOutsideStrings(t);
        for (String w : words) {
            String s = kb.symbols().get(w);
            if (s != null && Character.isUpperCase(w.charAt(0))) {
                notes.add(new Note(w, s));
            }
        }
        for (String kw : KEYWORD_PRIORITY) {
            if (words.contains(kw) && !kind.equals("import") && !kind.equals("package")) {
                addKeyword(notes, kw);
            }
        }
        return new Explanation(n, code, kind, summary, why, context, limit(notes, 8));
    }

    private Explanation closing(JavaFileModel m, int n, String code, String context) {
        Method ending = m.methodEndingAt(n);
        String summary;
        if (ending != null) {
            summary = "Closes " + (ending.constructor() ? "the constructor" : "the method " + ending.name() + "()") + ".";
        } else if (n == m.typeEndLine) {
            summary = "Closes the " + m.typeKind + " " + m.typeName + " - everything in this file lives between its { and this }.";
        } else {
            summary = "Closes the block opened above" + (context.isEmpty() ? "." : " (" + context + ").");
        }
        return new Explanation(n, code, "close", summary,
                "Every { needs its matching }. A missing one gives the classic 'reached end of file while parsing' error; "
                        + "indentation shows you which { this one belongs to.", context, List.of());
    }

    private String context(JavaFileModel m, int n) {
        Method method = m.methodAt(n);
        String type = m.typeName.isEmpty() ? null : m.typeKind + " " + m.typeName;
        if (method != null && method.startLine() != n && type != null) {
            return "inside " + (method.constructor() ? "the constructor " + method.name() + "()" : method.name() + "()") + " of " + type;
        }
        return type != null && n > m.typeLine && n < m.typeEndLine ? "inside " + type : "";
    }

    private String importWhy(JavaFileModel m, String target, int n) {
        if (target.endsWith(".*")) {
            return "A wildcard import brings in every class of that package. Explicit imports are preferred: they show exactly what the file depends on.";
        }
        String simple = target.substring(target.lastIndexOf('.') + 1);
        int use = m.firstUse(simple, n);
        if (use < 0) {
            return "Nothing in this file seems to use " + simple + " - your IDE would grey this import out. You can skip it in your own code.";
        }
        return "You need it because line " + use + " uses " + simple + ": \"" + JavaFileModel.shorten(m.lines.get(use - 1).strip(), 70)
                + "\". Without this import the compiler stops with 'cannot find symbol: " + simple + "'.";
    }

    private static String firstAnnotation(String t) {
        Matcher a = ANNOTATION.matcher(t);
        return a.find() ? a.group(1) : t.substring(1);
    }

    /** "the method create()", "the class TaskService", "the field title" - whatever the annotation sits on. */
    private String describeTarget(JavaFileModel m, int n) {
        for (int j = n; j < m.lines.size(); j++) {
            String t = m.lines.get(j).strip();
            if (t.isEmpty() || t.startsWith("@") || t.startsWith("//") || t.startsWith(")") || t.startsWith("\"")) {
                continue;
            }
            if (j + 1 == m.typeLine) {
                return "the " + m.typeKind + " " + m.typeName;
            }
            Method method = m.methodStartingAt(j + 1);
            if (method != null) {
                return method.constructor() ? "the constructor" : "the method " + method.name() + "()";
            }
            Matcher f = JavaFileModel.FIELD_DECL.matcher(t);
            if (f.matches()) {
                return "the field " + f.group(3);
            }
            return "the next line";
        }
        return "the next line";
    }

    private String endpointFor(JavaFileModel m, int n) {
        for (Method method : m.methods) {
            if (method.startLine() > n && method.startLine() - n <= 6) {
                String e = method.endpoint(m.basePath);
                if (e != null && m.lines.get(n - 1).contains("Mapping")) {
                    return e;
                }
                return null;
            }
        }
        return null;
    }

    private String typeSummary(JavaFileModel m, String t) {
        StringBuilder s = new StringBuilder("Declares the " + m.typeKind + " " + m.typeName + ".");
        Matcher ext = Pattern.compile("extends\\s+([\\w.]+)(<[^{]*>)?").matcher(t);
        if (ext.find()) {
            String parent = ext.group(1);
            if (parent.endsWith("Repository")) {
                s.append(" By extending ").append(parent).append(ext.group(2) == null ? "" : ext.group(2))
                        .append(" you get save, findById, findAll, deleteById... for free - Spring generates the implementation at startup.");
            } else if (parent.endsWith("Exception")) {
                s.append(" It is a ").append(parent).append(", so it can be thrown and caught like any exception.");
            } else {
                s.append(" It inherits everything public/protected from ").append(parent).append(".");
            }
        }
        Matcher impl = Pattern.compile("implements\\s+([\\w.<>, ]+)").matcher(t);
        if (impl.find()) {
            s.append(" It promises to provide the methods of ").append(impl.group(1).replace("{", "").strip()).append(".");
        }
        if (m.typeKind.equals("record")) {
            s.append(" A record is an immutable data carrier: the compiler writes the constructor, accessors, equals, hashCode and toString.");
        } else if (m.typeKind.equals("enum")) {
            s.append(" An enum is a fixed set of named constants.");
        } else if (m.typeKind.equals("interface") && !t.contains("Repository")) {
            s.append(" An interface only describes WHAT can be done; classes implement HOW.");
        }
        return s.toString();
    }

    private String typeWhy(JavaFileModel m, String t) {
        String role = switch (m.layer) {
            case CONTROLLER -> "This is the HTTP layer: it receives requests, validates input and hands the work to a service.";
            case SERVICE -> "This is where business rules live, so controllers stay thin and the rules can be tested without HTTP.";
            case REPOSITORY -> "This is the only place that talks to the database for this entity.";
            case DOMAIN -> m.isEntity() ? "This entity is the Java view of a database table - Hibernate turns objects into rows and back."
                    : "A domain type: a noun of the business that other layers share.";
            case DTO -> "DTOs define the exact JSON your API accepts/returns, so the entity (and database) can change without breaking clients.";
            case EXCEPTION -> "Errors get their own types so the API can map each one to the right HTTP status.";
            case SECURITY -> "Security code decides who may call what; it runs before any controller.";
            case SPRING_CONFIG -> "Configuration classes create beans that Spring cannot guess on its own.";
            case MAIN -> "This is the entry point: running it starts the whole application.";
            case TEST -> "Tests prove the code works and keep it working when you change things later.";
            default -> m.layer.why();
        };
        return "The file is named " + m.typeName + ".java because a public type must live in a file with the same name. " + role;
    }

    private String constructorSummary(JavaFileModel m, Method c) {
        if (c.params().isBlank()) {
            return "No-argument constructor of " + m.typeName + ".";
        }
        if (!m.isBean()) {
            return "Constructor: to create a " + m.typeName + " you must pass " + signatureParams(c.params())
                    + ". It copies them into the fields below.";
        }
        List<String> types = paramTypes(c.params());
        return "Constructor of " + m.typeName + ". When Spring creates this bean it finds beans of type " + String.join(", ", types)
                + " and passes them in here - constructor injection.";
    }

    private String constructorWhy(JavaFileModel m, Method c) {
        if (c.params().isBlank()) {
            return m.isEntity() ? "JPA/Hibernate needs a no-argument constructor to create an empty object before filling it from a row. "
                    + "'protected' stops your own code from creating half-empty entities." : "Lets code create the object without arguments.";
        }
        if (!m.isBean()) {
            return "A constructor guarantees an object is never created half-filled: whoever writes new " + m.typeName
                    + "(...) must supply these values.";
        }
        return "Constructor injection makes dependencies explicit and lets the fields be final. In a unit test you can pass mocks "
                + "straight in - no Spring needed. With a single constructor, @Autowired is not required.";
    }

    private String methodSummary(JavaFileModel m, Method method) {
        StringBuilder s = new StringBuilder();
        String endpoint = method.endpoint(m.basePath);
        if (endpoint != null) {
            s.append("Endpoint ").append(endpoint).append(": ");
        }
        s.append(endpoint != null ? "declares " : "Declares ").append(method.name()).append("(").append(signatureParams(method.params())).append(")");
        if (!method.returnType().isEmpty() && !method.returnType().equals("void")) {
            s.append(" which returns ").append(method.returnType());
        } else if (method.returnType().equals("void")) {
            s.append(" which returns nothing (void)");
        }
        s.append(".");
        Matcher derived = DERIVED_QUERY.matcher(method.name() + "(");
        if (method.abstractMethod() && derived.find()) {
            s.append(" ").append(DerivedQueryTranslator.describe(derived.group(1), derived.group(2)));
        }
        List<String> steps = m.steps(method, 5);
        if (!steps.isEmpty()) {
            s.append(" Step by step it ");
            for (int i = 0; i < steps.size(); i++) {
                s.append(i == 0 ? "" : i == steps.size() - 1 ? " and then " : ", ").append(steps.get(i));
            }
            s.append(".");
        }
        return s.toString();
    }

    private String methodWhy(JavaFileModel m, Method method) {
        String name = method.name();
        if (name.equals("main")) {
            return "The JVM starts every Java program at main(). Here it calls SpringApplication.run, which boots Spring.";
        }
        if (method.has("Test")) {
            return "JUnit runs every @Test method on its own. If an assertion inside fails, the build fails - so this test guards the behaviour it checks.";
        }
        if (method.has("Bean")) {
            return "Whatever this method returns becomes a Spring bean that other classes can inject.";
        }
        if (method.has("ExceptionHandler")) {
            return "Spring calls this method when the matching exception escapes any controller, and returns its result as the error response.";
        }
        String endpoint = method.endpoint(m.basePath);
        if (endpoint != null) {
            return "Spring calls this method when a " + endpoint + " request arrives. Parameters are filled from the URL/body, and the "
                    + "return value is turned into the JSON response.";
        }
        if (method.abstractMethod() && m.layer == FileLayer.REPOSITORY) {
            return "You only declare it; Spring Data implements it at startup from the method name (or the @Query above).";
        }
        if (name.matches("get[A-Z]\\w*|is[A-Z]\\w*") && method.params().isBlank()) {
            return "A getter: other classes (and Jackson, when building JSON) read the private field through it.";
        }
        if (name.matches("set[A-Z]\\w*")) {
            return "A setter: the controlled way to change a private field.";
        }
        if (name.equals("equals") || name.equals("hashCode")) {
            return "equals and hashCode must agree, otherwise HashSet/HashMap and JPA identity checks behave strangely.";
        }
        if (name.equals("toString")) {
            return "Controls what you see when the object is logged or printed.";
        }
        if (method.has("Transactional")) {
            return "Everything inside runs in ONE database transaction: either all changes are saved, or none are.";
        }
        return switch (m.layer) {
            case SERVICE -> "One business operation. Controllers call it; keeping the logic here means it can be reused and unit-tested.";
            case SECURITY -> "Part of the security flow that runs before your controllers.";
            case DTO -> "Keeps conversion/validation logic next to the data it belongs to.";
            default -> "Methods give a name to a piece of behaviour so it can be called (and tested) from other places.";
        };
    }

    private String fieldSummary(JavaFileModel m, Matcher f) {
        String modifiers = f.group(1);
        String type = f.group(2).strip();
        String name = f.group(3);
        if (modifiers.contains("static") && modifiers.contains("final")) {
            return "A constant '" + name + "' (" + type + "): one value shared by every instance, never changes.";
        }
        if (m.isEntity()) {
            return "The field '" + name + "' (" + type + ") - Hibernate maps it to the column " + snake(name) + " of the table.";
        }
        return "Declares the field '" + name + "' of type " + type + " - state that every " + m.typeName + " object carries.";
    }

    private String fieldWhy(JavaFileModel m, Matcher f) {
        String modifiers = f.group(1);
        String type = f.group(2).strip();
        if (type.equals("Logger")) {
            return "Every log line of this class goes through this logger, tagged with the class name.";
        }
        if (modifiers.contains("static") && modifiers.contains("final")) {
            return "Naming a magic value once avoids repeating it and makes changing it a one-line edit.";
        }
        if (type.endsWith("Repository")) {
            return "This class needs database access: Spring injects the " + type + " bean through the constructor. "
                    + (modifiers.contains("final") ? "'final' guarantees it is set exactly once." : "");
        }
        if (type.endsWith("Service") || type.endsWith("Client") || type.endsWith("Encoder") || type.endsWith("Mapper")
                || type.equals("Clock") || type.endsWith("Properties") || type.endsWith("Template")) {
            return "A dependency: " + m.typeName + " delegates part of its job to " + type + ", which Spring injects. "
                    + (modifiers.contains("final") ? "'final' = must be assigned once, in the constructor." : "");
        }
        if (m.isEntity()) {
            String sqlType = switch (type.replaceAll("<.*", "")) {
                case "String" -> "VARCHAR/TEXT";
                case "Long", "long" -> "BIGINT";
                case "Integer", "int" -> "INTEGER";
                case "Boolean", "boolean" -> "BOOLEAN";
                case "Instant", "OffsetDateTime" -> "TIMESTAMPTZ";
                case "LocalDateTime" -> "TIMESTAMP";
                case "LocalDate" -> "DATE";
                case "BigDecimal" -> "NUMERIC";
                case "UUID" -> "UUID";
                case "Double", "double" -> "DOUBLE PRECISION";
                default -> null;
            };
            return (sqlType != null ? "A Java " + type + " is stored as " + sqlType + ". " : "")
                    + "It must match the column in your SQL migration - with ddl-auto=validate a mismatch stops the app at startup.";
        }
        return modifiers.contains("private") ? "'private' hides it from other classes (encapsulation) - they go through methods instead."
                : "Holds state for this object.";
    }

    private String returnWhy(JavaFileModel m, int n) {
        Method method = m.methodAt(n);
        if (method != null && method.endpoint(m.basePath) != null) {
            return "In a controller the returned object is converted to JSON by Jackson and becomes the HTTP response body.";
        }
        if (method != null && !method.returnType().isEmpty() && !method.returnType().equals("void")) {
            return "The method promises to return " + method.returnType() + "; without a return the code does not compile.";
        }
        return "Ends the method early - the lines below are skipped for this call.";
    }

    private static String[] controlSummary(String t) {
        String kw = t.replaceFirst("^\\}\\s*", "").split("[\\s(]")[0];
        return switch (kw) {
            case "if" -> new String[]{"Condition: the block below runs only when " + JavaFileModel.shorten(
                    t.replaceFirst("^(\\}\\s*)?if\\s*", "").replaceAll("\\{$", "").strip(), 70) + " is true.",
                    "Guard clauses like this reject bad input or states early, so the rest of the method can assume things are valid."};
            case "else" -> new String[]{"The other branch: runs when the if-condition above was false.",
                    "Handles the remaining case explicitly so no situation is forgotten."};
            case "for" -> new String[]{"A loop: repeats the block below for every element / step.",
                    "Use a loop when the same work applies to many items; a stream is the functional alternative."};
            case "while", "do" -> new String[]{"A loop that repeats while its condition stays true.",
                    "Make sure something inside changes the condition, otherwise it loops forever."};
            case "switch" -> new String[]{"Chooses one branch based on a value.",
                    "Cleaner than a long if/else-if chain when comparing one value against many constants (like enums)."};
            case "try" -> new String[]{"Starts a try block: code that may throw an exception.",
                    "Paired with catch/finally so a failure is handled (or resources are closed) instead of crashing the request."};
            case "catch" -> new String[]{"Handles the exception type named here if the try block threw it.",
                    "Catch only what you can actually handle; otherwise let it bubble up to the global handler."};
            case "finally" -> new String[]{"Runs after try/catch no matter what happened.",
                    "Used for cleanup that must always happen (closing resources, resetting state)."};
            default -> new String[]{"Control flow.", "Decides which lines run next."};
        };
    }

    // ================================================================ file outline

    /** "Before you type": what this file is for and the members you are about to write. */
    public Outline outline(List<String> lines, String language, FileLayer layer) {
        List<OutlineItem> items = new ArrayList<>();
        String purpose = layer.why();
        if (language.equals("java") || language.equals("kotlin")) {
            JavaFileModel m = JavaFileModel.parse(lines, layer);
            if (!m.typeName.isEmpty()) {
                purpose = typeWhy(m, lines.get(m.typeLine - 1)).replaceFirst("^The file is named [^.]+\\.java because a public type must live in a file with the same name\\. ", "");
                if (!m.imports.isEmpty()) {
                    items.add(new OutlineItem(m.imports.values().iterator().next(), "imports", m.imports.size() + " imports",
                            "The classes this file uses from other packages and libraries."));
                }
                items.add(new OutlineItem(m.typeLine, m.typeKind, m.typeName, typeSummary(m, lines.get(m.typeLine - 1))));
                m.fieldTypes.forEach((name, type) -> {
                    int line = m.firstUse(name, 0);
                    items.add(new OutlineItem(Math.max(line, 1), "field", name + " : " + type, "State / dependency of " + m.typeName + "."));
                });
                for (Method method : m.methods) {
                    items.add(new OutlineItem(method.startLine(), method.constructor() ? "constructor" : "method",
                            method.constructor() ? m.typeName + "(" + signatureParams(method.params()) + ")"
                                    : method.name() + "(" + signatureParams(method.params()) + ")",
                            methodSummary(m, method)));
                }
            }
        } else if (language.equals("yaml")) {
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                if (!l.isBlank() && !l.startsWith(" ") && !l.startsWith("#") && l.contains(":")) {
                    String key = l.substring(0, l.indexOf(':')).strip();
                    items.add(new OutlineItem(i + 1, "section", key, "Settings under '" + key + "'."));
                }
            }
        } else if (language.equals("sql")) {
            Pattern table = Pattern.compile("(?i)^\\s*(CREATE|ALTER)\\s+(TABLE|INDEX|UNIQUE INDEX)\\s+(IF NOT EXISTS\\s+)?(\\w+)");
            for (int i = 0; i < lines.size(); i++) {
                Matcher tm = table.matcher(lines.get(i));
                if (tm.find()) {
                    items.add(new OutlineItem(i + 1, tm.group(2).toLowerCase(Locale.ROOT), tm.group(4),
                            tm.group(1).toUpperCase(Locale.ROOT) + " " + tm.group(2).toUpperCase(Locale.ROOT) + " " + tm.group(4)));
                }
            }
        } else if (language.equals("xml")) {
            for (int i = 0; i < lines.size(); i++) {
                Matcher a = XML_VALUE.matcher(lines.get(i));
                if (a.find() && i > 0 && String.join("", lines.subList(Math.max(0, i - 3), i)).contains("<dependency>")) {
                    items.add(new OutlineItem(i + 1, "dependency", a.group(1),
                            kb.artifacts().getOrDefault(a.group(1), "A library this project uses.")));
                }
            }
        }
        return new Outline(purpose, items);
    }

    // ================================================================ YAML / properties

    private Explanation explainYaml(List<String> lines, int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("#")) {
            return new Explanation(n, code, t.isEmpty() ? "blank" : "comment",
                    t.isEmpty() ? "Blank line." : "YAML comment - ignored by Spring.",
                    "Comments in config are the best place to say why a value was chosen.", "", List.of());
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
        boolean section = t.endsWith(":");
        String summary = key == null ? "YAML line." : section
                ? "Starts the section '" + key + "'. Indented lines below belong to it (indentation matters in YAML)."
                : "Sets the property " + key + ". In code you could read it with @Value(\"${" + key + "}\").";
        String why = exact != null ? "Spring Boot reads this at startup. " + exact
                : section ? "YAML nests keys by indentation instead of repeating the prefix on every line."
                : "Settings live outside the code so the same jar runs in dev, test and production with different values.";
        return new Explanation(n, code, "config", summary, why, "", notes);
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
            if (pi < indent && pt.contains(":")) {
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
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "Comment.", "Ignored by Spring.", "", List.of());
        }
        int eq = t.indexOf('=');
        String key = eq > 0 ? t.substring(0, eq).strip() : t;
        List<Note> notes = new ArrayList<>();
        String text = kb.yaml().getOrDefault(key, longestPrefix(kb.yaml(), key));
        if (text != null) {
            notes.add(new Note(key, text));
        }
        return new Explanation(n, code, "config", "Sets the property " + key + ".",
                text != null ? "Spring Boot reads this at startup. " + text
                        : "Keeping values in config instead of code means changing them needs no recompilation.", "", notes);
    }

    // ================================================================ XML (Maven)

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
        String why;
        if (artifact.find()) {
            String a = artifact.group(1);
            String text = kb.artifacts().get(a);
            if (text != null) {
                notes.addFirst(new Note(a, text));
            }
            why = "Maven downloads " + a + " (plus everything it needs) and puts it on the classpath. Without it, every import "
                    + "from this library fails to compile.";
        } else if (!seen.isEmpty() && kb.xml().containsKey(seen.iterator().next())) {
            why = kb.xml().get(seen.iterator().next());
        } else {
            why = "pom.xml is the single source of truth for how the project builds - the same on your laptop, in CI and in Docker.";
        }
        if (t.startsWith("<?xml")) {
            return new Explanation(n, code, "build", "XML declaration: this file is XML, encoded in UTF-8.",
                    "Every pom.xml starts with it so tools read special characters correctly. Copy it as-is.", "", notes);
        }
        if (t.startsWith("xmlns") || t.startsWith("xsi:")) {
            return new Explanation(n, code, "build", "Namespace/schema of the <project> element (continues the tag above).",
                    "It tells IDEs which XML schema a POM follows, so they can autocomplete and validate tags. Every pom has the same lines - copy them.",
                    "", notes);
        }
        String summary = t.isEmpty() ? "Blank line." : t.startsWith("<!--") ? "XML comment."
                : seen.isEmpty() ? "Value inside the tag above." : "Maven build configuration: <" + seen.iterator().next() + ">.";
        return new Explanation(n, code, "build", summary, why, "", notes);
    }

    // ================================================================ SQL / Docker

    private Explanation explainSql(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("--")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "SQL comment.",
                    "Ignored by the database; explains the schema to humans.", "", List.of());
        }
        String upper = t.toUpperCase(Locale.ROOT);
        List<Note> notes = new ArrayList<>();
        kb.sql().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> -e.getKey().length()))
                .filter(e -> Pattern.compile("\\b" + Pattern.quote(e.getKey()) + "\\b").matcher(upper).find())
                .filter(e -> notes.stream().noneMatch(x -> x.term().contains(e.getKey())))
                .forEach(e -> notes.add(new Note(e.getKey(), e.getValue())));
        String summary;
        String why;
        if (upper.startsWith("CREATE TABLE")) {
            summary = "Creates a table.";
            why = "Flyway runs each migration file exactly once, in version order, and records it - so every database "
                    + "(yours, CI, production) ends up with the same schema. Never edit a migration that already ran; add a new one.";
        } else if (upper.startsWith("CREATE") && upper.contains("INDEX")) {
            summary = "Creates an index.";
            why = "Indexes make WHERE/ORDER BY on these columns fast (no full table scan), at the cost of slightly slower writes.";
        } else if (upper.startsWith("ALTER TABLE")) {
            summary = "Changes an existing table.";
            why = "Schema changes after the first release go into new migrations, so existing data is kept.";
        } else if (upper.matches("^\\w+\\s+\\w+.*") && !upper.startsWith("INSERT") && !upper.startsWith("SELECT")
                && !upper.startsWith("UPDATE") && !upper.startsWith("DELETE")) {
            String column = t.split("\\s+")[0];
            summary = "Defines the column " + column + " (name, type and constraints) or a table constraint.";
            why = "The JPA entity field " + camel(column) + " maps to this column; constraints here protect the data even if buggy code tries to save bad values.";
        } else {
            summary = "SQL statement.";
            why = "Runs as part of this migration.";
        }
        return new Explanation(n, code, "sql", summary, why, "", limit(notes, 6));
    }

    private Explanation explainDocker(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("#")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "Dockerfile comment.", "Ignored by Docker.", "", List.of());
        }
        String instruction = t.split("\\s+")[0].toUpperCase(Locale.ROOT);
        String text = kb.docker().get(instruction);
        return new Explanation(n, code, "docker", "Dockerfile instruction " + instruction + ".",
                text != null ? text : "Each instruction adds a layer to the image; Docker caches layers that did not change.", "",
                text == null ? List.of() : List.of(new Note(instruction, text)));
    }

    // ================================================================ frontend (optional track)

    private Explanation explainScript(List<String> lines, int n, String code, String t) {
        String summary;
        String why;
        String kind = "script";
        if (t.isEmpty()) {
            return new Explanation(n, code, "blank", "Blank line.", "Separates blocks.", "", List.of());
        }
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
            return new Explanation(n, code, "comment", "Comment.", "Ignored when the code runs.", "", List.of());
        }
        if (t.startsWith("import ")) {
            kind = "import";
            Matcher from = Pattern.compile("from\\s+['\"]([^'\"]+)['\"]").matcher(t);
            String module = from.find() ? from.group(1) : t.replaceAll("^import\\s+['\"]|['\"];?$", "");
            summary = "Imports from '" + module + "'" + (module.startsWith(".") ? " (a file of this project)." : " (an npm package).");
            why = "ES modules only see what they import; the bundler (Vite) follows these imports to build the app.";
        } else if (t.matches("^export\\s+default\\s+function\\s+[A-Z].*|^(export\\s+)?function\\s+[A-Z]\\w*\\s*\\(.*")) {
            kind = "component";
            summary = "Declares a React component - a function that returns JSX (the UI).";
            why = "Components split the UI into reusable pieces; React calls this function whenever its props or state change.";
        } else if (t.matches(".*\\buse(State|Reducer)\\s*[<(].*")) {
            kind = "hook";
            summary = "useState: a piece of state that survives re-renders. It returns [value, setValue].";
            why = "Calling the setter re-renders the component with the new value - that is how the UI reacts to user input.";
        } else if (t.matches(".*\\buseEffect\\s*\\(.*")) {
            kind = "hook";
            summary = "useEffect: runs code after render (fetching, subscriptions, timers).";
            why = "Side effects must not run during rendering; the dependency array decides when the effect re-runs.";
        } else if (t.matches(".*\\buse(Query|Mutation)\\s*\\(.*")) {
            kind = "hook";
            summary = "TanStack Query hook: fetches/caches server data (or sends a change).";
            why = "It handles loading/error states, caching and refetching so components do not re-implement that.";
        } else if (t.matches("^(export\\s+)?(interface|type)\\s+\\w+.*")) {
            kind = "type";
            summary = "TypeScript type: describes the shape of an object.";
            why = "Types catch mistakes (a typo in a field name, a missing property) at build time instead of in the browser.";
        } else if (t.matches("^(export\\s+)?(const|let|var)\\s+.*")) {
            kind = "variable";
            summary = "Declares a " + (t.contains("const") ? "constant (cannot be reassigned)" : "variable") + ".";
            why = "Prefer const: values that never get reassigned are easier to reason about.";
        } else if (t.startsWith("return")) {
            kind = "return";
            summary = t.contains("<") ? "Returns JSX - the HTML-like markup React renders." : "Returns a value.";
            why = "A component's return value IS its UI.";
        } else if (t.startsWith("<") || t.startsWith("{") && t.endsWith("}")) {
            kind = "jsx";
            summary = "JSX markup: an element of the rendered UI. {expressions} insert JavaScript values.";
            why = "className sets CSS classes; onClick etc. wire events to functions.";
        } else {
            summary = "A statement of the frontend code.";
            why = "Frontend is optional practice here - focus on reading it once and understanding the data flow.";
        }
        return new Explanation(n, code, kind, summary, why, "", List.of());
    }

    private Explanation explainCss(int n, String code, String t) {
        if (t.isEmpty() || t.startsWith("/*")) {
            return new Explanation(n, code, "comment", t.isEmpty() ? "Blank line." : "Comment.", "Ignored by the browser.", "", List.of());
        }
        if (t.endsWith("{")) {
            return new Explanation(n, code, "selector", "Selector: the rules below apply to elements matching " + t.replace("{", "").strip() + ".",
                    "Selectors connect styles to HTML (by class, element or state such as :hover).", "", List.of());
        }
        if (t.contains(":") && t.endsWith(";")) {
            String prop = t.substring(0, t.indexOf(':')).strip();
            return new Explanation(n, code, "declaration", "Sets the CSS property " + prop + ".",
                    prop.startsWith("--") ? "A CSS variable (custom property) - defined once, reused everywhere with var(" + prop + ")."
                            : "One visual rule for the selected elements.", "", List.of());
        }
        return new Explanation(n, code, "css", "CSS.", "Styles the page.", "", List.of());
    }

    private Explanation explainHtml(int n, String code, String t) {
        return new Explanation(n, code, "html", t.isEmpty() ? "Blank line." : "HTML markup.",
                "The browser loads this page first; scripts and styles it references build the UI.", "", List.of());
    }

    private Explanation generic(int n, String code, String t) {
        return new Explanation(n, code, "text", t.isEmpty() ? "Blank line." : "Type this line exactly as shown.",
                "Supporting file - read it to understand how the project is run.", "", List.of());
    }

    // ================================================================ helpers

    private static final Pattern FIELD_CALL = Pattern.compile("\\b([a-z]\\w*)\\.(\\w+)\\s*\\(");

    /** One sentence about the most important call on the line - the part a learner most needs explained. */
    private String keyCall(JavaFileModel m, String t) {
        String code = stripStrings(t);
        Matcher derived = DERIVED_QUERY.matcher(code);
        boolean isDerived = derived.find();
        Matcher fc = FIELD_CALL.matcher(code);
        while (fc.find()) {
            String target = fc.group(1);
            String method = fc.group(2);
            String type = m.fieldTypes.get(target);
            if (type == null) {
                continue;
            }
            String known = kb.methods().get(method);
            if (type.endsWith("Service")) {
                return "The real work is delegated to " + type + "." + method + "(...)"
                        + (m.layer == FileLayer.CONTROLLER ? " - controllers stay thin and only translate HTTP <-> Java." : ".");
            }
            if (type.endsWith("Repository")) {
                return target + "." + method + "(...) goes to the database" + (known != null ? ": " + known
                        : isDerived ? ". " + DerivedQueryTranslator.describe(derived.group(1), derived.group(2)) : ".");
            }
            return "Uses the injected " + type + ": " + (known != null ? method + "() - " + known : "calls its " + method + "() method.");
        }
        if (isDerived) {
            return DerivedQueryTranslator.describe(derived.group(1), derived.group(2));
        }
        Matcher own = Pattern.compile("(?<![.\\w])([a-z]\\w*)\\s*\\(").matcher(code);
        while (own.find()) {
            String name = own.group(1);
            Method helper = m.methods.stream().filter(x -> x.name().equals(name) && !x.constructor()).findFirst().orElse(null);
            if (helper != null && !JavaFileModel.CONTROL.matcher(name).find()) {
                return "Calls " + name + "(...), a method of this same class (line " + helper.startLine()
                        + ") - repeated logic lives in one place instead of being copy-pasted.";
            }
        }
        Matcher any = ANY_CALL.matcher(code);
        while (any.find()) {
            String known = kb.methods().get(any.group(1));
            if (known != null) {
                return any.group(1) + "(): " + known;
            }
        }
        return null;
    }

    private static String join(String a, String b) {
        if (a == null || a.isBlank()) {
            return b;
        }
        return b == null || b.isBlank() ? a : a + " " + b;
    }

    private void addKeyword(List<Note> notes, String kw) {
        String text = kb.keywords().get(kw);
        if (text != null && notes.stream().noneMatch(x -> x.term().equals(kw))) {
            notes.add(new Note(kw, text));
        }
    }

    private static List<String> paramTypes(String params) {
        List<String> types = new ArrayList<>();
        for (String p : splitParams(params)) {
            String clean = p.replaceAll("@\\w+(\\([^)]*\\))?", "").replace("final ", "").strip();
            int space = clean.lastIndexOf(' ');
            if (space > 0) {
                types.add(clean.substring(0, space).strip());
            }
        }
        return types;
    }

    private static String signatureParams(String params) {
        List<String> names = new ArrayList<>();
        for (String p : splitParams(params)) {
            String clean = p.replaceAll("@\\w+(\\([^)]*\\))?", "").replace("final ", "").strip();
            if (!clean.isEmpty()) {
                names.add(clean);
            }
        }
        return String.join(", ", names);
    }

    /** Splits "Map<K, V> a, String b" on top-level commas only. */
    private static List<String> splitParams(String params) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (char c : params.toCharArray()) {
            if (c == '<' || c == '(') {
                depth++;
            } else if (c == '>' || c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            parts.add(cur.toString());
        }
        return parts.stream().filter(s -> !s.isBlank()).toList();
    }

    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static String camel(String snake) {
        StringBuilder b = new StringBuilder();
        boolean up = false;
        for (char c : snake.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                up = true;
            } else {
                b.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return b.toString();
    }

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
