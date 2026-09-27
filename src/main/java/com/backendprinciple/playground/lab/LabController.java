package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService lab;
    private final LabSyncService sync;
    private final ZipProjectImporter importer;

    public LabController(LabService lab, LabSyncService sync, ZipProjectImporter importer) {
        this.lab = lab;
        this.sync = sync;
        this.importer = importer;
    }

    public record ProgressBody(@Min(0) int linesCompleted) {
    }

    public record ProgressResponse(int linesCompleted) {
    }

    public record NoteBody(@Size(max = 5000) String note) {
    }

    @GetMapping("/projects")
    public List<LabService.ProjectDto> list(@CurrentUser AuthUser me) {
        return lab.list(me.id());
    }

    /** multipart/form-data upload: file=<project.zip>, name=..., description=... */
    @PostMapping(path = "/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LabService.ImportOutcome upload(@CurrentUser AuthUser me,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam("name") String name,
                                           @RequestParam(value = "description", required = false) String description)
            throws IOException {
        if (name.isBlank() || name.length() > 120) {
            throw ApiException.badRequest("Name must be 1-120 characters");
        }
        if (description != null && description.length() > 1000) {
            throw ApiException.badRequest("Description must be at most 1000 characters");
        }
        if (file.isEmpty()) {
            throw ApiException.badRequest("Choose a .zip file to upload");
        }
        var imported = read(file);
        return lab.importProject(me.id(), name, description, imported);
    }

    /** Moves the multipart temp file to our own temp file (no copy through memory, whatever its size). */
    private ZipProjectImporter.ImportResult read(MultipartFile file) throws IOException {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("lab-upload-", ".zip");
        try {
            file.transferTo(tmp);
            return importer.read(tmp);
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    /** Download your progress as a zip to continue on your own computer. */
    @GetMapping("/projects/{id}/progress/export")
    public ResponseEntity<byte[]> exportProgress(@CurrentUser AuthUser me, @PathVariable UUID id) {
        LabSyncService.Export export = sync.export(me.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(export.zip());
    }

    /** Upload the (zipped) folder you kept typing in; matching lines become your progress. */
    @PostMapping(path = "/projects/{id}/progress/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LabSyncService.SyncResult importProgress(@CurrentUser AuthUser me, @PathVariable UUID id,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Choose the .zip of your rebuild folder");
        }
        return sync.sync(me.id(), id, read(file).files());
    }

    @GetMapping("/projects/{id}")
    public LabService.ProjectDetailDto detail(@CurrentUser AuthUser me, @PathVariable UUID id) {
        return lab.detail(me.id(), id);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthUser me, @PathVariable UUID id) {
        lab.delete(me.id(), id);
    }

    @GetMapping("/files/{id}")
    public LabService.FileDto file(@CurrentUser AuthUser me, @PathVariable UUID id) {
        return lab.file(me.id(), id);
    }

    @PutMapping("/files/{id}/progress")
    public ProgressResponse progress(@CurrentUser AuthUser me, @PathVariable UUID id,
                                     @Valid @RequestBody ProgressBody body) {
        return new ProgressResponse(lab.saveProgress(me.id(), id, body.linesCompleted()));
    }

    @PutMapping("/files/{id}/lines/{line}/note")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void note(@CurrentUser AuthUser me, @PathVariable UUID id, @PathVariable int line,
                     @Valid @RequestBody NoteBody body) {
        lab.saveLineNote(me.id(), id, line, body.note());
    }

    @GetMapping("/files/{id}/lines/{line}/explain")
    public LineExplainer.Explanation explain(@CurrentUser AuthUser me, @PathVariable UUID id, @PathVariable int line) {
        return lab.explain(me.id(), id, line);
    }
}
