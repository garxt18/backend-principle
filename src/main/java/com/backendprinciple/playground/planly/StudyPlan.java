package com.backendprinciple.playground.planly;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "study_plans")
public class StudyPlan {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "hours_per_week", nullable = false)
    private int hoursPerWeek;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pace_mode", nullable = false)
    private PaceMode paceMode = PaceMode.HOURS;

    @Column(name = "hours_per_day", nullable = false, precision = 4, scale = 2)
    private BigDecimal hoursPerDay;

    /** ISO day numbers, e.g. "1,2,3,4,5,6" = Monday to Saturday. */
    @Column(name = "study_days", nullable = false)
    private String studyDays;

    @Column(name = "target_end_date")
    private LocalDate targetEndDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_weeks", nullable = false)
    private int totalWeeks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("plannedDate ASC, orderIndex ASC")
    private List<PlanItem> items = new ArrayList<>();

    protected StudyPlan() {
    }

    public StudyPlan(UUID userId, String name, LocalDate startDate, PaceMode paceMode, Set<DayOfWeek> studyDays,
                     LocalDate targetEndDate, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.startDate = startDate;
        this.paceMode = paceMode;
        this.studyDays = encode(studyDays);
        this.targetEndDate = targetEndDate;
        this.createdAt = createdAt;
    }

    /** Stores the schedule's pace and dates; called after (re)generating the items. */
    public void applySchedule(int minutesPerDay, LocalDate endDate) {
        this.hoursPerDay = BigDecimal.valueOf(minutesPerDay).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        this.hoursPerWeek = Math.clamp(Math.round(minutesPerDay * getStudyDays().size() / 60f), 1, 168);
        this.endDate = endDate;
        this.totalWeeks = (int) (ChronoUnit.DAYS.between(startDate, endDate) / 7) + 1;
    }

    public Set<DayOfWeek> getStudyDays() {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String d : studyDays.split(",")) {
            if (!d.isBlank()) {
                days.add(DayOfWeek.of(Integer.parseInt(d.strip())));
            }
        }
        return days;
    }

    private static String encode(Set<DayOfWeek> days) {
        return days.stream().sorted().map(d -> String.valueOf(d.getValue())).collect(Collectors.joining(","));
    }

    public int minutesPerDay() {
        return hoursPerDay.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public void addItem(PlanItem item) {
        items.add(item);
    }

    public void archive() {
        status = PlanStatus.ARCHIVED;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getStartDate() { return startDate; }
    public int getHoursPerWeek() { return hoursPerWeek; }
    public int getTotalWeeks() { return totalWeeks; }
    public PlanStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PlanItem> getItems() { return items; }
    public String getName() { return name; }
    public PaceMode getPaceMode() { return paceMode; }
    public BigDecimal getHoursPerDay() { return hoursPerDay; }
    public LocalDate getTargetEndDate() { return targetEndDate; }
    public LocalDate getEndDate() { return endDate; }
}
