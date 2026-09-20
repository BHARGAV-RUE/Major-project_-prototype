package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.Chunk;
import com.bhargav.cloudtransfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
    List<Chunk> findByTransferId(UUID transferId);
    List<Chunk> findAllByTransferOrderByChunkIndexAsc(Transfer transfer);
}