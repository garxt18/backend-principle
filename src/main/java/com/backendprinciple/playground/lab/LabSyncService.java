package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.lab.ZipProjectImporter.ImportedFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Moves Rebuild Lab progress between the website and the learner's own computer.
 * <ul>
 *   <li><b>Export</b>: a zip with every file you have started, cut at the line you reached, plus the full
 *       original under {@code _reference/} and a {@code .rebuild-progress.json} manifest. Open it in
 *       IntelliJ/VS Code and keep typing there.</li>
 *   <li><b>Import</b>: upload that folder (zipped) again. For every file, the leading lines that match the
 *       original (ignoring indentation and blank lines) become your progress. Progress never goes
 *       backwards, and the first line that does not match is reported so you can fix it.</li>
 * </ul>
 */
@Service
public class LabSyncService {

    static final String MANIFEST = ".rebuild-progress.json";
    static final String REFERENCE_DIR = "_reference/";

    private final LabService lab;
    private final LabFileRepository files;
    private final LabFileProgressRepository progress;
    private final ObjectMapper json;
    private final Clock clock;

    public LabSyncService(LabService lab, LabFileRepository files, LabFileProgressRepository progress, ObjectMapper json,
                          Clock clock) {
        this.lab = lab;
        this.files = files;
        this.progress = progress;
        this.json = json;
        this.clock = clock;
    }

    public record Export(String fileName, byte[] zip) {
    }

    record ManifestFile(String path, String track, int lineCount, int linesCompleted, boolean completed) {
    }

    record Manifest(String format, UUID projectId, String projectName, String exportedAt, int filesCompleted,
                    int filesTotal, int linesCompleted, int linesTotal, List<ManifestFile> files) {
    }

    /** @param mismatchLine 1-based line in YOUR file that differs from the original (null = no mismatch) */
    public record FileSync(String path, int before, int after, int lineCount, Integer mismatchLine, String expected,
                           String found) {
    }

    public record SyncResult(int filesMatched, int filesAdvanced, int linesBefore, int linesAfter,
                             List<FileSync> files, List<String> unmatched) {
    }

    // ------------------------------------------------------------------------------------ export

