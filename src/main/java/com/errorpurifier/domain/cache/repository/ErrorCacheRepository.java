package com.errorpurifier.domain.cache.repository;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorCacheRepository extends JpaRepository<ErrorCache,Long> {
    Optional<ErrorCache> findByCacheKeyAndIsBlindedFalse(String cacheKey);
    Optional<ErrorCache> findByCacheKey(String cacheKey);
}
