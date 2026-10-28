package com.cloudtransfer.core.repository;

import com.cloudtransfer.core.entity.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {

    List<FileChunk> findBySession_IdOrderByChunkIndexAsc(UUID sessionId);

    List<FileChunk> findBySessionIdOrderByChunkIndexAsc(UUID sessionId);

    // Added to support detaching chunks during contributor node forfeiture / termination
    List<FileChunk> findByPrimaryContributor_IdOrderByChunkIndexAsc(UUID primaryContributorId);

    List<FileChunk> findByReplicaContributor_IdOrderByChunkIndexAsc(UUID replicaContributorId);
}