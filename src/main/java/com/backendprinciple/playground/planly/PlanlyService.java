package com.backendprinciple.playground.planly;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.dsa.DsaService;
import com.backendprinciple.playground.planly.PlanGenerator.Allocation;
import com.backendprinciple.playground.planly.PlanGenerator.TopicSlot;
import com.backendprinciple.playground.progress.TopicProgressRepository;
import com.backendprinciple.playground.roadmap.RoadmapDtos.LevelDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.TopicDto;
import com.backendprinciple.playground.roadmap.RoadmapService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Planly: the study planner - roadmap hours per week plus a weekly DSA target. */
@Service
public class PlanlyService {

    private final StudyPlanRepository plans;
    private final StudySessionRepository sessions;
    private final TopicProgressRepository progress;
    private final RoadmapService roadmap;
    private final DsaService dsa;
    private final Clock clock;

    public PlanlyService(StudyPlanRepository plans, StudySessionRepository sessions, TopicProgressRepository progress,
                         RoadmapService roadmap, DsaService dsa, Clock clock) {
        this.plans = plans;
        this.dsa = dsa;
        this.sessions = sessions;
        this.progress = progress;
        this.roadmap = roadmap;
        this.clock = clock;
    }

    public record CreatePlanRequest(LocalDate startDate, int hoursPerWeek, int dsaPerWeek, int fromLevel, int toLevel,
                                    boolean skipCompleted) {
    }

    public record PlanItemDto(Long topicId, String topicTitle, int levelNumber, String levelTitle,
                              BigDecimal plannedHours, boolean done) {
    }

    public record WeekDto(int week, LocalDate startDate, LocalDate endDate, BigDecimal plannedHours,
                          long minutesLogged, int dsaTarget, long dsaSolved, List<PlanItemDto> items) {
    }

    public record PlanDto(UUID id, LocalDate startDate, LocalDate endDate, int hoursPerWeek, int dsaPerWeek,
                          int totalWeeks, int currentWeek, int percentDone, int scheduleDeltaHours, List<WeekDto> weeks) {
    }

    /** Creates a new active plan; the previous active plan (if any) is archived, not deleted. */
    @Transactional
    public PlanDto create(UUID userId, CreatePlanRequest req) {
        if (req.fromLevel() > req.toLevel()) {
            throw ApiException.badRequest("fromLevel must be <= toLevel");
        }
        Set<Long> done = req.skipCompleted() ? progress.findDoneTopicIds(userId) : Set.of();
        List<TopicSlot> slots = new ArrayList<>();
        for (LevelDto level : roadmap.fullRoadmap()) {
            if (level.levelNumber() < req.fromLevel() || level.levelNumber() > req.toLevel()) {
                continue;
            }
            for (TopicDto t : level.topics()) {
                if (!done.contains(t.id())) {
                    slots.add(new TopicSlot(t.id(), t.estimatedHours()));
                }
            }
        }
        if (slots.isEmpty()) {
            throw ApiException.badRequest("Nothing left to plan in that range - everything is done!");
        }

        plans.findByUserIdAndStatus(userId, PlanStatus.ACTIVE).ifPresent(old -> {
            old.archive();
            plans.saveAndFlush(old); // flush first so the "one active plan" unique index is satisfied
        });

        LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now(clock);
        StudyPlan plan = new StudyPlan(userId, start, req.hoursPerWeek(), req.dsaPerWeek(), clock.instant());
        PlanGenerator.Schedule schedule = PlanGenerator.generate(slots, req.hoursPerWeek());
        for (Allocation a : schedule.allocations()) {
            plan.addItem(new PlanItem(plan, a.topicId(), a.week(), a.orderInWeek(), BigDecimal.valueOf(a.hours())));
        }
        plan.setTotalWeeks(schedule.totalWeeks());
        plans.save(plan);
        return toDto(plan, userId);
    }

    @Transactional(readOnly = true)
    public PlanDto current(UUID userId) {
        StudyPlan plan = plans.findActiveWithItems(userId).orElseThrow(() -> ApiException.notFound("Active plan"));
        return toDto(plan, userId);
    }

