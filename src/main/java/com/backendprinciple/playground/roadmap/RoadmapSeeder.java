package com.backendprinciple.playground.roadmap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads roadmap/roadmap.json into the database at startup.
 * <ul>
 *   <li>Levels and topics are upserted by slug, so editing the JSON and restarting updates the text
 *       without losing anyone's progress (progress rows point at stable topic ids). Topics that were
 *       removed from the JSON are deleted (their progress rows go with them).</li>
 *   <li>Seeded resources are upserted by URL and removed when they leave the JSON. Resources an admin
 *       added through the API ({@code seeded = false}) are never touched.</li>
 * </ul>
 */
@Component
@Order(1)
public class RoadmapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoadmapSeeder.class);

    record SeedFile(int version, List<SeedLevel> levels) {
    }

    record SeedLevel(String slug, int number, String title, String summary, String whyItMatters, String projectTitle,
                     String projectDescription, Integer month, SeedPlaylist playlist, List<SeedTopic> topics,
                     List<SeedResource> resources) {
    }

    record SeedPlaylist(String name, String channel, String url, Integer fromLecture, Integer toLecture) {
    }

    record SeedTopic(String slug, String title, int hours, String description, String practice, Integer lecture,
                     String videoUrl) {
    }

    record SeedResource(String title, String url, String channel, ResourceLanguage language, ResourceKind kind,
                        boolean primary, String note) {
    }

    private final RoadmapLevelRepository levels;
    private final ObjectMapper objectMapper;

    public RoadmapSeeder(RoadmapLevelRepository levels, ObjectMapper objectMapper) {
        this.levels = levels;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RoadmapService.CACHE, allEntries = true)
    public void run(ApplicationArguments args) throws IOException {
        SeedFile seed;
        try (InputStream in = new ClassPathResource("roadmap/roadmap.json").getInputStream()) {
            seed = objectMapper.readValue(in, SeedFile.class);
        }
        int topicCount = 0;
        for (SeedLevel sl : seed.levels()) {
            RoadmapLevel level = levels.findBySlug(sl.slug()).orElseGet(() -> new RoadmapLevel(sl.slug()));
            level.update(sl.number(), sl.title(), sl.summary(), sl.whyItMatters(), sl.projectTitle(),
                    sl.projectDescription(), sl.month());
            SeedPlaylist pl = sl.playlist();
            level.updatePlaylist(pl == null ? null : pl.name(), pl == null ? null : pl.channel(),
                    pl == null ? null : pl.url());

            syncTopics(level, sl.topics());
            topicCount += sl.topics().size();
            syncResources(level, sl.resources());
            levels.save(level);
        }
        log.info("Roadmap seeded: {} levels, {} topics (seed version {})", seed.levels().size(), topicCount,
                seed.version());
    }

    private static void syncTopics(RoadmapLevel level, List<SeedTopic> seedTopics) {
        Map<String, Topic> existing = new HashMap<>();
        level.getTopics().forEach(t -> existing.put(t.getSlug(), t));
        Set<String> wanted = new HashSet<>();
        for (int i = 0; i < seedTopics.size(); i++) {
            SeedTopic st = seedTopics.get(i);
            wanted.add(st.slug());
            Topic topic = existing.get(st.slug());
            if (topic == null) {
                topic = new Topic(level, st.slug());
                level.getTopics().add(topic);
            }
            topic.update(st.title(), st.description(), st.practice(), st.hours(), i, st.lecture(), st.videoUrl());
        }
        level.getTopics().removeIf(t -> !wanted.contains(t.getSlug()));
    }

    private static void syncResources(RoadmapLevel level, List<SeedResource> seedResources) {
        Map<String, LearningResource> seededByUrl = new HashMap<>();
        level.getResources().stream().filter(LearningResource::isSeeded).forEach(r -> seededByUrl.put(r.getUrl(), r));
        Set<String> wanted = new HashSet<>();
        for (int i = 0; i < seedResources.size(); i++) {
            SeedResource sr = seedResources.get(i);
            wanted.add(sr.url());
            LearningResource r = seededByUrl.get(sr.url());
            if (r == null) {
                r = new LearningResource(level, true);
                level.getResources().add(r);
            }
            r.update(sr.title(), sr.url(), sr.channel(), sr.language(), sr.kind(), sr.primary(), sr.note(), i);
        }
        level.getResources().removeIf(r -> r.isSeeded() && !wanted.contains(r.getUrl()));
    }
}
