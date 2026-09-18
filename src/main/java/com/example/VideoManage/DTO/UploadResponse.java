package com.example.VideoManage.DTO;

public record UploadResponse(
        String uploadId,
        String message,
        Long chunkSize,
        Integer totalChunks
) {
}