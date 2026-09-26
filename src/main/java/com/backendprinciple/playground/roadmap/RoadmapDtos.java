package com.backendprinciple.playground.roadmap;

import java.util.List;

/** Read models for the roadmap API. Records are immutable and serialize cleanly to JSON. */
public final class RoadmapDtos {

    private RoadmapDtos() {
    }

    public record TopicDto(Long id, String slug, String title, String description, String practice,
                           int estimatedHours, int orderIndex) {
        static TopicDto from(Topic t) {
            return new TopicDto(t.getId(), t.getSlug(), t.getTitle(), t.getDescription(), t.getPractice(),
                    t.getEstimatedHours(), t.getOrderIndex());
        }
    }

    public record ResourceDto(Long id, Long levelId, String title, String url, String channel,
                              ResourceLanguage language, ResourceKind kind, boolean primaryPick, String note,
                              int orderIndex) {
        static ResourceDto from(LearningResource r) {
            return new ResourceDto(r.getId(), r.getLevel().getId(), r.getTitle(), r.getUrl(), r.getChannel(),
                    r.getLanguage(), r.getKind(), r.isPrimaryPick(), r.getNote(), r.getOrderIndex());
        }
    }

    public record LevelDto(Long id, String slug, int levelNumber, String title, String summary, String whyItMatters,
                           String projectTitle, String projectDescription, Integer suggestedMonth,
                           int totalHours, List<TopicDto> topics, List<ResourceDto> resources) {
    }
}