    @Transactional
    public void archiveCurrent(UUID userId) {
        StudyPlan plan = plans.findByUserIdAndStatus(userId, PlanStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("Active plan"));
        plan.archive();
    }

    private PlanDto toDto(StudyPlan plan, UUID userId) {
        Map<Long, TopicDto> topicById = new HashMap<>();
        Map<Long, LevelDto> levelByTopic = new HashMap<>();
        for (LevelDto l : roadmap.fullRoadmap()) {
            for (TopicDto t : l.topics()) {
                topicById.put(t.id(), t);
                levelByTopic.put(t.id(), l);
            }
        }
        Set<Long> done = progress.findDoneTopicIds(userId);
        Map<LocalDate, Long> minutesByDay = new HashMap<>();
        sessions.dailyMinutesSince(userId, plan.getStartDate())
                .forEach(d -> minutesByDay.put(d.getDay(), d.getMinutes()));
        Map<LocalDate, Long> dsaByDay = dsa.solvedPerDay(userId, plan.getStartDate());

        LocalDate today = LocalDate.now(clock);
        int currentWeek = (int) Math.clamp(ChronoUnit.DAYS.between(plan.getStartDate(), today) / 7 + 1,
                0, Math.max(plan.getTotalWeeks(), 1));

        Map<Integer, List<PlanItem>> byWeek = new TreeMap<>();
        plan.getItems().forEach(i -> byWeek.computeIfAbsent(i.getWeekNumber(), w -> new ArrayList<>()).add(i));

        List<WeekDto> weeks = new ArrayList<>();
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal doneHours = BigDecimal.ZERO;
        BigDecimal expectedByNow = BigDecimal.ZERO;
        for (var entry : byWeek.entrySet()) {
            int week = entry.getKey();
            LocalDate weekStart = plan.getStartDate().plusWeeks(week - 1L);
            LocalDate weekEnd = weekStart.plusDays(6);
            BigDecimal planned = BigDecimal.ZERO;
            List<PlanItemDto> items = new ArrayList<>();
            for (PlanItem i : entry.getValue()) {
                TopicDto t = topicById.get(i.getTopicId());
                LevelDto l = levelByTopic.get(i.getTopicId());
                if (t == null) {
                    continue; // topic removed from the roadmap since the plan was made
                }
                boolean isDone = done.contains(i.getTopicId());
                items.add(new PlanItemDto(t.id(), t.title(), l.levelNumber(), l.title(), i.getPlannedHours(), isDone));
                planned = planned.add(i.getPlannedHours());
                if (isDone) {
                    doneHours = doneHours.add(i.getPlannedHours());
                }
            }
            totalHours = totalHours.add(planned);
            if (week < currentWeek) {
                expectedByNow = expectedByNow.add(planned);
            }
            long minutes = weekStart.datesUntil(weekEnd.plusDays(1)).mapToLong(d -> minutesByDay.getOrDefault(d, 0L)).sum();
            long dsaSolved = weekStart.datesUntil(weekEnd.plusDays(1)).mapToLong(d -> dsaByDay.getOrDefault(d, 0L)).sum();
            weeks.add(new WeekDto(week, weekStart, weekEnd, planned, minutes, plan.getDsaPerWeek(), dsaSolved, items));
        }
        int percent = totalHours.signum() == 0 ? 0
                : doneHours.multiply(BigDecimal.valueOf(100)).divide(totalHours, 0, java.math.RoundingMode.HALF_UP).intValue();
        LocalDate end = plan.getStartDate().plusWeeks(plan.getTotalWeeks()).minusDays(1);
        return new PlanDto(plan.getId(), plan.getStartDate(), end, plan.getHoursPerWeek(), plan.getDsaPerWeek(),
                plan.getTotalWeeks(),
                currentWeek, percent, doneHours.subtract(expectedByNow).intValue(), weeks);
    }
}
