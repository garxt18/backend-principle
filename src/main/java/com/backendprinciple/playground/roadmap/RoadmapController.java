package com.backendprinciple.playground.roadmap;

import com.backendprinciple.playground.roadmap.RoadmapDtos.LevelDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.ResourceDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoadmapController {

    private final RoadmapService roadmap;

    public RoadmapController(RoadmapService roadmap) {
        this.roadmap = roadmap;
    }

    /** Public: anyone can browse the roadmap. Browsers may cache it for 5 minutes. */
    @GetMapping("/api/roadmap")
    public ResponseEntity<List<LevelDto>> roadmap() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(roadmap.fullRoadmap());
    }

    @GetMapping("/api/roadmap/levels/{slug}")
    public LevelDto level(@PathVariable String slug) {
        return roadmap.level(slug);
    }

    public record ResourceBody(
            @NotNull Long levelId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 1000) @Pattern(regexp = "https://.+", message = "must be an https:// URL") String url,
            @Size(max = 120) String channel,
            @NotNull ResourceLanguage language,
            @NotNull ResourceKind kind,
            boolean primaryPick,
            @Size(max = 500) String note,
            int orderIndex) {

        RoadmapService.ResourceRequest toRequest() {
            return new RoadmapService.ResourceRequest(levelId, title, url, channel, language, kind, primaryPick, note,
                    orderIndex);
        }
    }

    @PostMapping("/api/admin/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto create(@Valid @RequestBody ResourceBody body) {
        return roadmap.createResource(body.toRequest());
    }

    @PutMapping("/api/admin/resources/{id}")
    public ResourceDto update(@PathVariable Long id, @Valid @RequestBody ResourceBody body) {
        return roadmap.updateResource(id, body.toRequest());
    }

    @DeleteMapping("/api/admin/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roadmap.deleteResource(id);
    }
}
