package com.backendprinciple.playground.roadmap;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    @Query("select r from LearningResource r join fetch r.level order by r.level.levelNumber, r.orderIndex")
    List<LearningResource> findAllOrdered();

    boolean existsByLevelId(Long levelId);
}
