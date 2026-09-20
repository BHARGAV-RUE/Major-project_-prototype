package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DownloadTokenRepository extends JpaRepository<DownloadToken, UUID> {
    // Custom query to look up a transfer using the secure token hash
    Optional<DownloadToken> findByTokenHash(String tokenHash);
}