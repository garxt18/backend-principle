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
}
