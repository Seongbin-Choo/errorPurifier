package com.errorpurifier.domain.knowledge.service;

import com.errorpurifier.domain.knowledge.repository.DiagnosticPlaybookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiagnosticPlaybookMatcher {
    private final DiagnosticPlaybookRepository playbookRepository;

    public List<DiagnosticPlaybookMatch> findMatches(String refinedLog) {
        return playbookRepository.findByIsActiveTrueOrderByPriorityDesc().stream()
                .filter(playbook -> matches(playbook.getMatchPattern(), refinedLog))
                .map(playbook -> new DiagnosticPlaybookMatch(playbook.getId(), playbook.getName(), playbook.getGuidance()))
                .toList();
    }

    public void recordMatches(List<DiagnosticPlaybookMatch> matches) {
        if (!matches.isEmpty()) {
            playbookRepository.increaseMatchCountByIds(matches.stream().map(DiagnosticPlaybookMatch::id).toList());
        }
    }

    private boolean matches(String pattern, String refinedLog) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(refinedLog).find();
        } catch (PatternSyntaxException exception) {
            log.warn("잘못된 진단 플레이북 정규식: {}", pattern);
            return false;
        }
    }

    public record DiagnosticPlaybookMatch(Long id, String name, String guidance) {
    }
}
