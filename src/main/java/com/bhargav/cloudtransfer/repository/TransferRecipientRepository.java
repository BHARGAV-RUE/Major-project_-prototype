package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.TransferRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransferRecipientRepository extends JpaRepository<TransferRecipient, UUID> {
    List<TransferRecipient> findByTransferId(UUID transferId);
}