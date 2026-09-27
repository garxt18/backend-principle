package com.backendprinciple.playground.planly;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
public class PlanlyController {

    private final PlanlyService planner;
    private final StudySessionService sessions;

    public PlanlyController(PlanlyService planner, StudySessionService sessions) {
        this.planner = planner;
        this.sessions = sessions;
    }

    /**
     * Either {@code pace=HOURS} with {@code hoursPerDay}, or {@code pace=DEADLINE} with {@code targetEndDate}.
     * Scope: {@code topicIds}, else {@code levelNumbers}, else {@code fromLevel..toLevel} (default: everything).
     * {@code hoursPerWeek} is still accepted from older clients and converted to hours per day.
     */
    public record CreatePlanBody(@Size(max = 120) String name,
                                 LocalDate startDate,
                                 PaceMode pace,
                                 @DecimalMin("0.25") @DecimalMax("24") BigDecimal hoursPerDay,
                                 LocalDate targetEndDate,
                                 @Size(max = 7) Set<DayOfWeek> studyDays,
                                 @Size(max = 20) List<@Min(0) @Max(99) Integer> levelNumbers,
                                 @Size(max = 500) List<Long> topicIds,
                                 @Min(0) @Max(99) Integer fromLevel,
                                 @Min(0) @Max(99) Integer toLevel,
                                 @Min(1) @Max(168) Integer hoursPerWeek,
                                 boolean skipCompleted) {

        PlanlyService.CreatePlanRequest toRequest() {
            Set<DayOfWeek> days = studyDays == null || studyDays.isEmpty() ? PlanlyService.DEFAULT_DAYS : studyDays;
            BigDecimal perDay = hoursPerDay;
            if (perDay == null && hoursPerWeek != null) {
                perDay = BigDecimal.valueOf(hoursPerWeek).divide(BigDecimal.valueOf(days.size()), 2, RoundingMode.HALF_UP);
            }
            return new PlanlyService.CreatePlanRequest(name, startDate, pace, perDay, targetEndDate, days, levelNumbers,
                    topicIds, fromLevel == null ? 0 : fromLevel, toLevel == null ? 99 : toLevel, skipCompleted);
        }
    }

    public record ReplanBody(boolean keepDeadline) {
    }

    public record LogSessionBody(Long topicId, LocalDate date,
                                 @Min(1) @Max(720) int minutes,
                                 @Size(max = 500) String note) {
    }

    @PostMapping("/api/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanlyService.PlanDto create(@CurrentUser AuthUser me, @Valid @RequestBody CreatePlanBody body) {
        return planner.create(me.id(), body.toRequest());
    }

    /** Same body as create: shows topics, hours per day and finish date without saving anything. */
    @PostMapping("/api/plans/preview")
    public PlanlyService.PreviewDto preview(@CurrentUser AuthUser me, @Valid @RequestBody CreatePlanBody body) {
        return planner.preview(me.id(), body.toRequest());
    }

    /** Reschedule everything not done yet from today (same pace, or same finish date). */
    @PostMapping("/api/plans/current/replan")
    public PlanlyService.PlanDto replan(@CurrentUser AuthUser me, @RequestBody(required = false) ReplanBody body) {
        return planner.replanRemaining(me.id(), body != null && body.keepDeadline());
    }

    @GetMapping("/api/plans/current")
    public PlanlyService.PlanDto current(@CurrentUser AuthUser me) {
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
