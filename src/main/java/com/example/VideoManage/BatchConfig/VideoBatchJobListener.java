package com.example.VideoManage.BatchConfig;

import com.example.VideoManage.Entities.VideoStatus;
import com.example.VideoManage.Entities.VideoUploadEntity;
import com.example.VideoManage.Repository.VideoUploadRepository;
import com.example.VideoManage.Service.FfmpegService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class VideoBatchJobListener implements JobExecutionListener {
    @Value("${video.upload.temp-path}")
    private String tempPath;

    private final VideoUploadRepository videoRepository;

    @Value("${video.upload.final-path}")
    private String finalPath;//mainVideo
    private final FfmpegService ffmpegService;
    @Value("${video.upload.hls-path}")
    private String hlsPath;

    @Override               //represent job execution
    public void beforeJob(JobExecution jobExecution) {
        String uploadId = jobExecution.getJobParameters().getString("uploadId");
        System.out.println(
                "Video batch job started for uploadId: " + uploadId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {

            String uploadId = jobExecution.getJobParameters().getString("uploadId");
            VideoUploadEntity video = videoRepository.findByUploadId(uploadId).orElseThrow(() ->
                    new RuntimeException("Upload not found: " + uploadId));

            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                try {
                    String fileName = Path.of(video.getFileName()).getFileName().toString();
                    Path finalFilePath = Path.of(finalPath, fileName);
                    //video.setStatus(VideoStatus.PROCESSING);
                    //videoRepository.save(video);
                    if (!Files.exists(finalFilePath)) {
                        throw new IOException("Merge Video not Found");
                    }
                    else {
                        System.out.println("video merge successfully");
                    }
                    System.out.println("Processing joblistner");
                    // Delete chunks after successful merge
                    Path uploadDirectory = Path.of(tempPath, uploadId);
                    try {
                        deleteDirectory(uploadDirectory);
                        System.out.println("Directory delete successfully: " + uploadId);

                    } catch (IOException e) {
                        System.out.println("Failed to delete directory: " + uploadId);
                    }
                    //ffmpeg process
                    ffmpegService.convertToHls(uploadId, finalFilePath);
                    System.out.println("HLS completed...");
                    Path masterPlaylist = Paths.get(hlsPath, uploadId, "master.m3u8");
                    if (!Files.exists(masterPlaylist)) {
                        throw new IOException(
                                "HLS master playlist not found: "
                                        + masterPlaylist
                        );
                    }
                    Files.deleteIfExists(finalFilePath);
                    System.out.println(
                            "Master playlist created: "
                                    + masterPlaylist+" and original video deleted.."
                    );
                    video.setFilePath(masterPlaylist.toString());
                    video.setStatus(VideoStatus.COMPLETED);
                    videoRepository.save(video);
                    System.out.println("finally job completed");

                }
                catch (Exception e){
                e.printStackTrace();
                    video.setStatus(VideoStatus.FAILED);
                    videoRepository.save(video);
                    System.out.println(
                            "Video batch job failed: "
                                    + uploadId
                    );
                }

            } else {
                video.setStatus(VideoStatus.FAILED);
                videoRepository.save(video);
                System.out.println(
                        "Video batch job failed: "
                                + uploadId
                );
            }

    }

    public void deleteDirectory(Path uploadDirectory) throws IOException {
        Path meta= Paths.get(String.valueOf(uploadDirectory),"metadata.txt");
        System.out.println(meta);
        Files.deleteIfExists(meta);
        Files.deleteIfExists(uploadDirectory);
    }
}
