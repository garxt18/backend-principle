package com.backendprinciple.playground.lab;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LabProjectRepository extends JpaRepository<LabProject, UUID> {

    @Query("select p from LabProject p where p.ownerId is null or p.ownerId = :userId order by p.ownerId nulls first, p.createdAt desc")
    List<LabProject> findVisibleTo(UUID userId);

    long countByOwnerId(UUID ownerId);

    @Query("select p from LabProject p where p.ownerId is null and p.slug = :slug")
    Optional<LabProject> findTemplate(String slug);
}
