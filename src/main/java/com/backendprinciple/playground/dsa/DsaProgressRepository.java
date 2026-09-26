package com.backendprinciple.playground.dsa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DsaProgressRepository extends JpaRepository<DsaProgress, Long> {

    List<DsaProgress> findByUserId(UUID userId);

    Optional<DsaProgress> findByUserIdAndProblemId(UUID userId, Long problemId);

    @Query("select p.solvedAt from DsaProgress p where p.userId = :userId and p.solved = true and p.solvedAt >= :from")
    List<Instant> findSolvedTimesSince(UUID userId, Instant from);
}
