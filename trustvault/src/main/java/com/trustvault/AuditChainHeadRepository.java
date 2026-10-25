package com.trustvault;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditChainHeadRepository
        extends JpaRepository<AuditChainHead, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from AuditChainHead h where h.id = :id")
    Optional<AuditChainHead> lockForWrite(@Param("id") int id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select h from AuditChainHead h where h.id = :id")
    Optional<AuditChainHead> lockForRead(@Param("id") int id);
}
