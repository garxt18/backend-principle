package com.backendprinciple.playground.roadmap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *       without losing anyone's progress (progress rows point at stable topic ids).</li>
 *   <li>Resources are inserted only for levels that have none yet, so edits made by an admin through
 *       the API are never overwritten by a restart.</li>
 * </ul>
 */
@Component
@Order(1)
public class RoadmapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoadmapSeeder.class);

    record SeedFile(int version, List<SeedLevel> levels) {
    }

    record SeedLevel(String slug, int number, String title, String summary, String whyItMatters, String projectTitle,
                     String projectDescription, Integer month, List<SeedTopic> topics, List<SeedResource> resources) {
    }

    record SeedTopic(String slug, String title, int hours, String description, String practice) {
    }

    record SeedResource(String title, String url, String channel, ResourceLanguage language, ResourceKind kind,
                        boolean primary, String note) {
    }

    private final RoadmapLevelRepository levels;
    private final LearningResourceRepository resources;
    private final ObjectMapper objectMapper;

    public RoadmapSeeder(RoadmapLevelRepository levels, LearningResourceRepository resources, ObjectMapper objectMapper) {
        this.levels = levels;
        this.resources = resources;
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

            Map<String, Topic> existing = new HashMap<>();
            level.getTopics().forEach(t -> existing.put(t.getSlug(), t));
            for (int i = 0; i < sl.topics().size(); i++) {
                SeedTopic st = sl.topics().get(i);
                Topic topic = existing.get(st.slug());
                if (topic == null) {
                    topic = new Topic(level, st.slug());
                    level.getTopics().add(topic);
                }
                topic.update(st.title(), st.description(), st.practice(), st.hours(), i);
                topicCount++;
            }
            levels.save(level);

            if (!resources.existsByLevelId(level.getId())) {
                for (int i = 0; i < sl.resources().size(); i++) {
                    SeedResource sr = sl.resources().get(i);
                    LearningResource r = new LearningResource(level);
                    r.update(sr.title(), sr.url(), sr.channel(), sr.language(), sr.kind(), sr.primary(), sr.note(), i);
                    level.getResources().add(r);
                }
            }
        }
        log.info("Roadmap seeded: {} levels, {} topics (seed version {})", seed.levels().size(), topicCount,
                seed.version());
    }
}
