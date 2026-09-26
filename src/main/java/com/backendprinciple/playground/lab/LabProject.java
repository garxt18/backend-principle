package com.backendprinciple.playground.lab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A codebase to rebuild. ownerId == null means a shared template everybody can see. */
@Entity
@Table(name = "lab_projects")
public class LabProject {

    public enum SourceKind { TEMPLATE, UPLOAD }

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false)
    private SourceKind sourceKind;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LabProject() {
    }

    public LabProject(UUID ownerId, String slug, String name, String description, SourceKind sourceKind,
                      Instant createdAt) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.sourceKind = sourceKind;
        this.createdAt = createdAt;
    }

    public boolean isTemplate() {
        return ownerId == null;
    }

    /** Templates are readable by everyone; uploads only by their owner. */
    public boolean isVisibleTo(UUID userId) {
        return isTemplate() || ownerId.equals(userId);
    }

    public void setStats(int fileCount, int totalLines) {
        this.fileCount = fileCount;
        this.totalLines = totalLines;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SourceKind getSourceKind() { return sourceKind; }
    public int getFileCount() { return fileCount; }
    public int getTotalLines() { return totalLines; }
    public Instant getCreatedAt() { return createdAt; }
}
