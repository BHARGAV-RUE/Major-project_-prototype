package com.cloudtransfer.stream.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
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
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletResponse response) {
        log.error("[GlobalExceptionHandler] Intercepted stream exception: {}", ex.getMessage(), ex);

        // Ensure the response header is forced to application/json so Spring
        // won't attempt to serialize JSON errors into application/octet-stream
        if (response != null && !response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", 500,
                        "error", "Unexpected Server Error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Stream processing error occurred"
                ));
    }
}