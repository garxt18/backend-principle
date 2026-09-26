package com.backendprinciple.playground.dsa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DsaTopicRepository extends JpaRepository<DsaTopic, Long> {

    Optional<DsaTopic> findBySlug(String slug);

    @Query("select distinct t from DsaTopic t left join fetch t.problems order by t.orderIndex")
    List<DsaTopic> findAllWithProblems();
}
