package com.backendprinciple.playground.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** "Spend N hours on topic X in week W". A big topic can be split over several weeks. */
@Entity
@Table(name = "plan_items")
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private StudyPlan plan;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "planned_hours", nullable = false, precision = 5, scale = 1)
    private BigDecimal plannedHours;

    protected PlanItem() {
    }

    public PlanItem(StudyPlan plan, Long topicId, int weekNumber, int orderIndex, BigDecimal plannedHours) {
        this.plan = plan;
        this.topicId = topicId;
        this.weekNumber = weekNumber;
        this.orderIndex = orderIndex;
        this.plannedHours = plannedHours;
    }

    public Long getTopicId() { return topicId; }
    public int getWeekNumber() { return weekNumber; }
    public int getOrderIndex() { return orderIndex; }
    public BigDecimal getPlannedHours() { return plannedHours; }
}
