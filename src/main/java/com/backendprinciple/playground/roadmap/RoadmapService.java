package com.backendprinciple.playground.roadmap;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.roadmap.RoadmapDtos.LevelDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.ResourceDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.TopicDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The roadmap is read on every page load and changes rarely, so it is cached (cache-aside via
 * {@code @Cacheable}). Any admin write evicts the cache so readers never see stale data for long.
 */
@Service
public class RoadmapService {

    public static final String CACHE = "roadmap";

    private final RoadmapLevelRepository levels;
    private final LearningResourceRepository resources;
    private final TopicRepository topics;

    public RoadmapService(RoadmapLevelRepository levels, LearningResourceRepository resources, TopicRepository topics) {
        this.levels = levels;
        this.resources = resources;
        this.topics = topics;
    }

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public List<LevelDto> fullRoadmap() {
        Map<Long, List<ResourceDto>> resourcesByLevel = resources.findAllOrdered().stream()
                .map(ResourceDto::from)
                .collect(Collectors.groupingBy(ResourceDto::levelId));
        return levels.findAllWithTopics().stream()
                .map(l -> new LevelDto(l.getId(), l.getSlug(), l.getLevelNumber(), l.getTitle(), l.getSummary(),
                        l.getWhyItMatters(), l.getProjectTitle(), l.getProjectDescription(), l.getSuggestedMonth(),
                        l.getTopics().stream().mapToInt(Topic::getEstimatedHours).sum(),
                        l.getTopics().stream().map(TopicDto::from).toList(),
                        resourcesByLevel.getOrDefault(l.getId(), List.of())))
                .toList();
    }

    public LevelDto level(String slug) {
        return fullRoadmap().stream().filter(l -> l.slug().equals(slug)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Level"));
    }

    @Transactional(readOnly = true)
    public Topic topic(Long id) {
        return topics.findById(id).orElseThrow(() -> ApiException.notFound("Topic"));
    }

    // ---- admin writes -------------------------------------------------------------------------

    public record ResourceRequest(Long levelId, String title, String url, String channel, ResourceLanguage language,
                                  ResourceKind kind, boolean primaryPick, String note, int orderIndex) {
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ResourceDto createResource(ResourceRequest req) {
        RoadmapLevel level = levels.findById(req.levelId()).orElseThrow(() -> ApiException.notFound("Level"));
        LearningResource r = new LearningResource(level);
        apply(r, req);
        return ResourceDto.from(resources.save(r));
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public ResourceDto updateResource(Long id, ResourceRequest req) {
        LearningResource r = resources.findById(id).orElseThrow(() -> ApiException.notFound("Resource"));
        apply(r, req);
        return ResourceDto.from(r);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public void deleteResource(Long id) {
        if (!resources.existsById(id)) {
            throw ApiException.notFound("Resource");
        }
        resources.deleteById(id);
    }

    private static void apply(LearningResource r, ResourceRequest req) {
        r.update(req.title().strip(), req.url().strip(), req.channel(), req.language(), req.kind(),
                req.primaryPick(), req.note(), req.orderIndex());
    }
}
