package com.backendprinciple.playground.progress;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    public record UpdateBody(TopicStatus status,
                             @Min(1) @Max(5) Short confidence,
                             @Size(max = 10_000) String notes) {
    }

    @GetMapping
    public ProgressService.Summary summary(@CurrentUser AuthUser me) {
        return progressService.summary(me.id());
    }

    /** PUT is idempotent: sending the same body twice leaves the same state. */
    @PutMapping("/topics/{topicId}")
    public ProgressService.TopicProgressDto update(@CurrentUser AuthUser me, @PathVariable Long topicId,
                                                   @Valid @RequestBody UpdateBody body) {
        return progressService.update(me.id(), topicId,
                new ProgressService.UpdateRequest(body.status(), body.confidence(), body.notes()));
    }
}
