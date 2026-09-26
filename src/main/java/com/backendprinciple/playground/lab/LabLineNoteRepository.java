package com.backendprinciple.playground.lab;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabLineNoteRepository extends JpaRepository<LabLineNote, Long> {

    List<LabLineNote> findByUserIdAndFileIdOrderByLineNumber(UUID userId, UUID fileId);

    Optional<LabLineNote> findByUserIdAndFileIdAndLineNumber(UUID userId, UUID fileId, int lineNumber);
}
