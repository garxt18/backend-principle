package com.backendprinciple.playground.lab;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Decides a file's language and {@link FileLayer} from its path and content (cheap heuristics, no parsing). */
public final class FileClassifier {

    private static final Map<String, String> LANGUAGE_BY_EXTENSION = Map.ofEntries(
            Map.entry("java", "java"), Map.entry("kt", "kotlin"), Map.entry("xml", "xml"),
            Map.entry("yml", "yaml"), Map.entry("yaml", "yaml"), Map.entry("properties", "properties"),
            Map.entry("sql", "sql"), Map.entry("gradle", "groovy"), Map.entry("kts", "kotlin"),
            Map.entry("md", "markdown"), Map.entry("json", "json"), Map.entry("http", "http"),
            Map.entry("html", "html"), Map.entry("css", "css"), Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"), Map.entry("tsx", "typescript"), Map.entry("jsx", "javascript"), Map.entry("sh", "shell"), Map.entry("env", "properties"),
            Map.entry("txt", "text"), Map.entry("conf", "text"), Map.entry("toml", "text"),
            Map.entry("prisma", "prisma"), Map.entry("mjs", "javascript"), Map.entry("cjs", "javascript"));

    private static final Pattern ENTITY = Pattern.compile("@(Entity|Embeddable|MappedSuperclass|Document)\\b");
    private static final Pattern REPOSITORY = Pattern.compile("@Repository\\b|extends\\s+(Jpa|Crud|PagingAndSorting|Mongo|ListCrud)Repository");
    private static final Pattern CONTROLLER = Pattern.compile("@(RestController|Controller)\\b");
    private static final Pattern ADVICE = Pattern.compile("@(RestControllerAdvice|ControllerAdvice)\\b|class\\s+\\w+\\s+extends\\s+\\w*(Runtime)?Exception\\b");
    private static final Pattern SERVICE = Pattern.compile("@Service\\b");
    private static final Pattern SECURITY = Pattern.compile("@EnableWebSecurity\\b|SecurityFilterChain|OncePerRequestFilter|UserDetailsService|Jwt");
    private static final Pattern CONFIGURATION = Pattern.compile("@(Configuration|ConfigurationProperties)\\b");
    private static final Pattern MAIN = Pattern.compile("@SpringBootApplication\\b|public\\s+static\\s+void\\s+main\\s*\\(");
    private static final Pattern RECORD_OR_ENUM = Pattern.compile("\\b(public\\s+)?(enum|record)\\s+\\w+");

    private FileClassifier() {
    }

    public static boolean isSupported(String path) {
        String name = fileName(path);
        return LANGUAGE_BY_EXTENSION.containsKey(extension(name)) || isSpecialName(name);
    }

    public static String language(String path) {
        String name = fileName(path);
        if (name.equals("Dockerfile") || name.startsWith("Dockerfile.")) {
            return "dockerfile";
        }
        if (name.equals("Makefile")) {
            return "makefile";
        }
        if (name.equals(".gitignore") || name.equals(".dockerignore")) {
            return "text";
        }
        if (name.startsWith(".env")) {
            return "properties";
        }
        return LANGUAGE_BY_EXTENSION.getOrDefault(extension(name), "text");
    }

    public static FileLayer layer(String path, String content, ProjectStack stack) {
        return stack == ProjectStack.NEXTJS ? nextLayer(path, content) : layer(path, content);
    }

    private static final Pattern USE_SERVER = Pattern.compile("\\A(\\s|//[^\\n]*\\n|/\\*.*?\\*/)*['\"]use server['\"]", Pattern.DOTALL);
    private static final Pattern DATA_ACCESS = Pattern.compile("new PrismaClient|\\bprisma\\.\\w+\\.(find|create|update|delete|upsert|count|aggregate)|drizzle\\(|\\bdb\\.(select|insert|update|delete)\\(");
    private static final Pattern ZOD = Pattern.compile("\\bz\\.(object|string|enum|array|number)\\(");

    /**
     * Next.js (App Router, Prisma/Drizzle) layers, in build order: setup -> env -> schema -> types ->
     * data access -> server logic -> proxy/auth -> API routes | styles -> components -> pages.
     */
    static FileLayer nextLayer(String path, String content) {
        String p = path.toLowerCase(Locale.ROOT);
        String name = fileName(p);
        String base = name.replaceFirst("\\.[^.]+$", ""); // next.config.ts -> next.config
        boolean code = p.matches(".*\\.(ts|tsx|js|jsx|mjs|cjs)$");

        if (name.equals("package.json") || base.equals("next.config") || name.startsWith("tsconfig")
                || base.equals("tailwind.config") || base.equals("postcss.config") || base.equals("eslint.config")
                || name.startsWith(".eslintrc") || base.equals("prisma.config") || base.equals("drizzle.config")
                || base.equals("vitest.config") || base.equals("jest.config") || name.equals("components.json")) {
            return FileLayer.BUILD;
        }
        if (name.startsWith(".env")) {
            return FileLayer.CONFIG;
        }
        if (name.endsWith(".prisma") || p.contains("prisma/migrations/") || name.endsWith(".sql")
                || p.matches("(.*/)?(db|drizzle)/schema\\.(ts|js)")) {
            return FileLayer.MIGRATION;
        }
        if (p.matches(".*\\.(test|spec)\\.(ts|tsx|js|jsx)$") || p.contains("__tests__/") || p.startsWith("e2e/")
                || p.startsWith("tests/")) {
            return FileLayer.TEST;
        }
        if (name.startsWith("dockerfile") || name.startsWith("docker-compose") || p.startsWith(".github/")
                || name.equals(".dockerignore") || name.equals("vercel.json")) {
            return FileLayer.INFRA;
        }
        if (name.endsWith(".md")) {
            return FileLayer.DOCS;
        }
        String route = p.startsWith("src/") ? p.substring(4) : p;
        if (route.matches("app/api/.*route\\.(ts|js)") || route.startsWith("pages/api/")) {
            return FileLayer.CONTROLLER;
        }
        if (route.matches("(proxy|middleware)\\.(ts|js)") || base.equals("auth") || base.equals("auth.config")
                || route.startsWith("lib/auth")) {
            return FileLayer.SECURITY;
        }
        if (code && USE_SERVER.matcher(content).find() || base.equals("actions") || route.contains("/actions/")) {
            return FileLayer.SERVICE;
        }
        if (name.endsWith(".css") || name.endsWith(".scss") || route.startsWith("public/")) {
            return FileLayer.FRONTEND;
        }
        if (route.matches("app/(.*/)?(page|layout|loading|error|not-found|template|default|global-error)\\.(tsx|jsx|js|ts)")
                || route.startsWith("pages/")) {
            return FileLayer.UI_PAGE;
        }
        if (name.endsWith(".tsx") || name.endsWith(".jsx") || route.startsWith("components/") || route.startsWith("hooks/")) {
            return FileLayer.UI_COMPONENT;
        }
        if (code && DATA_ACCESS.matcher(content).find()) {
            return FileLayer.REPOSITORY;
        }
        if (route.startsWith("types/") || name.endsWith(".d.ts") || code && ZOD.matcher(content).find()) {
            return FileLayer.DOMAIN;
        }
        if (code) {
            return FileLayer.SERVICE; // lib/ helpers and other server-side code
        }
        return FileLayer.OTHER;
    }

