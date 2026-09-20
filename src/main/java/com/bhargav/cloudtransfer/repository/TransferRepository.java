package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
}