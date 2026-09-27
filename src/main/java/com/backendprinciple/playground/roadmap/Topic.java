package com.backendprinciple.playground.roadmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LAZY: loading a topic does not automatically load its level (avoids surprise queries). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_id")
    private RoadmapLevel level;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String practice;

    @Column(name = "estimated_hours", nullable = false)
    private int estimatedHours;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /** For playlist levels: which lecture of the level's playlist teaches this topic. */
    @Column(name = "lecture_number")
    private Integer lectureNumber;

    /** Direct link to that lecture's video, when known (otherwise the UI falls back to the playlist). */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    protected Topic() {
    }

    public Topic(RoadmapLevel level, String slug) {
        this.level = level;
        this.slug = slug;
    }

    public void update(String title, String description, String practice, int estimatedHours, int orderIndex,
                       Integer lectureNumber, String videoUrl) {
        this.title = title;
        this.description = description;
        this.practice = practice;
        this.estimatedHours = estimatedHours;
        this.orderIndex = orderIndex;
        this.lectureNumber = lectureNumber;
        this.videoUrl = videoUrl;
    }

    public Long getId() { return id; }
    public RoadmapLevel getLevel() { return level; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPractice() { return practice; }
    public int getEstimatedHours() { return estimatedHours; }
    public int getOrderIndex() { return orderIndex; }
    public Integer getLectureNumber() { return lectureNumber; }
    public String getVideoUrl() { return videoUrl; }
}
