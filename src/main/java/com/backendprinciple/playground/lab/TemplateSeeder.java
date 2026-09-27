package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.lab.ZipProjectImporter.ImportedFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes the built-in projects under classpath:lab-templates/ as shared Rebuild Lab templates:
 * <ul>
 *   <li>task-manager-api - the roadmap's "Project 1", small enough to rebuild in a weekend.</li>
 *   <li>nextjs-task-board - the same idea as a full-stack Next.js app.</li>
 *   <li>backend-playground - this very application (copied in by the Maven build), so you can
 *       rebuild the platform you are using.</li>
 * </ul>
 * A template is only inserted once; bump its slug (e.g. -v2) to publish a new version. Templates whose slug is no
 * longer listed here (older versions) are removed on startup, together with progress on them.
 */
@Component
@Order(2)
public class TemplateSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TemplateSeeder.class);

    record Template(String slug, String folder, String name, String description) {
    }

    static final List<Template> TEMPLATES = List.of(
            new Template("task-manager-api-v1", "task-manager-api", "Task Manager API (Roadmap Project 1)",
                    "A small, clean Spring Boot + PostgreSQL CRUD API: entity, repository, DTOs, validation, "
                            + "error handling, service, controller, tests and Dockerfile. Rebuild this first."),
            new Template("nextjs-task-board-v1", "nextjs-task-board", "Next.js Task Board (App Router + Prisma)",
                    "A small full-stack Next.js 16 app: Prisma 7 + PostgreSQL, zod validation, a data-access layer, "
                            + "server actions, REST route handlers behind proxy.ts, and server + client components."),
            new Template("backend-playground-v2", "backend-playground", "Backend Playground (this app)",
                    "The full source of this platform: JWT auth with refresh-token rotation, rate limiting, "
                            + "caching, Planly planner, DSA sheet and the Rebuild Lab. The advanced rebuild."));

    private final LabService lab;
    private final LabProjectRepository projects;

    public TemplateSeeder(LabService lab, LabProjectRepository projects) {
        this.lab = lab;
        this.projects = projects;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        var current = TEMPLATES.stream().map(Template::slug).toList();
        for (LabProject old : projects.findAllTemplates()) {
            if (!current.contains(old.getSlug())) {
                projects.delete(old);
                log.info("Removed outdated Rebuild Lab template {}", old.getSlug());
            }
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Template t : TEMPLATES) {
            if (projects.findTemplate(t.slug()).isPresent()) {
                continue;
            }
            String base = "lab-templates/" + t.folder() + "/";
            List<ImportedFile> files = new ArrayList<>();
            for (Resource r : resolver.getResources("classpath*:" + base + "**/*")) {
                if (!r.isReadable()) {
                    continue; // directories
                }
                String url = r.getURL().toString();
                int idx = url.lastIndexOf(base);
                String path = url.substring(idx + base.length());
                if (path.isEmpty() || !FileClassifier.isSupported(path)) {
                    continue;
                }
                files.add(new ImportedFile(path, new String(r.getContentAsByteArray(), StandardCharsets.UTF_8)
                        .replace("\r\n", "\n")));
            }
            if (files.isEmpty()) {
                log.info("Template {} not on the classpath - skipped", t.slug());
                continue;
            }
            lab.saveProject(null, t.slug(), t.name(), t.description(), LabProject.SourceKind.TEMPLATE, files);
            log.info("Published Rebuild Lab template {} ({} files)", t.slug(), files.size());
        }
    }
}
