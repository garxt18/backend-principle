package com.backendprinciple.playground.roadmap;

import com.backendprinciple.playground.common.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-learner layer on top of the shared roadmap: which resource they follow for each level and the
 * links they attached themselves. Kept out of the cached {@link RoadmapService} because it differs
 * per user.
 */
@Service
public class PersonalResourceService {

    /** Generous, but stops one account from filling the table. */
    static final int MAX_LINKS_PER_USER = 500;

    private final UserResourceRepository links;
    private final ResourceChoiceRepository choices;
    private final LearningResourceRepository catalog;
    private final RoadmapLevelRepository levels;
    private final TopicRepository topics;
    private final Clock clock;

    public PersonalResourceService(UserResourceRepository links, ResourceChoiceRepository choices,
                                   LearningResourceRepository catalog, RoadmapLevelRepository levels,
                                   TopicRepository topics, Clock clock) {
        this.links = links;
        this.choices = choices;
        this.catalog = catalog;
        this.levels = levels;
        this.topics = topics;
        this.clock = clock;
    }

    public record LinkDto(Long id, Long levelId, Long topicId, String title, String url, String note,
                          Instant createdAt) {
        static LinkDto from(UserResource r) {
            return new LinkDto(r.getId(), r.getLevelId(), r.getTopicId(), r.getTitle(), r.getUrl(), r.getNote(),
                    r.getCreatedAt());
        }
    }

    public record ChoiceDto(Long levelId, Long resourceId, Long userResourceId, Instant chosenAt) {
        static ChoiceDto from(ResourceChoice c) {
            return new ChoiceDto(c.getLevelId(), c.getResourceId(), c.getUserResourceId(), c.getChosenAt());
        }
    }

    public record MyResources(List<ChoiceDto> choices, List<LinkDto> links) {
    }

    public record NewLink(Long levelId, Long topicId, String title, String url, String note) {
    }

    @Transactional(readOnly = true)
    public MyResources mine(UUID userId) {
        return new MyResources(
                choices.findByUserId(userId).stream().map(ChoiceDto::from).toList(),
                links.findByUserIdOrderByCreatedAtAsc(userId).stream().map(LinkDto::from).toList());
    }

    /**
     * Attach a link to a level, or to one topic (the level is then taken from the topic, so the two
     * can never disagree).
     */
    @Transactional
    public LinkDto addLink(UUID userId, NewLink req) {
        if (links.countByUserId(userId) >= MAX_LINKS_PER_USER) {
            throw ApiException.conflict("You already have " + MAX_LINKS_PER_USER + " links - delete some first");
        }
        Long levelId = req.levelId();
        if (req.topicId() != null) {
            Topic topic = topics.findById(req.topicId()).orElseThrow(() -> ApiException.notFound("Topic"));
            levelId = topic.getLevel().getId();
        } else if (levelId == null || !levels.existsById(levelId)) {
            throw ApiException.notFound("Level");
        }
        UserResource link = new UserResource(userId, levelId, req.topicId(), req.title().strip(), req.url().strip(),
                blankToNull(req.note()), clock.instant());
        return LinkDto.from(links.save(link));
    }

    @Transactional
    public void deleteLink(UUID userId, Long id) {
        UserResource link = links.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Link"));
        links.delete(link); // a choice pointing at it is removed by ON DELETE CASCADE
    }

    /** Pick exactly one of a catalog resource or one of your own links as "the one I follow". */
    @Transactional
    public ChoiceDto follow(UUID userId, Long levelId, Long resourceId, Long userResourceId) {
        if ((resourceId == null) == (userResourceId == null)) {
            throw ApiException.badRequest("Send either resourceId or userResourceId");
        }
        if (resourceId != null) {
            LearningResource r = catalog.findById(resourceId).orElseThrow(() -> ApiException.notFound("Resource"));
            if (!r.getLevel().getId().equals(levelId)) {
                throw ApiException.badRequest("That resource belongs to another level");
            }
        } else {
            UserResource own = links.findByIdAndUserId(userResourceId, userId)
                    .orElseThrow(() -> ApiException.notFound("Link"));
            if (!own.getLevelId().equals(levelId)) {
                throw ApiException.badRequest("That link belongs to another level");
            }
        }
        ResourceChoice choice = choices.findByUserIdAndLevelId(userId, levelId)
                .orElseGet(() -> new ResourceChoice(userId, levelId));
        choice.choose(resourceId, userResourceId, clock.instant());
        return ChoiceDto.from(choices.save(choice));
    }

    @Transactional
    public void unfollow(UUID userId, Long levelId) {
        choices.findByUserIdAndLevelId(userId, levelId).ifPresent(choices::delete);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
