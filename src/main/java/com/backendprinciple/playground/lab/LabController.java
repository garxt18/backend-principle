package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final ZipProjectImporter importer;

    public LabController(LabService lab, ZipProjectImporter importer) {
        this.lab = lab;
        this.importer = importer;
    }

    public record ProgressBody(@Min(0) int linesCompleted) {
    }

    public record ProgressResponse(int linesCompleted) {
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
        var imported = importer.read(file.getInputStream());
        return lab.importProject(me.id(), name, description, imported);
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

    @GetMapping("/files/{id}/lines/{line}/explain")
    public LineExplainer.Explanation explain(@CurrentUser AuthUser me, @PathVariable UUID id, @PathVariable int line) {
        return lab.explain(me.id(), id, line);
    }
}
