package com.backendprinciple.playground.roadmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A video, playlist, channel or doc recommended for one roadmap level. */
@Entity
@Table(name = "resources")
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_id")
    private RoadmapLevel level;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceKind kind;

    @Column(name = "primary_pick", nullable = false)
    private boolean primaryPick;

    private String note;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected LearningResource() {
    }

    public LearningResource(RoadmapLevel level) {
        this.level = level;
    }

    public void update(String title, String url, String channel, ResourceLanguage language, ResourceKind kind,
                       boolean primaryPick, String note, int orderIndex) {
        this.title = title;
        this.url = url;
        this.channel = channel;
        this.language = language;
        this.kind = kind;
        this.primaryPick = primaryPick;
        this.note = note;
        this.orderIndex = orderIndex;
    }

    public Long getId() { return id; }
    public RoadmapLevel getLevel() { return level; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getChannel() { return channel; }
    public ResourceLanguage getLanguage() { return language; }
    public ResourceKind getKind() { return kind; }
    public boolean isPrimaryPick() { return primaryPick; }
    public String getNote() { return note; }
    public int getOrderIndex() { return orderIndex; }
}
