package com.backendprinciple.playground.planner;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlannerController {

    private final PlannerService planner;
    private final StudySessionService sessions;

    public PlannerController(PlannerService planner, StudySessionService sessions) {
        this.planner = planner;
        this.sessions = sessions;
    }

    public record CreatePlanBody(LocalDate startDate,
                                 @Min(1) @Max(80) int hoursPerWeek,
                                 @Min(0) @Max(99) Integer fromLevel,
                                 @Min(0) @Max(99) Integer toLevel,
                                 boolean skipCompleted) {
    }

    public record LogSessionBody(Long topicId, LocalDate date,
                                 @Min(1) @Max(720) int minutes,
                                 @Size(max = 500) String note) {
    }

    @PostMapping("/api/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlannerService.PlanDto create(@CurrentUser AuthUser me, @Valid @RequestBody CreatePlanBody body) {
        return planner.create(me.id(), new PlannerService.CreatePlanRequest(body.startDate(), body.hoursPerWeek(),
                body.fromLevel() == null ? 0 : body.fromLevel(),
                body.toLevel() == null ? 99 : body.toLevel(),
                body.skipCompleted()));
    }

    @GetMapping("/api/plans/current")
    public PlannerService.PlanDto current(@CurrentUser AuthUser me) {
        return planner.current(me.id());
    }

    @DeleteMapping("/api/plans/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@CurrentUser AuthUser me) {
        planner.archiveCurrent(me.id());
    }

    @PostMapping("/api/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionService.SessionDto log(@CurrentUser AuthUser me, @Valid @RequestBody LogSessionBody body) {
        return sessions.log(me.id(), body.topicId(), body.date(), body.minutes(), body.note());
    }

    @GetMapping("/api/sessions")
    public List<StudySessionService.SessionDto> recent(@CurrentUser AuthUser me) {
        return sessions.recent(me.id());
    }

    @GetMapping("/api/sessions/heatmap")
    public List<StudySessionService.DayDto> heatmap(@CurrentUser AuthUser me,
                                                    @RequestParam(defaultValue = "112") int days) {
        return sessions.heatmap(me.id(), Math.clamp(days, 7, 366));
    }

    @DeleteMapping("/api/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser AuthUser me, @PathVariable Long id) {
        sessions.delete(me.id(), id);
    }
}
