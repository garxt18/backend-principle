package com.backendprinciple.playground.lab;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LabFileRepository extends JpaRepository<LabFile, UUID> {

    /** Lightweight listing without file bodies (a DTO projection straight from JPQL). */
    record FileSummary(UUID id, String path, String language, FileLayer layer, int buildOrder, int lineCount) {
    }

    @Query("""
            select new com.backendprinciple.playground.lab.LabFileRepository$FileSummary(
                f.id, f.path, f.language, f.layer, f.buildOrder, f.lineCount)
            from LabFile f where f.projectId = :projectId order by f.buildOrder""")
    List<FileSummary> findSummaries(UUID projectId);
}