    public static FileLayer layer(String path, String content) {
        String p = path.toLowerCase(Locale.ROOT);
        String name = fileName(path);
        String lowerName = name.toLowerCase(Locale.ROOT);

        if (lowerName.equals("pom.xml") || lowerName.startsWith("build.gradle") || lowerName.startsWith("settings.gradle")) {
            return FileLayer.BUILD;
        }
        if (p.startsWith("src/test/") || p.contains("/src/test/")) {
            return FileLayer.TEST;
        }
        if (lowerName.startsWith("dockerfile") || lowerName.startsWith("docker-compose") || lowerName.startsWith("compose.")
                || p.startsWith(".github/") || p.startsWith("k8s/") || p.startsWith("deploy/") || lowerName.equals(".dockerignore")) {
            return FileLayer.INFRA;
        }
        if (lowerName.endsWith(".md")) {
            return FileLayer.DOCS;
        }
        if (lowerName.endsWith(".sql")) {
            return FileLayer.MIGRATION;
        }
        if (lowerName.startsWith("application") && (lowerName.endsWith(".yml") || lowerName.endsWith(".yaml")
                || lowerName.endsWith(".properties")) || lowerName.equals(".env.example")) {
            return FileLayer.CONFIG;
        }
        if (p.startsWith("frontend/") || p.contains("/static/") || p.contains("/templates/")
                || lowerName.endsWith(".html") || lowerName.endsWith(".css") || lowerName.endsWith(".js")
                || lowerName.endsWith(".jsx") || lowerName.endsWith(".ts") || lowerName.endsWith(".tsx")) {
            return FileLayer.FRONTEND;
        }
        if (!lowerName.endsWith(".java") && !lowerName.endsWith(".kt")) {
            return p.contains("/resources/") ? FileLayer.CONFIG : FileLayer.OTHER;
        }

        // Java/Kotlin source: annotations are the most reliable signal, package names second.
        if (MAIN.matcher(content).find() && content.contains("SpringApplication")) {
            return FileLayer.MAIN;
        }
        if (CONTROLLER.matcher(content).find()) {
            return FileLayer.CONTROLLER;
        }
        if (ADVICE.matcher(content).find() || p.contains("/exception/") || p.contains("/error/")) {
            return FileLayer.EXCEPTION;
        }
        if (ENTITY.matcher(content).find()) {
            return FileLayer.DOMAIN;
        }
        if (REPOSITORY.matcher(content).find()) {
            return FileLayer.REPOSITORY;
        }
        if (p.contains("/security/") || p.contains("/auth/") && SECURITY.matcher(content).find()) {
            return FileLayer.SECURITY;
        }
        if (SERVICE.matcher(content).find()) {
            return FileLayer.SERVICE;
        }
        if (SECURITY.matcher(content).find() && CONFIGURATION.matcher(content).find()) {
            return FileLayer.SECURITY;
        }
        if (CONFIGURATION.matcher(content).find() || p.contains("/config/")) {
            return FileLayer.SPRING_CONFIG;
        }
        if (p.contains("/dto/") || lowerName.matches(".*(request|response|dto)\\.(java|kt)")) {
            return FileLayer.DTO;
        }
        if (p.contains("/entity/") || p.contains("/model/") || p.contains("/domain/")
                || RECORD_OR_ENUM.matcher(content).find() && content.contains("enum ")) {
            return FileLayer.DOMAIN;
        }
        if (p.contains("/repository/")) {
            return FileLayer.REPOSITORY;
        }
        if (p.contains("/service/")) {
            return FileLayer.SERVICE;
        }
        if (p.contains("/controller/") || p.contains("/web/")) {
            return FileLayer.CONTROLLER;
        }
        return FileLayer.SERVICE; // plain helper classes usually sit with business logic
    }

    static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static boolean isSpecialName(String name) {
        return name.equals("Dockerfile") || name.startsWith("Dockerfile.") || name.equals("Makefile")
                || name.equals(".gitignore") || name.equals(".dockerignore") || name.equals("mvnw")
                || name.equals(".env.example") || name.equals(".env.local.example");
    }
}
