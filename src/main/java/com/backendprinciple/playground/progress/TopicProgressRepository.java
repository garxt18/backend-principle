package com.backendprinciple.playground.progress;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {

    List<TopicProgress> findByUserId(UUID userId);

    Optional<TopicProgress> findByUserIdAndTopicId(UUID userId, Long topicId);

    @Query("select p.topicId from TopicProgress p where p.userId = :userId and p.status = 'DONE'")
    Set<Long> findDoneTopicIds(UUID userId);
}
