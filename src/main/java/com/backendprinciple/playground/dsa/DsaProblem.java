package com.backendprinciple.playground.dsa;

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

@Entity
@Table(name = "dsa_problems")
public class DsaProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private DsaTopic topic;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "practice_url", nullable = false)
    private String practiceUrl;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected DsaProblem() {
    }

    public DsaProblem(DsaTopic topic, String slug) {
        this.topic = topic;
        this.slug = slug;
    }

    public void update(DsaTopic topic, String title, Difficulty difficulty, String practiceUrl, int orderIndex) {
        this.topic = topic;
        this.title = title;
        this.difficulty = difficulty;
        this.practiceUrl = practiceUrl;
        this.orderIndex = orderIndex;
    }

    public Long getId() { return id; }
    public DsaTopic getTopic() { return topic; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getPracticeUrl() { return practiceUrl; }
    public int getOrderIndex() { return orderIndex; }
}
