package com.cloudtransfer.stream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ResumableChunkTrackerService {

    // Map<SessionId, Set<ChunkIndex>>
    private final Map<UUID, Set<Integer>> uploadedChunksMap = new HashMap<>();

    public synchronized void recordChunkUploaded(UUID sessionId, int chunkIndex) {
        uploadedChunksMap.computeIfAbsent(sessionId, k -> new HashSet<>()).add(chunkIndex);
        log.info("[ResumableTracker] Recorded chunk #{} for session: {}", chunkIndex, sessionId);
    }

    public synchronized List<Integer> getUploadedChunkIndices(UUID sessionId) {
        Set<Integer> chunks = uploadedChunksMap.getOrDefault(sessionId, Collections.emptySet());
        List<Integer> sortedList = new ArrayList<>(chunks);
        Collections.sort(sortedList);
        return sortedList;
    }

    public synchronized boolean isChunkAlreadyUploaded(UUID sessionId, int chunkIndex) {
        return uploadedChunksMap.getOrDefault(sessionId, Collections.emptySet()).contains(chunkIndex);
    }
}