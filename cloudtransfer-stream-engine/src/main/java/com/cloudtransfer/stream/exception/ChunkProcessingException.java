package com.cloudtransfer.stream.exception;

public class ChunkProcessingException extends RuntimeException {
    public ChunkProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}