package com.example.VideoManage.Controller;

import com.example.VideoManage.DTO.InitUploadRequest;
import com.example.VideoManage.DTO.UploadResponse;
import com.example.VideoManage.Entities.VideoStatus;
import com.example.VideoManage.Entities.VideoUploadEntity;
import com.example.VideoManage.Service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
@CrossOrigin("*")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController{
    private final UploadService uploadService;
    //verify ki bhejsakta hu ya nhi
    @PostMapping("/init")
    public ResponseEntity<UploadResponse> initUpload(@RequestBody InitUploadRequest request) throws IOException {
        UploadResponse uploadResponse =uploadService.initupload(request);
        return ResponseEntity.ok(uploadResponse);
    }
    @PostMapping("/{uploadId}/chunk")
    public ResponseEntity<?> uploadChunks(
            @PathVariable String uploadId,@RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        uploadService.saveChunk(uploadId,chunkNumber,file);
        return ResponseEntity.ok(
                Map.of("message", "Chunk uploaded Successfully",
                        "chunkNumber", chunkNumber));
    }
    //merge here
    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<?> completeUpload(@PathVariable String uploadId) throws IOException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("start completion..."+uploadId);
        uploadService.completeUploading(uploadId);
        System.out.println("CompleteUpload");
        return ResponseEntity.ok().body(
                Map.of("message","Video uploaded Successfully"));
    }
    @GetMapping("/{uploadId}")
    public ResponseEntity<VideoUploadEntity> getUploadStatus(
            @PathVariable String uploadId) {

        VideoUploadEntity video =
                uploadService.getUploadStatus(uploadId);

        return ResponseEntity.ok(video);
    }

}


