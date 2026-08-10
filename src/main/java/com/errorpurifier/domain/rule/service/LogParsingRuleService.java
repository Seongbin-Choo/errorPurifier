package com.errorpurifier.domain.rule.service;

import com.errorpurifier.domain.rule.dto.LogParsingRuleRequest;
import com.errorpurifier.domain.rule.dto.LogParsingRuleResponse;
import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogParsingRuleService {
    private final LogParsingRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<LogParsingRuleResponse> findAll() {
        return ruleRepository.findAllByOrderByPriorityDescIdAsc().stream()
                .map(LogParsingRuleResponse::from)
                .toList();
    }

    @Transactional
    public LogParsingRuleResponse create(LogParsingRuleRequest request) {
        validateUniqueDescription(request.description(), null);
        LogParsingRule rule = ruleRepository.save(LogParsingRule.builder()
                .ruleType(request.ruleType())
                .targetFramework(request.targetFramework())
                .regexPattern(request.regexPattern())
                .priority(request.priority())
                .description(request.description())
                .minPluginVersion(request.minPluginVersion())
                .build());
        return LogParsingRuleResponse.from(rule);
    }

    @Transactional
    public LogParsingRuleResponse update(Long ruleId, LogParsingRuleRequest request) {
        LogParsingRule rule = getRule(ruleId);
        validateUniqueDescription(request.description(), ruleId);
        rule.update(request.ruleType(), request.targetFramework(), request.regexPattern(), request.priority(),
                request.description(), request.minPluginVersion());
        return LogParsingRuleResponse.from(rule);
    }

    @Transactional
    public void setActive(Long ruleId, boolean active) {
        LogParsingRule rule = getRule(ruleId);
        if (active) {
            rule.activate();
        } else {
            rule.deactivate();
        }
    }

    private LogParsingRule getRule(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파싱 룰을 찾을 수 없습니다."));
    }

    private void validateUniqueDescription(String description, Long ruleId) {
        boolean exists = ruleId == null ? ruleRepository.existsByDescription(description)
                : ruleRepository.existsByDescriptionAndIdNot(description, ruleId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 설명의 파싱 룰이 이미 존재합니다.");
        }
    }
}
