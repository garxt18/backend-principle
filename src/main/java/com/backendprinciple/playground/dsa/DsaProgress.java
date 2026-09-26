package com.backendprinciple.playground.dsa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One learner's state on one problem: solved, marked for revision, personal notes. */
@Entity
@Table(name = "dsa_progress")
public class DsaProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(nullable = false)
    private boolean solved;

    @Column(nullable = false)
    private boolean revision;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DsaProgress() {
    }

    public DsaProgress(UUID userId, Long problemId) {
        this.userId = userId;
        this.problemId = problemId;
    }

    /** solvedAt is kept in sync with solved so "problems solved this week" can be counted. */
    public void markSolved(boolean value, Instant now) {
        if (value && !solved) {
            solvedAt = now;
        } else if (!value) {
            solvedAt = null;
        }
        solved = value;
    }

    public void setRevision(boolean revision) { this.revision = revision; }
    public void setNotes(String notes) { this.notes = notes; }
    public void touch(Instant now) { this.updatedAt = now; }

    public Long getProblemId() { return problemId; }
    public boolean isSolved() { return solved; }
    public boolean isRevision() { return revision; }
    public String getNotes() { return notes; }
    public Instant getSolvedAt() { return solvedAt; }
}
