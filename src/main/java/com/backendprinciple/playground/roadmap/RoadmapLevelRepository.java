package com.backendprinciple.playground.roadmap;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoadmapLevelRepository extends JpaRepository<RoadmapLevel, Long> {

    Optional<RoadmapLevel> findBySlug(String slug);

    /** One query for levels + topics; resources are fetched in a second query to avoid a cartesian product. */
    @Query("select distinct l from RoadmapLevel l left join fetch l.topics order by l.levelNumber")
    List<RoadmapLevel> findAllWithTopics();
}
