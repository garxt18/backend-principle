package com.backendprinciple.playground.planner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    @Query("select p from StudyPlan p left join fetch p.items where p.userId = :userId and p.status = 'ACTIVE'")
    Optional<StudyPlan> findActiveWithItems(UUID userId);

    Optional<StudyPlan> findByUserIdAndStatus(UUID userId, PlanStatus status);

    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
