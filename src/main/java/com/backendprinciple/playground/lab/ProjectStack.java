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

    /**
     * The manifest closest to the project root decides: a root pom.xml / build.gradle makes it Spring even if a
     * Next.js app sits deeper inside (e.g. a template under src/main/resources), and a root package.json that
     * depends on "next" (or a next.config file) makes it Next.js. On a tie, Spring wins.
     */
    public static ProjectStack detect(List<ImportedFile> files) {
        int springDepth = Integer.MAX_VALUE;
        int nextDepth = Integer.MAX_VALUE;
        for (ImportedFile f : files) {
            String name = FileClassifier.fileName(f.path());
            int depth = (int) f.path().chars().filter(c -> c == '/').count();
            if (name.equals("pom.xml") || name.startsWith("build.gradle")) {
                springDepth = Math.min(springDepth, depth);
            } else if (name.startsWith("next.config.")
                    || name.equals("package.json") && NEXT_DEPENDENCY.matcher(f.content()).find()) {
                nextDepth = Math.min(nextDepth, depth);
            }
        }
        return nextDepth < springDepth ? NEXTJS : SPRING;
    }
}
