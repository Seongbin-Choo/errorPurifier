package com.errorpurifier.domain.audit.repository;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParsingAuditRepository extends JpaRepository<ParsingAuditLog,Long> {
}
