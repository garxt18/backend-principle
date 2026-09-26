package com.backendprinciple.playground.roadmap;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query("select t from Topic t join fetch t.level l order by l.levelNumber, t.orderIndex")
    List<Topic> findAllInRoadmapOrder();
}
