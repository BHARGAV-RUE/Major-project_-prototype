package com.cloudtransfer.stream.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChunkProcessingException.class)
    public ResponseEntity<Map<String, String>> handleChunkProcessingException(ChunkProcessingException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Encryption/Hashing Failure");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payload Too Large");
        errorResponse.put("message", "The uploaded chunk exceeds the maximum allowed size of 50MB.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unexpected Server Error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}