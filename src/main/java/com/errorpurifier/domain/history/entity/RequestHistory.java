package com.errorpurifier.domain.history.entity;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.client.entity.ClientDevice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ClientDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cache_id")
    private ErrorCache cache; // Cache Miss일 경우 null 허용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestType requestType;

    @Column(nullable = false)
    private int processingTimeMs;

    @CreatedDate
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    public enum RequestType {
        CACHE_HIT, LLM_CALL
    }

    @Builder
    public RequestHistory(ClientDevice device, ErrorCache cache, RequestType requestType, int processingTimeMs) {
        this.device = device;
        this.cache = cache;
        this.requestType = requestType;
        this.processingTimeMs = processingTimeMs;
    }
}