package com.errorpurifier.domain.client.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_device", indexes = {
        @Index(name = "idx_last_access_at", columnList = "last_access_at") // 정리(Cleanup) 배치용 인덱스
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientDevice {

    // 1. PK와 UUID 통합, BINARY(16) 최적화 (Hibernate 6+ 지원)
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 2. 파편화된 boolean 플래그를 하나의 상태(Enum)로 통합
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;

    // 3. 날짜 기반 카운트 리셋용 컬럼 추가
    @Column(nullable = false)
    private LocalDate quotaDate;

    @Column(nullable = false)
    private int dailyRequestCount;

    @Column(nullable = false, length = 20)
    private String pluginVersion;

    // 4. 어뷰징 대응용 Audit 컬럼
    @Column(length = 500)
    private String blockedReason;
    private LocalDateTime blockedAt;

    private LocalDateTime lastAccessAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public ClientDevice(UUID id, String pluginVersion) {
        this.id = id;
        this.status = DeviceStatus.ACTIVE;
        this.quotaDate = LocalDate.now();
        this.dailyRequestCount = 0;
        this.pluginVersion = pluginVersion;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void recordAccess() {
        LocalDate today = LocalDate.now();
        if (!today.equals(this.quotaDate)) {
            this.dailyRequestCount = 0;
            this.quotaDate = today;
        }
        this.dailyRequestCount++;
        this.lastAccessAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void recordAccess(String currentPluginVersion) {
        this.recordAccess(); // 위의 로직 그대로 재사용
        this.pluginVersion = currentPluginVersion;
    }

    /** Device synchronization updates liveness/version but must not consume the prompt-request quota. */
    public void recordSynchronization(String currentPluginVersion) {
        this.pluginVersion = currentPluginVersion;
        this.lastAccessAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
