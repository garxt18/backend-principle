package com.backendprinciple.playground.progress;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One learner's status on one topic. Unique on (user_id, topic_id). */
@Entity
@Table(name = "topic_progress")
public class TopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicStatus status = TopicStatus.NOT_STARTED;

    private Short confidence;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TopicProgress() {
    }

    public TopicProgress(UUID userId, Long topicId) {
        this.userId = userId;
        this.topicId = topicId;
    }

    /** Status transitions keep the timestamps consistent - the entity guards its own invariants. */
    public void changeStatus(TopicStatus next, Instant now) {
        if (next == status) {
            return;
        }
        if (next != TopicStatus.NOT_STARTED && startedAt == null) {
            startedAt = now;
        }
        completedAt = next == TopicStatus.DONE ? now : null;
        if (next == TopicStatus.NOT_STARTED) {
            startedAt = null;
        }
        status = next;
    }

    public void setConfidence(Short confidence) { this.confidence = confidence; }
    public void setNotes(String notes) { this.notes = notes; }
    public void touch(Instant now) { this.updatedAt = now; }

    public Long getTopicId() { return topicId; }
    public TopicStatus getStatus() { return status; }
    public Short getConfidence() { return confidence; }
    public String getNotes() { return notes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
