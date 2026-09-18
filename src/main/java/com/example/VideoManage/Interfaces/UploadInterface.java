package com.example.VideoManage.Interfaces;

import com.example.VideoManage.DTO.InitUploadRequest;
import com.example.VideoManage.DTO.UploadResponse;
import com.example.VideoManage.Entities.VideoUploadEntity;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadInterface {
    UploadResponse initupload(InitUploadRequest request ) throws IOException;
    void saveChunk(String uploadId, int chunkNumber, MultipartFile file) throws IOException;
    void completeUploading(String uploadId) throws IOException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException;
    VideoUploadEntity getUploadStatus(String uploadId);
}
