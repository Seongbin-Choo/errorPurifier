package com.errorpurifier.domain.knowledge.entity;

import com.errorpurifier.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "diagnostic_playbook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosticPlaybook extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String matchPattern;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String guidance;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private long matchCount;

    @Builder
    public DiagnosticPlaybook(String name, String matchPattern, String guidance, int priority) {
        this.name = name;
        this.matchPattern = matchPattern;
        this.guidance = guidance;
        this.priority = priority;
        this.isActive = true;
        this.matchCount = 0;
    }

    public void update(String name, String matchPattern, String guidance, int priority) {
        this.name = name;
        this.matchPattern = matchPattern;
        this.guidance = guidance;
        this.priority = priority;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}
