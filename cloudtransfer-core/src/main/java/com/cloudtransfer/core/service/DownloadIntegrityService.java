package com.cloudtransfer.core.service;

import com.cloudtransfer.core.entity.FileChunk;
import com.cloudtransfer.core.service.BlockchainService.OnChainResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Integrity primitives for the download path.
 *
 * Intended download flow (the caller drives it):
 *   1. isSequenceComplete(...)  - every chunk index 0..n-1 is present, in order, with a stored hash
 *   2. verifySessionRoot(...)   - recomputed Merkle root of those hashes vs the anchored root
 *                                 (UNAVAILABLE = blockchain off/unreachable, NOT tampering)
 *   3. per chunk: download primary -> isChunkIntact(primary, hash)
 *                 if not intact (or missing) -> download replica -> isChunkIntact(replica, hash)
 *                 if that fails too -> abort the download
 *   4. decrypt and combine
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadIntegrityService {

    private final BlockchainService blockchainService;

    /** True only if the downloaded (encrypted) bytes hash to the value recorded at upload time. */
    public boolean isChunkIntact(FileChunk chunk, String actualDownloadedHash) {
        if (chunk == null) {
            return false;
        }
        String expected = chunk.getSha256Hash();
        if (expected == null || actualDownloadedHash == null) {
            log.warn("[DownloadIntegrity] Chunk #{}: expected or actual hash missing - treating as NOT intact.",
                    chunk.getChunkIndex());
            return false;
        }

        boolean intact = expected.trim().equalsIgnoreCase(actualDownloadedHash.trim());
        if (intact) {
            log.info("[DownloadIntegrity] Chunk #{} SHA-256 verified.", chunk.getChunkIndex());
        } else {
            log.error("[DownloadIntegrity] ALERT: Chunk #{} hash mismatch - data modified or corrupted.",
                    chunk.getChunkIndex());
        }
        return intact;
    }

    /** Sequence check that works with or without blockchain: nothing missing, reordered or unhashed. */
    public boolean isSequenceComplete(List<FileChunk> chunksInOrder, int expectedTotalChunks) {
        if (chunksInOrder == null || chunksInOrder.size() != expectedTotalChunks) {
            log.error("[DownloadIntegrity] Expected {} chunks but found {}.",
                    expectedTotalChunks, chunksInOrder == null ? 0 : chunksInOrder.size());
            return false;
        }
        for (int i = 0; i < chunksInOrder.size(); i++) {
            FileChunk chunk = chunksInOrder.get(i);
            if (chunk.getChunkIndex() == null || chunk.getChunkIndex() != i) {
                log.error("[DownloadIntegrity] Chunk sequence broken at position {} (found index {}).",
                        i, chunk.getChunkIndex());
                return false;
            }
            if (chunk.getSha256Hash() == null) {
                log.error("[DownloadIntegrity] Chunk #{} has no stored hash (upload incomplete?).", i);
                return false;
            }
        }
        return true;
    }

    /**
     * Compares the Merkle root recomputed from the stored chunk hashes with the root anchored on-chain.
     * MISMATCH means hashes or their order changed; UNAVAILABLE means the chain could not be checked.
     */
    public OnChainResult verifySessionRoot(String sessionId, String recomputedMerkleRoot) {
        OnChainResult result = blockchainService.checkRootOnChain(sessionId, recomputedMerkleRoot);
        switch (result) {
            case VERIFIED -> log.info("[DownloadIntegrity] Session {} root matches the on-chain anchor.", sessionId);
            case MISMATCH -> log.error("[DownloadIntegrity] ALERT: Session {} root does NOT match the on-chain anchor.", sessionId);
            case UNAVAILABLE -> log.warn("[DownloadIntegrity] Session {} could not be checked on-chain; relying on per-chunk hashes.", sessionId);
        }
        return result;
    }

    /**
     * @deprecated Cannot verify the replica (it has not been downloaded yet), so a corrupted replica
     * would pass silently. Use isChunkIntact() on primary, then on the replica after downloading it.
     */
    @Deprecated
    public String resolveChunkDownloadTarget(FileChunk primaryChunk, FileChunk replicaChunk, String actualDownloadedHash) {
        if (primaryChunk == null) {
            log.warn("[DownloadIntegrity] Primary chunk missing. Replica must be hash-verified by the caller after download.");
            return replicaChunk != null ? replicaChunk.getCloudFileId() : null;
        }
        if (isChunkIntact(primaryChunk, actualDownloadedHash)) {
            return primaryChunk.getCloudFileId();
        }
        if (replicaChunk != null) {
            log.warn("[DownloadIntegrity] Primary failed verification; returning replica file ID. "
                    + "The replica is NOT verified here - caller must run isChunkIntact() on it after download.");
            return replicaChunk.getCloudFileId();
        }
        throw new SecurityException("Primary chunk failed integrity verification and no replica is available.");
    }
}