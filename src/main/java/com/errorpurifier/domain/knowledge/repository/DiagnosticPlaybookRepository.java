package com.errorpurifier.domain.knowledge.repository;

import com.errorpurifier.domain.knowledge.entity.DiagnosticPlaybook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiagnosticPlaybookRepository extends JpaRepository<DiagnosticPlaybook, Long> {
    List<DiagnosticPlaybook> findByIsActiveTrueOrderByPriorityDesc();
    List<DiagnosticPlaybook> findAllByOrderByPriorityDescIdAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Modifying
    @Query("update DiagnosticPlaybook playbook set playbook.matchCount = playbook.matchCount + 1 where playbook.id in :ids")
    void increaseMatchCountByIds(@Param("ids") List<Long> ids);
}
