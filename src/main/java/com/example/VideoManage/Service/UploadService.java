package com.example.VideoManage.Service;

import com.example.VideoManage.DTO.InitUploadRequest;
import com.example.VideoManage.DTO.UploadResponse;
import com.example.VideoManage.Entities.VideoStatus;
import com.example.VideoManage.Entities.VideoUploadEntity;
import com.example.VideoManage.Interfaces.UploadInterface;
import com.example.VideoManage.Repository.VideoUploadRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService implements UploadInterface {
    @Value("${video.upload.temp-path}")
    private String tempPath;
    @Value("${video.upload.final-path}")
    private String finalPath;
    @Value("${video.upload.hls-path}")
    private String hls;
    private final VideoUploadRepository videoRepository;
    private final VideoBatchService videoBatchService;
    @Transactional
    public UploadResponse initupload(InitUploadRequest request ) throws IOException {
        String uploadId= UUID.randomUUID().toString();
        long chunkSize = 5L * 1024 * 1024;
        int totalChunks = (int) Math.ceil((double) request.fileSize() / chunkSize);
        Files.createDirectories(Paths.get(finalPath));
        Files.createDirectories(Paths.get(tempPath));
        Files.createDirectories(Paths.get(hls));
        Path uploadDirectory= Paths.get(tempPath,uploadId);//get path and String append in path
        Files.createDirectories(uploadDirectory);//create dia.... of chunkFolder and if exist not do anything
        //save meta data
        String metadata=request.fileName()+"\n"+ request.fileSize()+"\n"+ request.contentType();
        Files.writeString(uploadDirectory.resolve("metadata.txt"),metadata,StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);//delete the existing content of file

        VideoUploadEntity video = new VideoUploadEntity();
        video.setUploadId(uploadId);
        video.setStatus(VideoStatus.UPLOADING);
        video.setTotalChunks(totalChunks);
        video.setFilePath(null);
        video.setCreate(LocalDateTime.now());
        video.setFileSize(request.fileSize());
        video.setFileName(request.fileName());
        video.setContentType(request.contentType());
        videoRepository.save(video);
        System.out.println("init complete with saving in database..");


        return new UploadResponse(uploadId,"Upload Initialization Success",chunkSize,totalChunks);
    }
    @Transactional
    public void saveChunk(String uploadId, int chunkNumber, MultipartFile file) throws IOException {

        Path uploadDirectory=Paths.get(tempPath,uploadId);
        Files.createDirectories(uploadDirectory);//create dia.... of chunkFolder, if already exist not create
        Path chunkPath = uploadDirectory.resolve("chunk-" + chunkNumber);
        System.out.println(chunkPath);
        try (var inputStream = file.getInputStream()) //open an input stream to read input binary data of uploadedfile
        {
            Files.copy(
                    inputStream,//read data from memory stream
                    chunkPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        System.out.println("save chunkNumber "+chunkNumber+" done");
    }



    public void completeUploading(String uploadId) throws IOException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        // 1. Upload metadata nikalo
        // 2. Check karo saare chunks available hain
        // 3. Spring Batch Job start karo
        // 4. Processing status set karo
        // 5. Response return karo
        VideoUploadEntity video=videoRepository.findByUploadId(uploadId).orElseThrow(()->new RuntimeException("Upload not found"));
        Integer totalChunks = video.getTotalChunks();
        if (totalChunks == null || totalChunks <= 0) {
            throw new RuntimeException("Invalid total chunks for upload: " + uploadId);
        }
        Path uploadDirectory = Paths.get(tempPath, uploadId);
        for (int i = 0; i < totalChunks; i++) {
            Path chunkPath = uploadDirectory.resolve("chunk-" + i);
            if (!Files.exists(chunkPath)) {
                throw new RuntimeException("Missing chunk: chunk-" + i);
            }
        }
        video.setStatus(VideoStatus.PROCESSING);
        videoRepository.saveAndFlush(video);
        videoBatchService.startVideoMergeJob(uploadId,totalChunks, video.getFileName());
    }

    public VideoUploadEntity getUploadStatus(String uploadId) {
        return videoRepository.findByUploadId(uploadId)
                .orElseThrow(() ->
                        new RuntimeException("Upload not found: " + uploadId)
                );
    }
}
