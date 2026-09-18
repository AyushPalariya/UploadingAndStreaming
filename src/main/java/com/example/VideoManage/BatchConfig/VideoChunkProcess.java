package com.example.VideoManage.BatchConfig;

import com.example.VideoManage.DTO.VideoChunk;


import jakarta.annotation.Nullable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

public class VideoChunkProcess implements ItemProcessor<VideoChunk,VideoChunk> {

    //Spring Batch ek-ek item ko read + process karta hai,
    // lekin har item ke baad write nahi karta.
    //10 items means 10 chunks
    @Override
    public VideoChunk process(VideoChunk chunk) throws Exception {
        //validate
        if(!Files.exists(chunk.chunkPath())){
            throw new IllegalStateException("Chunk doesn't exist-"+chunk.chunkNumber());
        }
        if(Files.size(chunk.chunkPath())==0){
            throw new IllegalStateException("Chunk is empty: " + chunk.chunkNumber());
        }
        return chunk;
    }
}
