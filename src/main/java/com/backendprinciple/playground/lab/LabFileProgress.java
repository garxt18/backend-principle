package com.backendprinciple.playground.lab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_file_progress")
public class LabFileProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "lines_completed", nullable = false)
    private int linesCompleted;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LabFileProgress() {
    }

    public LabFileProgress(UUID userId, UUID fileId) {
        this.userId = userId;
        this.fileId = fileId;
    }

    public void record(int linesCompleted, int lineCount, Instant now) {
        this.linesCompleted = Math.clamp(linesCompleted, 0, lineCount);
        this.completed = this.linesCompleted >= lineCount;
        this.updatedAt = now;
    }

    public UUID getFileId() { return fileId; }
    public int getLinesCompleted() { return linesCompleted; }
    public boolean isCompleted() { return completed; }
    public Instant getUpdatedAt() { return updatedAt; }
}
