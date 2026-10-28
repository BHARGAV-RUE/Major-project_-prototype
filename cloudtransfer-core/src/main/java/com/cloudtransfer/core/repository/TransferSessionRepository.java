package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.TransferSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransferSessionRepository extends JpaRepository<TransferSession, UUID> {
    List<TransferSession> findByStatusNotAndExpiryTimestampBefore(String status, LocalDateTime timestamp);
}