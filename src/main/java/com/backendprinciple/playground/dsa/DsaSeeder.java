package com.backendprinciple.playground.dsa;

import com.backendprinciple.playground.roadmap.ResourceLanguage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
 * Loads dsa/dsa-sheet.json at startup. Topics and problems are upserted by slug, so editing the JSON
 * never loses anyone's progress; topic resources are replaced on every start.
 */
@Component
@Order(3)
public class DsaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DsaSeeder.class);

    record SeedFile(int version, List<SeedTopic> topics) {
    }

    record SeedTopic(String slug, String title, String summary, List<SeedResource> resources, List<SeedProblem> problems) {
    }

    record SeedResource(String title, String url, String channel, ResourceLanguage language) {
    }

    record SeedProblem(String slug, String title, Difficulty difficulty, String url) {
    }

    private final DsaTopicRepository topics;
    private final DsaProblemRepository problems;
    private final ObjectMapper objectMapper;

    public DsaSeeder(DsaTopicRepository topics, DsaProblemRepository problems, ObjectMapper objectMapper) {
        this.topics = topics;
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DsaService.CACHE, allEntries = true)
    public void run(ApplicationArguments args) throws IOException {
        SeedFile seed;
        try (InputStream in = new ClassPathResource("dsa/dsa-sheet.json").getInputStream()) {
            seed = objectMapper.readValue(in, SeedFile.class);
        }
        int problemCount = 0;
        for (int ti = 0; ti < seed.topics().size(); ti++) {
            SeedTopic st = seed.topics().get(ti);
            DsaTopic topic = topics.findBySlug(st.slug()).orElseGet(() -> new DsaTopic(st.slug()));
            topic.update(st.title(), st.summary(), ti);
            topic = topics.save(topic);

            topic.getResources().clear();
            for (int ri = 0; ri < st.resources().size(); ri++) {
                SeedResource r = st.resources().get(ri);
                topic.getResources().add(new DsaTopicResource(topic, r.title(), r.url(), r.channel(), r.language(), ri));
            }
            for (int pi = 0; pi < st.problems().size(); pi++) {
                SeedProblem sp = st.problems().get(pi);
                DsaTopic owner = topic;
                DsaProblem problem = problems.findBySlug(sp.slug()).orElseGet(() -> new DsaProblem(owner, sp.slug()));
                problem.update(topic, sp.title(), sp.difficulty(), sp.url(), pi);
                problems.save(problem);
                problemCount++;
            }
        }
        log.info("DSA sheet seeded: {} topics, {} problems (seed version {})", seed.topics().size(), problemCount,
                seed.version());
    }
}
