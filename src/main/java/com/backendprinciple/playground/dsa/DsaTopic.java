package com.backendprinciple.playground.dsa;

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

/** One step of the DSA sheet, e.g. "Sliding Window". */
@Entity
@Table(name = "dsa_topics")
public class DsaTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DsaProblem> problems = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<DsaTopicResource> resources = new ArrayList<>();

    protected DsaTopic() {
    }

    public DsaTopic(String slug) {
        this.slug = slug;
    }

    public void update(String title, String summary, int orderIndex) {
        this.title = title;
        this.summary = summary;
        this.orderIndex = orderIndex;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public int getOrderIndex() { return orderIndex; }
    public List<DsaProblem> getProblems() { return problems; }
    public List<DsaTopicResource> getResources() { return resources; }
}
