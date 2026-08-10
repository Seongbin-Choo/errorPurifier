package com.errorpurifier.domain.knowledge.service;

import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookRequest;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookResponse;
import com.errorpurifier.domain.knowledge.entity.DiagnosticPlaybook;
import com.errorpurifier.domain.knowledge.repository.DiagnosticPlaybookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@RequiredArgsConstructor
public class DiagnosticPlaybookService {
    private final DiagnosticPlaybookRepository playbookRepository;

    @Transactional(readOnly = true)
    public List<DiagnosticPlaybookResponse> findAll() {
        return playbookRepository.findAllByOrderByPriorityDescIdAsc().stream().map(DiagnosticPlaybookResponse::from).toList();
    }

    @Transactional
    public DiagnosticPlaybookResponse create(DiagnosticPlaybookRequest request) {
        validate(request, null);
        DiagnosticPlaybook playbook = playbookRepository.save(DiagnosticPlaybook.builder()
                .name(request.name()).matchPattern(request.matchPattern()).guidance(request.guidance()).priority(request.priority()).build());
        return DiagnosticPlaybookResponse.from(playbook);
    }

    @Transactional
    public DiagnosticPlaybookResponse update(Long playbookId, DiagnosticPlaybookRequest request) {
        DiagnosticPlaybook playbook = get(playbookId);
        validate(request, playbookId);
        playbook.update(request.name(), request.matchPattern(), request.guidance(), request.priority());
        return DiagnosticPlaybookResponse.from(playbook);
    }

    @Transactional
    public void setActive(Long playbookId, boolean active) {
        get(playbookId).setActive(active);
    }

    @Transactional(readOnly = true)
    public boolean matches(String matchPattern, String log) {
        validatePattern(matchPattern);
        return Pattern.compile(matchPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(log).find();
    }

    private DiagnosticPlaybook get(Long playbookId) {
        return playbookRepository.findById(playbookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "진단 플레이북을 찾을 수 없습니다."));
    }

    private void validate(DiagnosticPlaybookRequest request, Long playbookId) {
        validatePattern(request.matchPattern());
        boolean duplicate = playbookId == null ? playbookRepository.existsByName(request.name())
                : playbookRepository.existsByNameAndIdNot(request.name(), playbookId);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 이름의 진단 플레이북이 이미 존재합니다.");
        }
    }

    private void validatePattern(String matchPattern) {
        try {
            Pattern.compile(matchPattern);
        } catch (PatternSyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "매칭 정규식 형식이 올바르지 않습니다.");
        }
    }
}
