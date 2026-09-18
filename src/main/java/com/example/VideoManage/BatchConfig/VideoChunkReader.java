package com.example.VideoManage.BatchConfig;

import com.example.VideoManage.DTO.VideoChunk;
import com.example.VideoManage.Entities.VideoUploadEntity;

import io.micrometer.common.lang.Nullable;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
//iska matlab:
//Spring Batch mujhe baar-baar read() call karega aur main ek-ek VideoChunk return karunga.

public class VideoChunkReader implements ItemReader<VideoChunk> {
    private final Path uploadDirectory;
    private final int totalChunks;
    private int currentChunk=0;
    public VideoChunkReader(Path uploadDirectory, int totalChunks) {
        this.uploadDirectory = uploadDirectory;
        this.totalChunks = totalChunks;

    }
    //poore chunks read honge or obj return hoga
    @Override
    public VideoChunk read() throws Exception {//null ka meaning hai Reader ka data khatam ho gaya.
        if(currentChunk>=totalChunks) return null;
        Path chunkPath=uploadDirectory.resolve("chunk-"+currentChunk);
        if(!Files.exists(chunkPath)){
            throw new RuntimeException("Missing chunk-"+currentChunk);
        }
        VideoChunk chunk=new VideoChunk(currentChunk,chunkPath);
        currentChunk++;
        return chunk;
    }
}
