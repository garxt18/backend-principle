package com.backendprinciple.playground.roadmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * "This is the resource I'm going to follow" for one level. Exactly one of {@code resourceId}
 * (a catalog resource) or {@code userResourceId} (one of the learner's own links) is set - the
 * database enforces that with a CHECK constraint.
 */
@Entity
@Table(name = "resource_choices")
public class ResourceChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "user_resource_id")
    private Long userResourceId;

    @Column(name = "chosen_at", nullable = false)
    private Instant chosenAt;

    protected ResourceChoice() {
    }

    public ResourceChoice(UUID userId, Long levelId) {
        this.userId = userId;
        this.levelId = levelId;
    }

    public void choose(Long resourceId, Long userResourceId, Instant at) {
        this.resourceId = resourceId;
        this.userResourceId = userResourceId;
        this.chosenAt = at;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getLevelId() { return levelId; }
    public Long getResourceId() { return resourceId; }
    public Long getUserResourceId() { return userResourceId; }
    public Instant getChosenAt() { return chosenAt; }
}
