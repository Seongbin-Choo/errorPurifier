package com.errorpurifier.domain.feedback.repository;

import com.errorpurifier.domain.feedback.entity.RefinementFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefinementFeedbackRepository extends JpaRepository<RefinementFeedback, Long> {
}