    @Transactional(readOnly = true)
    public Export export(UUID userId, UUID projectId) {
        LabProject project = lab.visibleProject(userId, projectId);
        List<LabFile> all = files.findByProjectIdOrderByBuildOrder(projectId);
        Map<UUID, LabFileProgress> byFile = progressByFile(userId, projectId);
        String root = slug(project.getName()) + "-rebuild/";

        List<ManifestFile> entries = new ArrayList<>();
        int filesDone = 0;
        int linesDone = 0;
        int linesTotal = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            StringBuilder next = new StringBuilder();
            for (LabFile f : all) {
                LabFileProgress p = byFile.get(f.getId());
                int done = p == null ? 0 : Math.min(p.getLinesCompleted(), f.getLineCount());
                boolean completed = p != null && p.isCompleted();
                entries.add(new ManifestFile(f.getPath(), f.getLayer().track().name(), f.getLineCount(), done, completed));
                filesDone += completed ? 1 : 0;
                linesDone += done;
                linesTotal += f.getLineCount();
                if (done > 0) {
                    List<String> lines = f.getContent().lines().toList();
                    put(zip, root + f.getPath(), String.join("\n", lines.subList(0, done)) + "\n");
                }
                if (!completed && next.isEmpty() && f.getLayer().track() == FileLayer.Track.BACKEND) {
                    next.append(f.getPath()).append(" - continue at line ").append(done + 1);
                }
                put(zip, root + REFERENCE_DIR + f.getPath(), f.getContent());
            }
            Manifest manifest = new Manifest("backend-playground-rebuild/v1", project.getId(), project.getName(),
                    clock.instant().toString(), filesDone, all.size(), linesDone, linesTotal, entries);
            put(zip, root + MANIFEST, json.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
            put(zip, root + "README-REBUILD.md", readme(project, filesDone, all.size(), linesDone, linesTotal,
                    next.isEmpty() ? "everything in the backend track is done!" : next.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Export(slug(project.getName()) + "-rebuild-progress.zip", bytes.toByteArray());
    }

    private static String readme(LabProject p, int filesDone, int files, int linesDone, int lines, String next) {
        return """
                # Rebuild progress: %s

                You have rebuilt **%d of %d files** (%d of %d lines).
                Next up: %s

                ## Keep going on your own computer
                1. Open this folder in IntelliJ IDEA or VS Code.
                2. Each file you started contains exactly the lines you have typed so far.
                   Continue typing it - the full original is in `_reference/` next to it (try not to peek!).
                3. New files: create them at the same path as in `_reference/`.

                ## Sync back to the website
                Zip this whole folder and upload it on the project page ("Sync from computer").
                For each file, the lines at the top that match the original (indentation and blank lines
                do not matter) become your progress. Progress never goes backwards, and the first line that
                differs is shown to you so you can fix it.

                Do not rename the files - they are matched by their path.
                """.formatted(p.getName(), filesDone, files, linesDone, lines, next);
    }

    private static void put(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ------------------------------------------------------------------------------------ import

    @Transactional
    public SyncResult sync(UUID userId, UUID projectId, List<ImportedFile> uploaded) {
        lab.visibleProject(userId, projectId);
        List<ImportedFile> candidates = uploaded.stream()
                .filter(f -> !f.path().startsWith(REFERENCE_DIR) && !f.path().contains("/" + REFERENCE_DIR)
                        && !f.path().endsWith(MANIFEST) && !f.path().endsWith("README-REBUILD.md"))
                .toList();
        if (candidates.isEmpty()) {
            throw ApiException.badRequest("The zip contains no project files to sync");
        }
        List<LabFile> all = files.findByProjectIdOrderByBuildOrder(projectId);
        Map<UUID, LabFileProgress> byFile = progressByFile(userId, projectId);

        List<FileSync> results = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        int before = 0;
        int after = 0;
        int advanced = 0;
        for (ImportedFile up : candidates) {
            LabFile target = match(all, up.path());
            if (target == null) {
                unmatched.add(up.path());
                continue;
            }
            LabFileProgress p = byFile.computeIfAbsent(target.getId(), id -> new LabFileProgress(userId, id));
            int old = p.getLinesCompleted();
            Comparison c = compare(target.getContent().lines().toList(), up.content().lines().toList());
            int now = Math.max(old, c.matchedLines());
            if (now > old) {
                p.record(now, target.getLineCount(), clock.instant());
                progress.save(p);
                advanced++;
            }
            before += old;
            after += now;
            results.add(new FileSync(target.getPath(), old, now, target.getLineCount(), c.mismatchLine(), c.expected(),
                    c.found()));
        }
        return new SyncResult(results.size(), advanced, before, after, results, unmatched);
    }

    /** Exact path first, then the unique file whose path ends with the uploaded one (or vice versa). */
    static LabFile match(List<LabFile> all, String path) {
        for (LabFile f : all) {
            if (f.getPath().equals(path)) {
                return f;
            }
        }
        List<LabFile> suffix = all.stream()
                .filter(f -> f.getPath().endsWith("/" + path) || path.endsWith("/" + f.getPath()))
                .toList();
        return suffix.size() == 1 ? suffix.getFirst() : null;
    }

    record Comparison(int matchedLines, Integer mismatchLine, String expected, String found) {
    }

    /**
     * How many lines of the original are "done": walks both files skipping blank lines and compares lines
     * with whitespace collapsed, exactly like the typing trainer does.
     */
    static Comparison compare(List<String> original, List<String> local) {
        int i = 0;
        int j = 0;
        int matched = 0;
        while (true) {
            while (i < original.size() && original.get(i).isBlank()) {
                i++;
            }
            while (j < local.size() && local.get(j).isBlank()) {
                j++;
            }
            if (i >= original.size()) {
                return new Comparison(original.size(), null, null, null);
            }
            if (j >= local.size()) {
                return new Comparison(matched, null, null, null);
            }
            if (!normalize(original.get(i)).equals(normalize(local.get(j)))) {
                return new Comparison(matched, j + 1, original.get(i).strip(), local.get(j).strip());
            }
            i++;
            j++;
            matched = i;
        }
    }

    private static String normalize(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    private Map<UUID, LabFileProgress> progressByFile(UUID userId, UUID projectId) {
        return progress.findForProject(userId, projectId).stream()
                .collect(Collectors.toMap(LabFileProgress::getFileId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    static String slug(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "project" : s.length() > 60 ? s.substring(0, 60) : s;
    }
}
