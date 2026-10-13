package com.cloudtransfer.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitMessage {
    private UUID sessionId;
    private Integer totalChunks;
}