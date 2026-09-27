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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "total_weeks", nullable = false)
    private int totalWeeks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC, orderIndex ASC")
    private List<PlanItem> items = new ArrayList<>();

    protected StudyPlan() {
    }

    public StudyPlan(UUID userId, LocalDate startDate, int hoursPerWeek, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.startDate = startDate;
        this.hoursPerWeek = hoursPerWeek;
        this.createdAt = createdAt;
    }

    public void addItem(PlanItem item) {
        items.add(item);
    }

    public void archive() {
        status = PlanStatus.ARCHIVED;
    }

    public void setTotalWeeks(int totalWeeks) {
        this.totalWeeks = totalWeeks;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getStartDate() { return startDate; }
    public int getHoursPerWeek() { return hoursPerWeek; }
    public int getTotalWeeks() { return totalWeeks; }
    public PlanStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PlanItem> getItems() { return items; }
}
