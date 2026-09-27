package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.lab.ZipProjectImporter.ImportedFile;
import java.util.List;
import java.util.regex.Pattern;

/** The kind of project in the Rebuild Lab; decides how files are classified, ordered and named. */
public enum ProjectStack {
    SPRING("Spring Boot"),
    NEXTJS("Next.js");

    private static final Pattern NEXT_DEPENDENCY = Pattern.compile("\"next\"\\s*:");

    private final String label;

    ProjectStack(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** A package.json that depends on "next" (or a next.config file) makes it a Next.js project. */
    public static ProjectStack detect(List<ImportedFile> files) {
        for (ImportedFile f : files) {
            String name = FileClassifier.fileName(f.path());
            if (name.startsWith("next.config.")
                    || name.equals("package.json") && NEXT_DEPENDENCY.matcher(f.content()).find()) {
                return NEXTJS;
            }
        }
        return SPRING;
    }
}
