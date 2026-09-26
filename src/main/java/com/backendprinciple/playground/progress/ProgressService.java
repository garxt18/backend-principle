package com.backendprinciple.playground.progress;

import com.backendprinciple.playground.planner.StudySessionRepository;
import com.backendprinciple.playground.roadmap.RoadmapDtos.LevelDto;
import com.backendprinciple.playground.roadmap.RoadmapDtos.TopicDto;
import com.backendprinciple.playground.roadmap.RoadmapService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {

    private final TopicProgressRepository progress;
    private final StudySessionRepository sessions;
    private final RoadmapService roadmap;
    private final Clock clock;

    public ProgressService(TopicProgressRepository progress, StudySessionRepository sessions,
                           RoadmapService roadmap, Clock clock) {
        this.progress = progress;
        this.sessions = sessions;
        this.roadmap = roadmap;
        this.clock = clock;
    }

    public record TopicProgressDto(Long topicId, TopicStatus status, Short confidence, String notes,
                                   Instant startedAt, Instant completedAt, Instant updatedAt) {
        static TopicProgressDto from(TopicProgress p) {
            return new TopicProgressDto(p.getTopicId(), p.getStatus(), p.getConfidence(), p.getNotes(),
                    p.getStartedAt(), p.getCompletedAt(), p.getUpdatedAt());
        }
    }

    public record LevelProgress(Long levelId, int levelNumber, String title, int topicsDone, int topicsTotal,
                                int hoursDone, int hoursTotal, int percent) {
    }

    public record Summary(int percent, int topicsDone, int topicsTotal, int hoursDone, int hoursTotal,
                          int currentStreakDays, int minutesLast7Days, List<LevelProgress> levels,
                          List<TopicProgressDto> topics) {
    }

    public record UpdateRequest(TopicStatus status, Short confidence, String notes) {
    }

    @Transactional
    public TopicProgressDto update(UUID userId, Long topicId, UpdateRequest req) {
        roadmap.topic(topicId); // 404 if the topic does not exist
        Instant now = clock.instant();
        TopicProgress p = progress.findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> new TopicProgress(userId, topicId));
        if (req.status() != null) {
            p.changeStatus(req.status(), now);
        }
        if (req.confidence() != null) {
            p.setConfidence(req.confidence());
        }
        if (req.notes() != null) {
            p.setNotes(req.notes().isBlank() ? null : req.notes());
        }
        p.touch(now);
        return TopicProgressDto.from(progress.save(p));
    }

    @Transactional(readOnly = true)
    public Summary summary(UUID userId) {
        Map<Long, TopicProgress> byTopic = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(TopicProgress::getTopicId, Function.identity()));
        Set<Long> done = byTopic.values().stream()
                .filter(p -> p.getStatus() == TopicStatus.DONE)
                .map(TopicProgress::getTopicId)
                .collect(Collectors.toSet());

        List<LevelProgress> levels = roadmap.fullRoadmap().stream().map(l -> levelProgress(l, done)).toList();
        int topicsDone = levels.stream().mapToInt(LevelProgress::topicsDone).sum();
        int topicsTotal = levels.stream().mapToInt(LevelProgress::topicsTotal).sum();
        int hoursDone = levels.stream().mapToInt(LevelProgress::hoursDone).sum();
        int hoursTotal = levels.stream().mapToInt(LevelProgress::hoursTotal).sum();

        LocalDate today = LocalDate.now(clock);
        int streak = StreakCalculator.currentStreak(sessions.findStudyDatesSince(userId, today.minusDays(400)), today);
        int minutes = sessions.sumMinutesSince(userId, today.minusDays(6));

        return new Summary(percent(hoursDone, hoursTotal), topicsDone, topicsTotal, hoursDone, hoursTotal,
                streak, minutes, levels, byTopic.values().stream().map(TopicProgressDto::from).toList());
    }

    private static LevelProgress levelProgress(LevelDto l, Set<Long> done) {
        int topicsDone = 0;
        int hoursDone = 0;
        for (TopicDto t : l.topics()) {
            if (done.contains(t.id())) {
                topicsDone++;
                hoursDone += t.estimatedHours();
            }
        }
        return new LevelProgress(l.id(), l.levelNumber(), l.title(), topicsDone, l.topics().size(),
                hoursDone, l.totalHours(), percent(hoursDone, l.totalHours()));
    }

    static int percent(int part, int whole) {
        return whole == 0 ? 0 : Math.round(part * 100f / whole);
    }
}
