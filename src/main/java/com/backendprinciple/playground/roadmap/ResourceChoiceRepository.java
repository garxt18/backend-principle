package com.backendprinciple.playground.roadmap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceChoiceRepository extends JpaRepository<ResourceChoice, Long> {

    List<ResourceChoice> findByUserId(UUID userId);

    Optional<ResourceChoice> findByUserIdAndLevelId(UUID userId, Long levelId);
}
