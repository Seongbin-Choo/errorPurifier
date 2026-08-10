package com.errorpurifier.domain.rule.repository;

import com.errorpurifier.domain.rule.entity.LogParsingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogParsingRuleRepository extends JpaRepository<LogParsingRule, Long> {
    List<LogParsingRule> findByIsActiveTrueOrderByPriorityDesc();
    List<LogParsingRule> findAllByOrderByPriorityDescIdAsc();
    boolean existsByDescription(String description);
    boolean existsByDescriptionAndIdNot(String description, Long id);
}
