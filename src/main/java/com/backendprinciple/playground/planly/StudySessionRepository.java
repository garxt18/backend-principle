package com.backendprinciple.playground.planly;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    /** Interface-based projection: Spring Data maps the two selected columns onto these getters. */
    interface DailyMinutes {
        LocalDate getDay();

        long getMinutes();
    }

    List<StudySession> findTop50ByUserIdOrderBySessionDateDescIdDesc(UUID userId);

    Optional<StudySession> findByIdAndUserId(Long id, UUID userId);

    @Query("select distinct s.sessionDate from StudySession s where s.userId = :userId and s.sessionDate >= :from")
    List<LocalDate> findStudyDatesSince(UUID userId, LocalDate from);

    @Query("select coalesce(sum(s.minutes), 0) from StudySession s where s.userId = :userId and s.sessionDate >= :from")
    int sumMinutesSince(UUID userId, LocalDate from);

    @Query("""
            select s.sessionDate as day, sum(s.minutes) as minutes from StudySession s
            where s.userId = :userId and s.sessionDate >= :from
            group by s.sessionDate order by s.sessionDate""")
    List<DailyMinutes> dailyMinutesSince(UUID userId, LocalDate from);

    @Query("select count(distinct s.userId) from StudySession s where s.sessionDate >= :from")
    long countDistinctUsersSince(LocalDate from);
}
