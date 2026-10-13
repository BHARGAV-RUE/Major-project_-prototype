package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.TransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRecipientRepository extends JpaRepository<TransferRecipient, UUID> {
    Optional<TransferRecipient> findByDownloadToken(String downloadToken);
}