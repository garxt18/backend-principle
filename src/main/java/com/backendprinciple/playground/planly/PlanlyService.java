package com.backendprinciple.playground.planly;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.planly.PlanGenerator.Allocation;
import com.backendprinciple.playground.planly.PlanGenerator.Schedule;
import com.backendprinciple.playground.planly.PlanGenerator.TopicSlot;
import com.backendprinciple.playground.progress.TopicProgressRepository;
import com.backendprinciple.playground.roadmap.RoadmapDtos.LevelDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.TopicDto;
import com.backendprinciple.playground.roadmap.RoadmapService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planly: turns roadmap topics into a day-by-day schedule.
 * <ul>
 *   <li><b>Pace</b> mode: "2 hours a day, Monday to Saturday" - the end date follows.</li>
 *   <li><b>Deadline</b> mode: "Java Basics in 7 days" - the hours per day follow.</li>
 * </ul>
 * The plan never stores "done": it reads topic progress, so ticking a topic in the Roadmap, the dashboard
 * or here is the same action and everything stays in sync.
 */
@Service
public class PlanlyService {

    static final Set<DayOfWeek> DEFAULT_DAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);

    private final StudyPlanRepository plans;
    private final StudySessionRepository sessions;
    private final TopicProgressRepository progress;
    private final RoadmapService roadmap;
    private final Clock clock;

    public PlanlyService(StudyPlanRepository plans, StudySessionRepository sessions, TopicProgressRepository progress,
                         RoadmapService roadmap, Clock clock) {
        this.plans = plans;
        this.sessions = sessions;
        this.progress = progress;
        this.roadmap = roadmap;
        this.clock = clock;
    }

    /**
     * @param levelNumbers levels to include (empty = all, or the fromLevel..toLevel range)
     * @param topicIds     explicit topics (wins over levels), always scheduled in roadmap order
     */
    public record CreatePlanRequest(String name, LocalDate startDate, PaceMode pace, BigDecimal hoursPerDay,
                                    LocalDate targetEndDate, Set<DayOfWeek> studyDays, List<Integer> levelNumbers,
                                    List<Long> topicIds, int fromLevel, int toLevel, boolean skipCompleted) {
    }

    public record PlanItemDto(Long topicId, String topicTitle, int levelNumber, String levelTitle, Integer lectureNumber,
                              BigDecimal plannedHours, LocalDate plannedDate, boolean done) {
    }

    public record DayDto(LocalDate date, boolean studyDay, BigDecimal plannedHours, long minutesLogged,
                         List<PlanItemDto> items) {
    }

    public record WeekDto(int week, LocalDate startDate, LocalDate endDate, BigDecimal plannedHours,
                          long minutesLogged, List<DayDto> days, List<PlanItemDto> items) {
    }

    /**
     * @param scheduleDeltaHours positive = ahead of plan, negative = behind (planned hours of past days not done)
     * @param projectedEndDate   when you will finish at the planned pace, starting from today with what is left
     */
    public record PlanDto(UUID id, String name, PaceMode paceMode, LocalDate startDate, LocalDate endDate,
                          LocalDate targetEndDate, BigDecimal hoursPerDay, int hoursPerWeek, List<DayOfWeek> studyDays,
                          int totalWeeks, int currentWeek, int percentDone, BigDecimal totalHours, BigDecimal doneHours,
                          int scheduleDeltaHours, int topicsTotal, int topicsDone, LocalDate projectedEndDate,
                          DayDto today, List<PlanItemDto> overdue, List<WeekDto> weeks) {
    }

    /** What a plan would look like, before saving it. */
    public record PreviewDto(int topics, BigDecimal totalHours, BigDecimal hoursPerDay, int studyDays,
                             LocalDate startDate, LocalDate endDate, int weeks, String warning) {
    }

    // ------------------------------------------------------------------------------------ create / preview

    @Transactional(readOnly = true)
    public PreviewDto preview(UUID userId, CreatePlanRequest req) {
        Draft d = draft(userId, req);
        int total = d.slots().stream().mapToInt(TopicSlot::minutes).sum();
        Schedule s = d.schedule();
        return new PreviewDto(d.slots().size(), hours(total), hours(s.minutesPerDay()),
                PlanGenerator.countStudyDays(d.start(), s.endDate(), d.days()), d.start(), s.endDate(),
                (int) (ChronoUnit.DAYS.between(d.start(), s.endDate()) / 7) + 1, warning(s.minutesPerDay()));
    }

    /** Creates a new active plan; the previous active plan (if any) is archived, not deleted. */
    @Transactional
    public PlanDto create(UUID userId, CreatePlanRequest req) {
        Draft d = draft(userId, req);
        if (d.schedule().minutesPerDay() > PlanGenerator.MAX_MINUTES_PER_DAY) {
            throw ApiException.badRequest("That needs " + hours(d.schedule().minutesPerDay())
                    + " hours per study day - pick a later date, more study days or fewer topics");
        }
        plans.findByUserIdAndStatus(userId, PlanStatus.ACTIVE).ifPresent(old -> {
            old.archive();
            plans.saveAndFlush(old); // flush first so the "one active plan" unique index is satisfied
        });
        StudyPlan plan = new StudyPlan(userId, blankToNull(req.name()), d.start(), d.pace(), d.days(),
                d.pace() == PaceMode.DEADLINE ? req.targetEndDate() : null, clock.instant());
        addItems(plan, d.schedule());
        plans.save(plan);
        return toDto(plan, userId);
    }

    private record Draft(LocalDate start, PaceMode pace, Set<DayOfWeek> days, List<TopicSlot> slots, Schedule schedule) {
    }

    private Draft draft(UUID userId, CreatePlanRequest req) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = req.startDate() != null ? req.startDate() : today;
        Set<DayOfWeek> days = req.studyDays() == null || req.studyDays().isEmpty() ? DEFAULT_DAYS : EnumSet.copyOf(req.studyDays());
        PaceMode pace = req.pace() == null ? PaceMode.HOURS : req.pace();
        List<TopicSlot> slots = topicsFor(userId, req);
        if (slots.isEmpty()) {
            throw ApiException.badRequest("Nothing left to plan in that selection - everything is done!");
        }
        Schedule schedule;
        if (pace == PaceMode.DEADLINE) {
            if (req.targetEndDate() == null || req.targetEndDate().isBefore(start)) {
                throw ApiException.badRequest("Choose a finish date on or after the start date");
            }
            if (PlanGenerator.countStudyDays(start, req.targetEndDate(), days) == 0) {
                throw ApiException.badRequest("There is no study day between the start and the finish date");
            }
            schedule = PlanGenerator.fitDeadline(slots, days, start, req.targetEndDate());
        } else {
            if (req.hoursPerDay() == null || req.hoursPerDay().signum() <= 0) {
                throw ApiException.badRequest("Tell Planly how many hours per day you can study");
            }
            int minutes = PlanGenerator.roundUp(req.hoursPerDay().multiply(BigDecimal.valueOf(60)).intValue());
            schedule = PlanGenerator.generate(slots, Math.max(15, minutes), days, start);
        }
        return new Draft(start, pace, days, slots, schedule);
    }

    /** The selected topics in roadmap order, as (topic, minutes) slots. */
    private List<TopicSlot> topicsFor(UUID userId, CreatePlanRequest req) {
        Set<Long> done = req.skipCompleted() ? progress.findDoneTopicIds(userId) : Set.of();
        Set<Long> chosenTopics = req.topicIds() == null ? Set.of() : new LinkedHashSet<>(req.topicIds());
        Set<Integer> chosenLevels = req.levelNumbers() == null ? Set.of() : new LinkedHashSet<>(req.levelNumbers());
        List<TopicSlot> slots = new ArrayList<>();
        for (LevelDto level : roadmap.fullRoadmap()) {
            boolean levelIn = !chosenLevels.isEmpty() ? chosenLevels.contains(level.levelNumber())
                    : level.levelNumber() >= req.fromLevel() && level.levelNumber() <= req.toLevel();
            for (TopicDto t : level.topics()) {
                boolean in = chosenTopics.isEmpty() ? levelIn : chosenTopics.contains(t.id());
                if (in && !done.contains(t.id())) {
                    slots.add(new TopicSlot(t.id(), t.estimatedHours() * 60));
                }
            }
        }
        return slots;
    }

    private static void addItems(StudyPlan plan, Schedule schedule) {
        for (Allocation a : schedule.allocations()) {
            int week = (int) (ChronoUnit.DAYS.between(plan.getStartDate(), a.date()) / 7) + 1;
            plan.addItem(new PlanItem(plan, a.topicId(), a.date(), week, a.orderInDay(), hours(a.minutes())));
        }
        plan.applySchedule(schedule.minutesPerDay(), schedule.endDate());
    }

    // ------------------------------------------------------------------------------------ re-plan

    /**
     * Life happened: reschedule everything not done yet, starting today. Finished topics keep their days.
     *
     * @param keepDeadline true = same finish date, more hours per day; false = same hours per day, later finish
     */
    @Transactional
    public PlanDto replanRemaining(UUID userId, boolean keepDeadline) {
        StudyPlan plan = plans.findActiveWithItems(userId).orElseThrow(() -> ApiException.notFound("Active plan"));
        LocalDate today = LocalDate.now(clock);
        Set<Long> done = progress.findDoneTopicIds(userId);
        Map<Long, TopicDto> topics = topicIndex();

        List<TopicSlot> remaining = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (PlanItem i : plan.getItems()) {
            TopicDto t = topics.get(i.getTopicId());
            if (t != null && !done.contains(t.id()) && seen.add(t.id())) {
                remaining.add(new TopicSlot(t.id(), t.estimatedHours() * 60));
            }
        }
        if (remaining.isEmpty()) {
            throw ApiException.badRequest("Everything in this plan is done - create a new one!");
        }
        plan.getItems().removeIf(i -> !done.contains(i.getTopicId())); // finished work stays where it was done
        LocalDate from = today.isBefore(plan.getStartDate()) ? plan.getStartDate() : today;
        Schedule schedule = keepDeadline && plan.getTargetEndDate() != null && !plan.getTargetEndDate().isBefore(from)
                ? PlanGenerator.fitDeadline(remaining, plan.getStudyDays(), from, plan.getTargetEndDate())
                : PlanGenerator.generate(remaining, plan.minutesPerDay(), plan.getStudyDays(), from);
        if (schedule.minutesPerDay() > PlanGenerator.MAX_MINUTES_PER_DAY) {
            throw ApiException.badRequest("Keeping the deadline would need " + hours(schedule.minutesPerDay())
                    + " hours per day - re-plan at your normal pace instead");
        }
        addItems(plan, schedule);
        return toDto(plan, userId);
    }

    // ------------------------------------------------------------------------------------ read

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

    private Map<Long, TopicDto> topicIndex() {
        Map<Long, TopicDto> byId = new HashMap<>();
        roadmap.fullRoadmap().forEach(l -> l.topics().forEach(t -> byId.put(t.id(), t)));
        return byId;
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
        LocalDate today = LocalDate.now(clock);
        Set<DayOfWeek> studyDays = plan.getStudyDays();

        Map<LocalDate, List<PlanItemDto>> byDay = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal doneHours = BigDecimal.ZERO;
        BigDecimal expected = BigDecimal.ZERO;
        int remainingMinutes = 0;
        Set<Long> planTopics = new LinkedHashSet<>();
        Map<Long, PlanItemDto> overdue = new LinkedHashMap<>();
        for (PlanItem i : plan.getItems()) {
            TopicDto t = topicById.get(i.getTopicId());
            if (t == null) {
                continue; // topic removed from the roadmap since the plan was made
            }
            LevelDto l = levelByTopic.get(t.id());
            boolean isDone = done.contains(t.id());
            PlanItemDto dto = new PlanItemDto(t.id(), t.title(), l.levelNumber(), l.title(), t.lectureNumber(),
                    i.getPlannedHours(), i.getPlannedDate(), isDone);
            byDay.computeIfAbsent(i.getPlannedDate(), d -> new ArrayList<>()).add(dto);
            planTopics.add(t.id());
            total = total.add(i.getPlannedHours());
            if (isDone) {
                doneHours = doneHours.add(i.getPlannedHours());
            } else {
                remainingMinutes += i.getPlannedHours().multiply(BigDecimal.valueOf(60)).intValue();
            }
            if (i.getPlannedDate().isBefore(today)) {
                expected = expected.add(i.getPlannedHours());
                if (!isDone) {
                    overdue.putIfAbsent(t.id(), dto);
                }
            }
        }

        // Weeks of 7 days from the start date, each with every calendar day (rest days included).
        LocalDate end = plan.getEndDate().isBefore(plan.getStartDate()) ? plan.getStartDate() : plan.getEndDate();
        List<WeekDto> weeks = new ArrayList<>();
        int week = 1;
        for (LocalDate ws = plan.getStartDate(); !ws.isAfter(end); ws = ws.plusDays(7), week++) {
            List<DayDto> days = new ArrayList<>();
            List<PlanItemDto> weekItems = new ArrayList<>();
            BigDecimal planned = BigDecimal.ZERO;
            long minutes = 0;
            for (LocalDate d = ws; d.isBefore(ws.plusDays(7)); d = d.plusDays(1)) {
                DayDto day = day(d, byDay, minutesByDay, studyDays);
                days.add(day);
                weekItems.addAll(day.items());
                planned = planned.add(day.plannedHours());
                minutes += day.minutesLogged();
            }
            weeks.add(new WeekDto(week, ws, ws.plusDays(6), planned, minutes, days, weekItems));
        }

        int currentWeek = (int) Math.clamp(ChronoUnit.DAYS.between(plan.getStartDate(), today) / 7 + 1,
                0, Math.max(weeks.size(), 1));
        int percent = total.signum() == 0 ? 0
                : doneHours.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
        LocalDate projected = remainingMinutes == 0 ? null
                : PlanGenerator.finishDate(today.isBefore(plan.getStartDate()) ? plan.getStartDate() : today,
                remainingMinutes, plan.minutesPerDay(), studyDays);
        int topicsDone = (int) planTopics.stream().filter(done::contains).count();
        return new PlanDto(plan.getId(), plan.getName(), plan.getPaceMode(), plan.getStartDate(), plan.getEndDate(),
                plan.getTargetEndDate(), plan.getHoursPerDay(), plan.getHoursPerWeek(), studyDays.stream().sorted().toList(),
                plan.getTotalWeeks(), currentWeek, percent, total, doneHours,
                doneHours.subtract(expected).setScale(0, RoundingMode.HALF_UP).intValue(),
                planTopics.size(), topicsDone, projected, day(today, byDay, minutesByDay, studyDays),
                List.copyOf(overdue.values()), weeks);
    }

    private static DayDto day(LocalDate d, Map<LocalDate, List<PlanItemDto>> byDay, Map<LocalDate, Long> minutesByDay,
                              Set<DayOfWeek> studyDays) {
        List<PlanItemDto> items = byDay.getOrDefault(d, List.of());
        BigDecimal planned = items.stream().map(PlanItemDto::plannedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DayDto(d, studyDays.contains(d.getDayOfWeek()), planned, minutesByDay.getOrDefault(d, 0L), items);
    }

    static BigDecimal hours(int minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static String warning(int minutesPerDay) {
        if (minutesPerDay > PlanGenerator.MAX_MINUTES_PER_DAY) {
            return "More than 16 hours a day is not possible - choose a later date, more study days or fewer topics.";
        }
        if (minutesPerDay > 8 * 60) {
            return "That is more than a full working day of study - fine for a sprint, hard to keep up for weeks.";
        }
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
