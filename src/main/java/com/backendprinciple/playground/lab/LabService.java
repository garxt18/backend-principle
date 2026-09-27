package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.lab.BuildOrderPlanner.Candidate;
import com.backendprinciple.playground.lab.LabFileRepository.FileSummary;
import com.backendprinciple.playground.lab.ZipProjectImporter.ImportedFile;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabService {

    private final LabProjectRepository projects;
    private final LabFileRepository files;
    private final LabFileProgressRepository progress;
    private final LabLineNoteRepository notes;
    private final LineExplainer explainer;
    private final LabProperties props;
    private final Clock clock;

    public LabService(LabProjectRepository projects, LabFileRepository files, LabFileProgressRepository progress,
                      LabLineNoteRepository notes, LineExplainer explainer, LabProperties props, Clock clock) {
        this.notes = notes;
        this.projects = projects;
        this.files = files;
        this.progress = progress;
        this.explainer = explainer;
        this.props = props;
        this.clock = clock;
    }

    // ---- DTOs -------------------------------------------------------------------------------------

    public record ProjectDto(UUID id, String name, String description, boolean template, int fileCount,
                             int totalLines, Instant createdAt) {
        static ProjectDto from(LabProject p) {
            return new ProjectDto(p.getId(), p.getName(), p.getDescription(), p.isTemplate(), p.getFileCount(),
                    p.getTotalLines(), p.getCreatedAt());
        }
    }

    public record FileEntryDto(UUID id, String path, String language, FileLayer layer, String layerLabel,
                               FileLayer.Track track, int buildOrder, int lineCount, int linesCompleted, boolean completed) {
    }

    public record LayerDto(FileLayer layer, String label, String why) {
    }

    public record ProjectDetailDto(ProjectDto project, int linesCompleted, int percent, List<LayerDto> layers,
                                   List<FileEntryDto> files) {
    }

    /**
     * @param outline        "before you type": what the file is for and the members it contains
     * @param previousFileId previous file in the same track (backend or frontend)
     * @param positionInTrack 1-based position of this file within its track, out of {@code filesInTrack}
     */
    public record FileDto(UUID id, UUID projectId, String path, String language, FileLayer layer, String layerLabel,
                          String layerWhy, FileLayer.Track track, int buildOrder, int positionInTrack, int filesInTrack,
                          int lineCount, int linesCompleted, List<String> lines, Map<Integer, String> notes,
                          LineExplainer.Outline outline, UUID previousFileId, UUID nextFileId) {
    }

    public record ImportOutcome(ProjectDto project, List<String> skipped) {
    }

    // ---- queries ----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjectDto> list(UUID userId) {
        return projects.findVisibleTo(userId).stream().map(ProjectDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailDto detail(UUID userId, UUID projectId) {
        LabProject project = visibleProject(userId, projectId);
        Map<UUID, LabFileProgress> byFile = progress.findForProject(userId, projectId).stream()
                .collect(Collectors.toMap(LabFileProgress::getFileId, Function.identity()));
        List<FileEntryDto> entries = new ArrayList<>();
        int done = 0;
        for (FileSummary f : files.findSummaries(projectId)) {
            LabFileProgress p = byFile.get(f.id());
            int lines = p == null ? 0 : p.getLinesCompleted();
            done += lines;
            entries.add(new FileEntryDto(f.id(), f.path(), f.language(), f.layer(), f.layer().label(), f.layer().track(),
                    f.buildOrder(),
                    f.lineCount(), lines, p != null && p.isCompleted()));
        }
        List<LayerDto> layers = entries.stream().map(FileEntryDto::layer).distinct()
                .map(l -> new LayerDto(l, l.label(), l.why())).toList();
        int percent = project.getTotalLines() == 0 ? 0 : Math.round(done * 100f / project.getTotalLines());
        return new ProjectDetailDto(ProjectDto.from(project), done, percent, layers, entries);
    }

    @Transactional(readOnly = true)
    public FileDto file(UUID userId, UUID fileId) {
        LabFile f = visibleFile(userId, fileId);
        FileLayer.Track track = f.getLayer().track();
        List<FileSummary> siblings = files.findSummaries(f.getProjectId()).stream()
                .filter(s -> s.layer().track() == track).toList();
        UUID prev = null;
        UUID next = null;
        int position = 0;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id().equals(fileId)) {
                prev = i > 0 ? siblings.get(i - 1).id() : null;
                next = i < siblings.size() - 1 ? siblings.get(i + 1).id() : null;
                position = i + 1;
            }
        }
        int done = progress.findByUserIdAndFileId(userId, fileId).map(LabFileProgress::getLinesCompleted).orElse(0);
        List<String> lines = f.getContent().lines().toList();
        return new FileDto(f.getId(), f.getProjectId(), f.getPath(), f.getLanguage(), f.getLayer(), f.getLayer().label(),
                f.getLayer().why(), track, f.getBuildOrder(), position, siblings.size(), f.getLineCount(), done, lines,
                notesOf(userId, fileId), explainer.outline(lines, f.getLanguage(), f.getLayer()), prev, next);
    }

    @Transactional(readOnly = true)
    public LineExplainer.Explanation explain(UUID userId, UUID fileId, int lineNumber) {
        LabFile f = visibleFile(userId, fileId);
        List<String> lines = f.getContent().lines().toList();
        if (lineNumber < 1 || lineNumber > lines.size()) {
            throw ApiException.badRequest("Line number out of range");
        }
        return explainer.explain(lines, lineNumber, f.getLanguage(), f.getLayer());
    }

    private Map<Integer, String> notesOf(UUID userId, UUID fileId) {
        Map<Integer, String> result = new java.util.TreeMap<>();
        notes.findByUserIdAndFileIdOrderByLineNumber(userId, fileId).forEach(n -> result.put(n.getLineNumber(), n.getNote()));
        return result;
    }

    // ---- commands ---------------------------------------------------------------------------------

    /** Saves (or, with blank text, deletes) the learner's own explanation of a line. */
    @Transactional
    public void saveLineNote(UUID userId, UUID fileId, int lineNumber, String text) {
        LabFile f = visibleFile(userId, fileId);
        if (lineNumber < 1 || lineNumber > f.getLineCount()) {
            throw ApiException.badRequest("Line number out of range");
        }
        var existing = notes.findByUserIdAndFileIdAndLineNumber(userId, fileId, lineNumber);
        if (text == null || text.isBlank()) {
            existing.ifPresent(notes::delete);
            return;
        }
        LabLineNote note = existing.orElseGet(() -> new LabLineNote(userId, fileId, lineNumber));
        note.write(text.strip(), clock.instant());
        notes.save(note);
    }

    @Transactional
    public int saveProgress(UUID userId, UUID fileId, int linesCompleted) {
        LabFile f = visibleFile(userId, fileId);
        LabFileProgress p = progress.findByUserIdAndFileId(userId, fileId)
                .orElseGet(() -> new LabFileProgress(userId, fileId));
        p.record(linesCompleted, f.getLineCount(), clock.instant());
        progress.save(p);
        return p.getLinesCompleted();
    }

    @Transactional
    public ImportOutcome importProject(UUID userId, String name, String description,
                                       ZipProjectImporter.ImportResult imported) {
        if (projects.countByOwnerId(userId) >= props.maxProjectsPerUser()) {
            throw ApiException.conflict("You can keep at most " + props.maxProjectsPerUser()
                    + " projects - delete one first");
        }
        if (imported.files().isEmpty()) {
            throw ApiException.badRequest("No source files found in the zip (.java, .yml, .xml, .sql, ...)");
        }
        LabProject project = saveProject(userId, null, name, description, LabProject.SourceKind.UPLOAD, imported.files());
        return new ImportOutcome(ProjectDto.from(project), imported.skipped());
    }

    /** Shared by uploads and template seeding: classify, order, persist. */
    LabProject saveProject(UUID ownerId, String slug, String name, String description, LabProject.SourceKind kind,
                           List<ImportedFile> imported) {
        LabProject project = new LabProject(ownerId, slug, name.strip(), description, kind, clock.instant());
        List<Candidate> ordered = BuildOrderPlanner.order(imported.stream()
                .map(f -> new Candidate(f.path(), FileClassifier.layer(f.path(), f.content()), f.content()))
                .toList());
        List<LabFile> entities = new ArrayList<>(ordered.size());
        int totalLines = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Candidate c = ordered.get(i);
            LabFile file = new LabFile(project.getId(), c.path(), FileClassifier.language(c.path()), c.layer(), i, c.content());
            totalLines += file.getLineCount();
            entities.add(file);
        }
        project.setStats(entities.size(), totalLines);
        projects.save(project);
        files.saveAll(entities);
        return project;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        LabProject p = projects.findById(projectId)
                .filter(x -> !x.isTemplate() && x.getOwnerId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Project"));
        projects.delete(p); // files and progress go with it (ON DELETE CASCADE)
    }

    // ---- ownership checks -------------------------------------------------------------------------

    LabProject visibleProject(UUID userId, UUID projectId) {
        // Someone else's private project answers 404, not 403, so ids cannot be probed.
        return projects.findById(projectId).filter(p -> p.isVisibleTo(userId))
                .orElseThrow(() -> ApiException.notFound("Project"));
    }

    private LabFile visibleFile(UUID userId, UUID fileId) {
        LabFile f = files.findById(fileId).orElseThrow(() -> ApiException.notFound("File"));
        visibleProject(userId, f.getProjectId());
        return f;
    }
}
