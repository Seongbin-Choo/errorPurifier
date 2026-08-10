package com.errorpurifier.domain.history.service;

import com.errorpurifier.domain.history.dto.RequestHistoryResponse;
import com.errorpurifier.domain.history.repository.RequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestHistoryService {
    private final RequestHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public Page<RequestHistoryResponse> findAll(Pageable pageable) {
        return historyRepository.findAllByOrderByCreatedAtDescIdDesc(pageable).map(RequestHistoryResponse::from);
    }
}
