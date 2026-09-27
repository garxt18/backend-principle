package com.backendprinciple.playground.roadmap;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roadmap_levels")
public class RoadmapLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "level_number", nullable = false, unique = true)
    private int levelNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "why_it_matters", columnDefinition = "text")
    private String whyItMatters;

    @Column(name = "project_title")
    private String projectTitle;

    @Column(name = "project_description", columnDefinition = "text")
    private String projectDescription;

    @Column(name = "suggested_month")
    private Integer suggestedMonth;

    /** Set when the whole level is taught by one playlist (Java and Spring Boot levels). */
    @Column(name = "playlist_name")
    private String playlistName;

    @Column(name = "playlist_channel")
    private String playlistChannel;

    @Column(name = "playlist_url", length = 500)
    private String playlistUrl;

    /** mappedBy = the field on Topic that owns the foreign key. Order is enforced in SQL, not in Java. */
    @OneToMany(mappedBy = "level", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Topic> topics = new ArrayList<>();

    @OneToMany(mappedBy = "level", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<LearningResource> resources = new ArrayList<>();

    protected RoadmapLevel() {
    }

    public RoadmapLevel(String slug) {
        this.slug = slug;
    }

    public void update(int levelNumber, String title, String summary, String whyItMatters,
                       String projectTitle, String projectDescription, Integer suggestedMonth) {
        this.levelNumber = levelNumber;
        this.title = title;
        this.summary = summary;
        this.whyItMatters = whyItMatters;
        this.projectTitle = projectTitle;
        this.projectDescription = projectDescription;
        this.suggestedMonth = suggestedMonth;
    }

    public void updatePlaylist(String name, String channel, String url) {
        this.playlistName = name;
        this.playlistChannel = channel;
        this.playlistUrl = url;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public int getLevelNumber() { return levelNumber; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getWhyItMatters() { return whyItMatters; }
    public String getProjectTitle() { return projectTitle; }
    public String getProjectDescription() { return projectDescription; }
    public Integer getSuggestedMonth() { return suggestedMonth; }
    public String getPlaylistName() { return playlistName; }
    public String getPlaylistChannel() { return playlistChannel; }
    public String getPlaylistUrl() { return playlistUrl; }
    public List<Topic> getTopics() { return topics; }
    public List<LearningResource> getResources() { return resources; }
}
