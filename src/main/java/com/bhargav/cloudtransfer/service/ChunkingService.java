package com.bhargav.cloudtransfer.service;

import com.bhargav.cloudtransfer.entity.Contributor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    /**
     * Splits a file dynamically based on the proportional available storage of each contributor.
     */
    public List<byte[]> splitFileDynamically(MultipartFile file, List<Contributor> contributors) throws IOException {
        byte[] fileBytes = file.getBytes();
        long totalFileSize = fileBytes.length;

        // 1. Calculate total available space across the selected drives
        long totalAvailableSpace = contributors.stream()
                .mapToLong(Contributor::getAvailableSpace)
                .sum();

        if (totalAvailableSpace < totalFileSize) {
            throw new RuntimeException("Insufficient total storage across network for this file.");
        }

        List<byte[]> chunks = new ArrayList<>();
        int currentOffset = 0;

        // 2. Generate chunks proportionally
        for (int i = 0; i < contributors.size(); i++) {
            Contributor contributor = contributors.get(i);

            // If it's the last contributor, give them the exact remaining bytes to avoid rounding errors
            if (i == contributors.size() - 1) {
                int remainingBytes = fileBytes.length - currentOffset;
                byte[] chunk = new byte[remainingBytes];
                System.arraycopy(fileBytes, currentOffset, chunk, 0, remainingBytes);
                chunks.add(chunk);
                break;
            }

            // Calculate the proportional weight and exact chunk size for this drive
            double weight = (double) contributor.getAvailableSpace() / totalAvailableSpace;
            int chunkSizeBytes = (int) Math.floor(totalFileSize * weight);

            byte[] chunk = new byte[chunkSizeBytes];
            System.arraycopy(fileBytes, currentOffset, chunk, 0, chunkSizeBytes);
            chunks.add(chunk);

            currentOffset += chunkSizeBytes;
        }

        return chunks;
    }
}