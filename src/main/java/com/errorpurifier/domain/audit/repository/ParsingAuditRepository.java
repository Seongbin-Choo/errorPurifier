package com.errorpurifier.domain.audit.repository;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParsingAuditRepository extends JpaRepository<ParsingAuditLog,Long> {
    Page<ParsingAuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
