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
        @Index(name = "idx_last_access_at", columnList = "last_access_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientDevice {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;

    @Column(nullable = false)
    private LocalDate quotaDate;

    @Column(nullable = false)
    private int dailyRequestCount;

    @Column(nullable = false, length = 20)
    private String pluginVersion;

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
        this.recordAccess();
        this.pluginVersion = currentPluginVersion;
    }

    public void recordSynchronization(String currentPluginVersion) {
        this.pluginVersion = currentPluginVersion;
        this.lastAccessAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
