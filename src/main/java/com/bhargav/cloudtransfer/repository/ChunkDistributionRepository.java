package com.bhargav.cloudtransfer.repository;

import com.bhargav.cloudtransfer.entity.Chunk;
import com.bhargav.cloudtransfer.entity.ChunkDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChunkDistributionRepository extends JpaRepository<ChunkDistribution, UUID> {
    // Custom query to find all storage locations for a specific chunk
    List<ChunkDistribution> findByChunkId(UUID chunkId);
    ChunkDistribution findFirstByChunk(Chunk chunk);
}