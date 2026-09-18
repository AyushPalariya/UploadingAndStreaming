package com.example.VideoManage.BatchConfig;

import com.example.VideoManage.DTO.VideoChunk;


import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//chunks ko actual final video mein write karna.
public class VideoChunkWriter implements ItemWriter<VideoChunk> {
    private final Path finalPath;
    public VideoChunkWriter(Path finalPath) {
        this.finalPath = finalPath;
    }
    @Override
    public void write(Chunk<? extends VideoChunk> chunks) throws Exception {
        try(OutputStream outputStream= Files.newOutputStream(finalPath, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)){
            for (VideoChunk chunk:chunks){
                Files.copy(chunk.chunkPath(),outputStream);
                Files.deleteIfExists(chunk.chunkPath());
            }
        }
    }
}
