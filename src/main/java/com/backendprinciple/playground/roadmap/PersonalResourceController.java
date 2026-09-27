package com.backendprinciple.playground.roadmap;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The learner's own layer on the roadmap: followed resources and personal links. */
@RestController
@RequestMapping("/api/me/resources")
public class PersonalResourceController {

    private final PersonalResourceService service;

    public PersonalResourceController(PersonalResourceService service) {
        this.service = service;
    }

    /** Only http(s) links - a "javascript:" URL rendered as a link would run script on click. */
    public record LinkBody(Long levelId, Long topicId,
                           @NotBlank @Size(max = 200) String title,
                           @NotBlank @Size(max = 1000)
                           @Pattern(regexp = "(?i)https?://\\S+", message = "must be an http(s):// link") String url,
                           @Size(max = 500) String note) {
    }

    public record FollowBody(Long resourceId, Long userResourceId) {
    }

    @GetMapping
    public PersonalResourceService.MyResources mine(@CurrentUser AuthUser me) {
        return service.mine(me.id());
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalResourceService.LinkDto addLink(@CurrentUser AuthUser me, @Valid @RequestBody LinkBody body) {
        return service.addLink(me.id(), new PersonalResourceService.NewLink(body.levelId(), body.topicId(),
                body.title(), body.url(), body.note()));
    }

    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@CurrentUser AuthUser me, @PathVariable Long id) {
        service.deleteLink(me.id(), id);
    }

    @PutMapping("/follow/{levelId}")
    public PersonalResourceService.ChoiceDto follow(@CurrentUser AuthUser me, @PathVariable Long levelId,
                                                    @RequestBody FollowBody body) {
        return service.follow(me.id(), levelId, body.resourceId(), body.userResourceId());
    }

    @DeleteMapping("/follow/{levelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@CurrentUser AuthUser me, @PathVariable Long levelId) {
        service.unfollow(me.id(), levelId);
    }
}
