package com.example.VideoManage.DTO;

public record InitUploadRequest(
        String fileName,
        Long fileSize,
        String contentType){

}
