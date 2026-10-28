package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.TransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRecipientRepository extends JpaRepository<TransferRecipient, UUID> {

    // Derived query matching the 'transferSession' property on TransferRecipient entity
    List<TransferRecipient> findByTransferSession_Id(UUID sessionId);

    List<TransferRecipient> findByTransferSessionId(UUID sessionId);
}