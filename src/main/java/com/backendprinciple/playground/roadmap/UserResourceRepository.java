package com.backendprinciple.playground.roadmap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserResourceRepository extends JpaRepository<UserResource, Long> {

    List<UserResource> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<UserResource> findByIdAndUserId(Long id, UUID userId);

    long countByUserId(UUID userId);
}
