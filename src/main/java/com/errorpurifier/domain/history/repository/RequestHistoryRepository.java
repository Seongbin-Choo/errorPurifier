package com.errorpurifier.domain.history.repository;

import com.errorpurifier.domain.history.entity.RequestHistory;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistory,Long> {
    List<RequestHistory> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

    @Query("""
            select history from RequestHistory history
             where history.createdAt < :createdAt
                or (history.createdAt = :createdAt and history.id < :id)
             order by history.createdAt desc, history.id desc
            """)
    List<RequestHistory> findSliceBefore(@Param("createdAt") LocalDateTime createdAt, @Param("id") Long id,
                                         Limit limit);
}
