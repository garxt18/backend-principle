package com.backendprinciple.playground.lab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A learner's own explanation of one line. Writing "why this line exists" in your own words is the
 * fastest way to find out whether you really understood it.
 */
@Entity
@Table(name = "lab_line_notes")
public class LabLineNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LabLineNote() {
    }

    public LabLineNote(UUID userId, UUID fileId, int lineNumber) {
        this.userId = userId;
        this.fileId = fileId;
        this.lineNumber = lineNumber;
    }

    public void write(String note, Instant now) {
        this.note = note;
        this.updatedAt = now;
    }

    public int getLineNumber() { return lineNumber; }
    public String getNote() { return note; }
}
