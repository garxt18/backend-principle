package com.backendprinciple.playground.lab;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

/**
 * Orders files so you never type code that uses a class you have not written yet.
 *
 * <ol>
 *   <li>Group by {@link FileLayer} (build file, config, migrations, entities, repositories, ...).</li>
 *   <li>Inside a layer, sort Java/Kotlin files topologically: if {@code Order.java} mentions
 *       {@code OrderStatus}, then {@code OrderStatus.java} comes first (Kahn's algorithm).
 *       Ties and cycles fall back to alphabetical order so the result is deterministic.</li>
 * </ol>
 */
public final class BuildOrderPlanner {

    public record Candidate(String path, FileLayer layer, String content) {
    }

    private BuildOrderPlanner() {
    }

    public static List<Candidate> order(List<Candidate> files) {
        Map<FileLayer, List<Candidate>> byLayer = new HashMap<>();
        files.forEach(f -> byLayer.computeIfAbsent(f.layer(), l -> new ArrayList<>()).add(f));
        List<Candidate> result = new ArrayList<>(files.size());
        for (FileLayer layer : FileLayer.values()) {
            List<Candidate> group = byLayer.getOrDefault(layer, List.of());
            result.addAll(topologicalWithinLayer(group));
        }
        return result;
    }

    private static final java.util.Set<String> MANIFESTS = java.util.Set.of("pom.xml", "build.gradle", "build.gradle.kts", "package.json");

    private static List<Candidate> topologicalWithinLayer(List<Candidate> group) {
        if (group.size() < 2) {
            return group;
        }
        Map<String, Candidate> byClassName = new HashMap<>();
        for (Candidate c : group) {
            String name = className(c.path());
            if (name != null) {
                byClassName.putIfAbsent(name, c);
            }
        }
        // edge: dependency -> dependent
        Map<Candidate, List<Candidate>> dependents = new HashMap<>();
        Map<Candidate, Integer> inDegree = new HashMap<>();
        group.forEach(c -> inDegree.put(c, 0));
        Map<String, Candidate> byModule = new HashMap<>();
        for (Candidate c : group) {
            String module = moduleKey(c.path());
            if (module != null) {
                byModule.putIfAbsent(module, c);
            }
        }
        for (Candidate c : group) {
            java.util.Set<Candidate> deps = new java.util.LinkedHashSet<>();
            for (var entry : byClassName.entrySet()) {
                if (mentions(c.content(), entry.getKey())) {
                    deps.add(entry.getValue());
                }
            }
            for (String imported : importedModules(c)) {
                Candidate dep = byModule.get(imported);
                if (dep == null) {
                    dep = byModule.get(imported + "/index");
                }
                if (dep != null) {
                    deps.add(dep);
                }
            }
            for (Candidate dep : deps) {
                if (dep != c) {
                    dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(c);
                    inDegree.merge(c, 1, Integer::sum);
                }
            }
        }
        // The project's manifest (pom.xml / package.json) and the Prisma schema open their layer: the rest
        // is derived from them (prisma migrate generates the SQL from schema.prisma).
        Comparator<Candidate> byPath = Comparator.<Candidate>comparingInt(c -> MANIFESTS.contains(c.path())
                        || c.path().endsWith("schema.prisma") ? 0 : 1)
                .thenComparing(Candidate::path);
        PriorityQueue<Candidate> ready = new PriorityQueue<>(byPath);
        inDegree.forEach((c, d) -> {
            if (d == 0) {
                ready.add(c);
            }
        });
        List<Candidate> ordered = new ArrayList<>(group.size());
        Deque<Candidate> remaining = new ArrayDeque<>(group.stream().sorted(byPath).toList());
        while (ordered.size() < group.size()) {
            if (ready.isEmpty()) {
                // Cycle: release the alphabetically-first file that is still waiting.
                Candidate next = remaining.stream().filter(c -> !ordered.contains(c)).findFirst().orElseThrow();
                inDegree.put(next, 0);
                ready.add(next);
            }
            Candidate c = ready.poll();
            if (ordered.contains(c)) {
                continue;
            }
            ordered.add(c);
            for (Candidate d : dependents.getOrDefault(c, List.of())) {
                int left = inDegree.merge(d, -1, Integer::sum);
                if (left == 0 && !ordered.contains(d)) {
                    ready.add(d);
                }
            }
        }
        return ordered;
    }

    private static final Pattern IMPORT = Pattern.compile(
            "(?:import|export)\\s[^'\";]*?from\\s*['\"]([^'\"]+)['\"]|import\\s*['\"]([^'\"]+)['\"]|require\\(['\"]([^'\"]+)['\"]\\)");
    private static final Pattern SCRIPT = Pattern.compile(".*\\.(ts|tsx|js|jsx|mjs|cjs)$");

    /** "components/TaskItem.tsx" -> "components/TaskItem" (null for non-script files). */
    static String moduleKey(String path) {
        if (!SCRIPT.matcher(path).matches()) {
            return null;
        }
        String key = path.substring(0, path.lastIndexOf('.'));
        return key.startsWith("src/") ? key.substring(4) : key;
    }

    /**
     * Project files a TS/JS file imports, as module keys: "@/lib/db" and "../lib/db" both become "lib/db".
     * Package imports ("react", "next/server") are ignored.
     */
    static List<String> importedModules(Candidate c) {
        List<String> result = new ArrayList<>();
        if (!SCRIPT.matcher(c.path()).matches()) {
            return result;
        }
        java.util.regex.Matcher m = IMPORT.matcher(c.content());
        String dir = c.path().contains("/") ? c.path().substring(0, c.path().lastIndexOf('/')) : "";
        if (dir.startsWith("src/")) {
            dir = dir.substring(4);
        } else if (dir.equals("src")) {
            dir = "";
        }
        while (m.find()) {
            String spec = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3);
            String resolved;
            if (spec.startsWith("@/") || spec.startsWith("~/")) {
                resolved = spec.substring(2);
            } else if (spec.startsWith("./") || spec.startsWith("../")) {
                resolved = normalize(dir.isEmpty() ? spec : dir + "/" + spec);
            } else {
                continue;
            }
            if (resolved != null) {
                result.add(resolved.replaceAll("\\.(ts|tsx|js|jsx|mjs|cjs)$", ""));
            }
        }
        return result;
    }

    private static String normalize(String path) {
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (parts.isEmpty()) {
                    return null;
                }
                parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        return String.join("/", parts);
    }

    private static boolean mentions(String content, String className) {
        return Pattern.compile("\\b" + Pattern.quote(className) + "\\b").matcher(content).find();
    }

    private static String className(String path) {
        String name = FileClassifier.fileName(path);
        if (name.endsWith(".java")) {
            return name.substring(0, name.length() - 5);
        }
        if (name.endsWith(".kt")) {
            return name.substring(0, name.length() - 3);
        }
        return null;
    }
}
