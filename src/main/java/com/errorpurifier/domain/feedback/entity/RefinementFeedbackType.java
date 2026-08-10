package com.errorpurifier.domain.feedback.entity;

/** Evaluation of log purification, independent from whether the LLM answer was useful. */
public enum RefinementFeedbackType {
    APPROPRIATE,
    MISSING_CONTEXT,
    TOO_NOISY
}
