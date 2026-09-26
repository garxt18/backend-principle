package com.backendprinciple.playground.lab;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "lab_files")
public class LabFile {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, length = 20)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileLayer layer;

    @Column(name = "build_order", nullable = false)
    private int buildOrder;

    @Column(name = "line_count", nullable = false)
    private int lineCount;

    /** Content can be large; LAZY means listing files does not drag every file body out of the DB. */
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    protected LabFile() {
    }

    public LabFile(UUID projectId, String path, String language, FileLayer layer, int buildOrder, String content) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.path = path;
        this.language = language;
        this.layer = layer;
        this.buildOrder = buildOrder;
        this.content = content;
        this.lineCount = content.isEmpty() ? 0 : (int) content.lines().count();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getPath() { return path; }
    public String getLanguage() { return language; }
    public FileLayer getLayer() { return layer; }
    public int getBuildOrder() { return buildOrder; }
    public int getLineCount() { return lineCount; }
    public String getContent() { return content; }
}
