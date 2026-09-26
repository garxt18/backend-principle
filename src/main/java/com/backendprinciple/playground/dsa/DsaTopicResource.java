package com.backendprinciple.playground.dsa;

import com.backendprinciple.playground.roadmap.ResourceLanguage;
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
@Table(name = "dsa_topic_resources")
public class DsaTopicResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private DsaTopic topic;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceLanguage language;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected DsaTopicResource() {
    }

    public DsaTopicResource(DsaTopic topic, String title, String url, String channel, ResourceLanguage language,
                            int orderIndex) {
        this.topic = topic;
        this.title = title;
        this.url = url;
        this.channel = channel;
        this.language = language;
        this.orderIndex = orderIndex;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getChannel() { return channel; }
    public ResourceLanguage getLanguage() { return language; }
}
