package com.backendprinciple.playground.planner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A logged block of study time: "45 minutes on Spring Data JPA on 2026-10-02". */
@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private int minutes;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StudySession() {
    }

    public StudySession(UUID userId, Long topicId, LocalDate sessionDate, int minutes, String note, Instant createdAt) {
        this.userId = userId;
        this.topicId = topicId;
        this.sessionDate = sessionDate;
        this.minutes = minutes;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getTopicId() { return topicId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public int getMinutes() { return minutes; }
    public String getNote() { return note; }
}
