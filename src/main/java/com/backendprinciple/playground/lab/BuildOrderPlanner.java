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
        for (Candidate c : group) {
            for (var entry : byClassName.entrySet()) {
                Candidate dep = entry.getValue();
                if (dep != c && mentions(c.content(), entry.getKey())) {
                    dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(c);
                    inDegree.merge(c, 1, Integer::sum);
                }
            }
        }
        Comparator<Candidate> byPath = Comparator.comparing(Candidate::path);
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
