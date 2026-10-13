package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.TransferSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransferSessionRepository extends JpaRepository<TransferSession, UUID> {
}