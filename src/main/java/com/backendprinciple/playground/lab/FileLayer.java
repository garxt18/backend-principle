package com.backendprinciple.playground.lab;

/**
 * The "layer" a file belongs to, in the order a senior engineer would build a Spring Boot project
 * from scratch. The Rebuild Lab walks you through files in this order, so every file you type only
 * depends on files you have already typed.
 */
public enum FileLayer {
    BUILD("Build file", "Start here: the build file declares Java version and every library the project uses."),
    CONFIG("Configuration", "application.yml/properties: database URL, ports and feature switches, kept outside code."),
    MIGRATION("Database migrations", "SQL that creates the tables. The schema comes before the Java that maps to it."),
    MAIN("Application entry point", "The class with main() and @SpringBootApplication that boots everything."),
    DOMAIN("Entities & domain model", "Entities, enums and value objects: the nouns of your system and how they map to tables."),
    REPOSITORY("Repositories", "Data access: Spring Data interfaces that turn method names into SQL."),
    DTO("DTOs", "Request/response shapes of your API, separate from entities so the database can change freely."),
    EXCEPTION("Errors", "Custom exceptions and the global handler that turns them into clean JSON errors."),
    SERVICE("Services", "Business logic and @Transactional boundaries. Controllers stay thin because of this layer."),
    SECURITY("Security", "Authentication/authorization: filters, JWT, password hashing, access rules."),
    SPRING_CONFIG("Spring configuration", "@Configuration classes that define extra beans and wire infrastructure."),
    CONTROLLER("Controllers", "HTTP endpoints: routing, validation, status codes. The outermost layer."),
    FRONTEND("Frontend", "Static files the browser loads: HTML, CSS and JavaScript."),
    UI_COMPONENT("Components", "Reusable UI pieces the pages are built from."),
    UI_PAGE("Pages & layouts", "The screens users visit, built from the components above."),
    TEST("Tests", "Unit and integration tests that prove every layer above works."),
    INFRA("Infrastructure", "Dockerfile, docker-compose, CI pipelines, Kubernetes manifests: how the code ships."),
    DOCS("Documentation", "README and docs: how to run and understand the project."),
    OTHER("Other", "Supporting files.");

    private final String label;
    private final String why;

    FileLayer(String label, String why) {
        this.label = label;
        this.why = why;
    }

    public String label() {
        return label;
    }

    public String why() {
        return why;
    }

    /** The Rebuild Lab keeps backend + database files in the main track; frontend files are optional practice. */
    public Track track() {
        return this == FRONTEND || this == UI_COMPONENT || this == UI_PAGE ? Track.FRONTEND : Track.BACKEND;
    }

    /** Stack-specific name, e.g. "API route handlers" instead of "Controllers" in a Next.js project. */
    public String label(ProjectStack stack) {
        return stack == ProjectStack.NEXTJS ? next()[0] : label;
    }

    public String why(ProjectStack stack) {
        return stack == ProjectStack.NEXTJS ? next()[1] : why;
    }

    private String[] next() {
        return switch (this) {
            case BUILD -> new String[]{"Project setup", "package.json (dependencies and scripts), next.config and tsconfig: "
                    + "what the app is made of and how it builds."};
            case CONFIG -> new String[]{"Environment", ".env.example: the database URL and secrets the app needs, kept out of the code."};
            case MIGRATION -> new String[]{"Database schema", "prisma/schema.prisma and its SQL migrations: the tables come "
                    + "before the code that reads them."};
            case DOMAIN -> new String[]{"Types & validation", "Shared TypeScript types and zod schemas that check every input."};
            case REPOSITORY -> new String[]{"Data access", "The database client and the functions that query it - the only "
                    + "place that talks to the database."};
            case SERVICE -> new String[]{"Server logic", "Server actions ('use server') and server-only helpers: the business rules."};
            case SECURITY -> new String[]{"Proxy & auth", "proxy.ts (middleware) and auth helpers that run before a request "
                    + "reaches a route."};
            case CONTROLLER -> new String[]{"API route handlers", "app/api/**/route.ts: GET/POST/PATCH/DELETE functions "
                    + "that answer HTTP requests with JSON."};
            case FRONTEND -> new String[]{"Styles & assets", "Global CSS and static files."};
            case UI_COMPONENT -> new String[]{"Components", "Reusable React components - server components by default, "
                    + "'use client' when they need state or event handlers."};
            case UI_PAGE -> new String[]{"Pages & layouts", "app/**/layout.tsx and page.tsx: every folder is a URL, "
                    + "every page.tsx is what that URL shows."};
            default -> new String[]{label, why};
        };
    }

    public enum Track { BACKEND, FRONTEND }
}
