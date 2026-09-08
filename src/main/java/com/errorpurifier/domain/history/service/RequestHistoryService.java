package com.errorpurifier.domain.history.service;

import com.errorpurifier.domain.history.dto.RequestHistoryCursor;
import com.errorpurifier.domain.history.dto.RequestHistoryResponse;
import com.errorpurifier.domain.history.entity.RequestHistory;
import com.errorpurifier.domain.history.repository.RequestHistoryRepository;
import com.errorpurifier.global.common.CursorSlice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestHistoryService {
    private static final int MAX_SIZE = 100;

    private final RequestHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public CursorSlice<RequestHistoryResponse> findSlice(String cursor, int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
        List<RequestHistory> fetched = fetch(cursor, Limit.of(size + 1));
        boolean hasNext = fetched.size() > size;
        List<RequestHistory> page = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext ? RequestHistoryCursor.from(page.get(page.size() - 1)).encode() : null;
        return CursorSlice.of(page.stream().map(RequestHistoryResponse::from).toList(), nextCursor);
    }

    private List<RequestHistory> fetch(String cursor, Limit limit) {
        if (cursor == null || cursor.isBlank()) {
            return historyRepository.findAllByOrderByCreatedAtDescIdDesc(limit);
        }
        RequestHistoryCursor decoded = RequestHistoryCursor.decode(cursor);
        return historyRepository.findSliceBefore(decoded.createdAt(), decoded.id(), limit);
    }
}
