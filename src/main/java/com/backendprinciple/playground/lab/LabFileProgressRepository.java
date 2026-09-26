package com.backendprinciple.playground.lab;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LabFileProgressRepository extends JpaRepository<LabFileProgress, Long> {

    Optional<LabFileProgress> findByUserIdAndFileId(UUID userId, UUID fileId);

    @Query("select p from LabFileProgress p where p.userId = :userId and p.fileId in "
            + "(select f.id from LabFile f where f.projectId = :projectId)")
    List<LabFileProgress> findForProject(UUID userId, UUID projectId);

    @Query("select coalesce(sum(p.linesCompleted), 0) from LabFileProgress p where p.userId = :userId")
    long totalLinesTyped(UUID userId);
}
