package com.errorpurifier.domain.audit.repository;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParsingAuditRepository extends JpaRepository<ParsingAuditLog,Long> {
    List<ParsingAuditLog> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Query("""
            select audit from ParsingAuditLog audit
             where audit.createdAt < :createdAt
                or (audit.createdAt = :createdAt and audit.id < :id)
             order by audit.createdAt desc, audit.id desc
            """)
    List<ParsingAuditLog> findSliceBefore(@Param("createdAt") LocalDateTime createdAt, @Param("id") Long id,
                                          Limit limit);
}
