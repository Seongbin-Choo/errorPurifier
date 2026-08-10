package com.errorpurifier.domain.audit.entity;


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
@Table(name = "parsing_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 업데이트는 안 치므로 BaseTimeEntity 대신 직접 명시
public class ParsingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 실무 필수: 연관관계는 무조건 지연로딩(LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ClientDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_cache_id")
    private ErrorCache linkedCache;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueType issueType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawLogContent;

    @Column(columnDefinition = "TEXT")
    private String parsedLogContent;

    @Column(length = 500)
    private String userComment;

    @Column(nullable = false)
    private boolean isMasked;

    @Column(nullable = false)
    private boolean isReviewed = false;

    @CreatedDate
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public ParsingAuditLog(ClientDevice device, ErrorCache linkedCache, IssueType issueType, String rawLogContent, String parsedLogContent, String userComment, boolean isMasked) {
        this.device = device;
        this.linkedCache = linkedCache;
        this.issueType = issueType;
        this.rawLogContent = rawLogContent;
        this.parsedLogContent = parsedLogContent;
        this.userComment = userComment;
        this.isMasked = isMasked;
    }

    public enum IssueType {
        USER_REPORTED, LOW_COMPRESSION, PARSING_ERROR
    }

    public void markAsReviewed() {
        this.isReviewed = true;
    }
}