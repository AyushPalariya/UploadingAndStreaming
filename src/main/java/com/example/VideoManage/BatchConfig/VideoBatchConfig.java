package com.example.VideoManage.BatchConfig;

import com.example.VideoManage.DTO.VideoChunk;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;

import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class VideoBatchConfig {
    private final String tempPath;
    private final String finalPath;
    public VideoBatchConfig(@Value("${video.upload.temp-path}") String tempPath,
                            @Value("${video.upload.final-path}") String finalPath) {
        this.tempPath = tempPath;
        this.finalPath=finalPath;
    }
    @Bean                        //To maintain job execution history.
    public Job videoMergeJob(JobRepository jobRepository, Step videoMergeStep, VideoBatchJobListener listner){
        return new JobBuilder("videoMergeJob",jobRepository)
                .listener(listner)
                .start(videoMergeStep)//Jab videoMergeJob start hoga, sabse pehle videoMergeStep execute karo
                .build();
    }
    @Bean
    public Step videoMergeStep(JobRepository jobRepository,PlatformTransactionManager transactionManager,
                               ItemReader<VideoChunk> videoChunkReader,
                               ItemProcessor<VideoChunk,VideoChunk> videoChunkProcessor,
                               ItemWriter<VideoChunk> videoChunkWriter){
        return new StepBuilder("videoMergeStep",jobRepository)
                .<VideoChunk,VideoChunk>chunk(6,transactionManager)//Matlab Spring Batch ek transaction ke andar 6 items tak process karega,
                                            // phir writer ko dega aur transaction commit karega.
                .reader(videoChunkReader)//Yahan hum apna previous class connect kar rahe hain:
                .processor(videoChunkProcessor)
                .writer(videoChunkWriter)
                .build();
    }
    @Bean
    @StepScope                                  //Current running Batch Job ke jobParameters mein se uploadId mujhe do.
    public ItemReader<VideoChunk> videoChunkReader(@Value("#{jobParameters['uploadId']}") String uploadId,
                                                   @Value("#{jobParameters['totalChunks']}") Long totalChunks) {
        Path uploadDirectory= Paths.get(tempPath,uploadId);
        return new VideoChunkReader(uploadDirectory,totalChunks.intValue());

    }

    @Bean
    public ItemProcessor<VideoChunk, VideoChunk> videoChunkProcessor() {
        return new VideoChunkProcess();
    }

    @Bean
    @StepScope  //Create bean at runtime when jobParameters fetch
    public ItemWriter<VideoChunk> videoChunkWriter(@Value("#{jobParameters['fileName']}") String fileName) throws IOException {
        Path finalVideo = Paths.get(finalPath, fileName);
        Files.createDirectories(finalVideo.getParent());
        return new VideoChunkWriter(finalVideo);
    }
}
