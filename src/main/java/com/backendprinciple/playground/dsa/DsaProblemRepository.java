package com.backendprinciple.playground.dsa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DsaProblemRepository extends JpaRepository<DsaProblem, Long> {

    Optional<DsaProblem> findBySlug(String slug);
}
