package com.backendprinciple.playground.mentor;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mentor")
public class MentorController {

    private final MentorService mentor;

    public MentorController(MentorService mentor) {
        this.mentor = mentor;
    }

    public record AskBody(@NotBlank @Size(max = 2000) String question, Long topicId, UUID fileId,
                          @Min(1) Integer lineNumber) {
    }

    @GetMapping("/status")
    public MentorService.Status status(@CurrentUser AuthUser me) {
        return mentor.status(me.id());
    }

    @PostMapping("/ask")
    public MentorService.Answer ask(@CurrentUser AuthUser me, @Valid @RequestBody AskBody body) {
        return mentor.ask(me.id(), body.question(), body.topicId(), body.fileId(), body.lineNumber());
    }

    @PostMapping("/explain/{fileId}/{line}")
    public MentorService.Answer explainLine(@CurrentUser AuthUser me, @PathVariable UUID fileId, @PathVariable int line) {
        return mentor.explainLine(me.id(), fileId, line);
    }

    @PostMapping("/quiz/{topicId}")
    public MentorService.Quiz quiz(@CurrentUser AuthUser me, @PathVariable Long topicId) {
        return mentor.quiz(me.id(), topicId);
    }
}
