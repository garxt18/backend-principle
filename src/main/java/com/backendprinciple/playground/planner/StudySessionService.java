package com.backendprinciple.playground.planner;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.roadmap.RoadmapService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudySessionService {

    private final StudySessionRepository sessions;
    private final RoadmapService roadmap;
    private final Clock clock;

    public StudySessionService(StudySessionRepository sessions, RoadmapService roadmap, Clock clock) {
        this.sessions = sessions;
        this.roadmap = roadmap;
        this.clock = clock;
    }

    public record SessionDto(Long id, Long topicId, LocalDate date, int minutes, String note) {
        static SessionDto from(StudySession s) {
            return new SessionDto(s.getId(), s.getTopicId(), s.getSessionDate(), s.getMinutes(), s.getNote());
        }
    }

    public record DayDto(LocalDate date, long minutes) {
    }

    @Transactional
    public SessionDto log(UUID userId, Long topicId, LocalDate date, int minutes, String note) {
        LocalDate today = LocalDate.now(clock);
        LocalDate day = date != null ? date : today;
        if (day.isAfter(today.plusDays(1))) { // +1 day tolerance for users ahead of UTC
            throw ApiException.badRequest("You cannot log study time in the future");
        }
        if (topicId != null) {
            roadmap.topic(topicId);
        }
        return SessionDto.from(sessions.save(new StudySession(userId, topicId, day, minutes, note, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<SessionDto> recent(UUID userId) {
        return sessions.findTop50ByUserIdOrderBySessionDateDescIdDesc(userId).stream().map(SessionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DayDto> heatmap(UUID userId, int days) {
        LocalDate from = LocalDate.now(clock).minusDays(days - 1L);
        return sessions.dailyMinutesSince(userId, from).stream().map(d -> new DayDto(d.getDay(), d.getMinutes())).toList();
    }

    @Transactional
    public void delete(UUID userId, Long id) {
        // Ownership is part of the query: another user's id simply looks like "not found".
        StudySession s = sessions.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Session"));
        sessions.delete(s);
    }
}
