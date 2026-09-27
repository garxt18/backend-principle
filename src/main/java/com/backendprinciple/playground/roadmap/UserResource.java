package com.backendprinciple.playground.roadmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A link a learner attached themselves: a playlist they prefer for a level, or a better video for
 * one lecture/topic. Private to that learner. Plain ids instead of @ManyToOne keep it a simple row.
 */
@Entity
@Table(name = "user_resources")
public class UserResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    /** null = attached to the whole level. */
    @Column(name = "topic_id")
    private Long topicId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserResource() {
    }

    public UserResource(UUID userId, Long levelId, Long topicId, String title, String url, String note,
                        Instant createdAt) {
        this.userId = userId;
        this.levelId = levelId;
        this.topicId = topicId;
        this.title = title;
        this.url = url;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getLevelId() { return levelId; }
    public Long getTopicId() { return topicId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
