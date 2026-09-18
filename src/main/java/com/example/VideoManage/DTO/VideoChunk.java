package com.example.VideoManage.DTO;

import java.nio.file.Path;
//Reader jab chunk-0 read karega, woh ek VideoChunk object return karega or yahi chlta rahega for each chunk
public record VideoChunk(
        int chunkNumber,
        Path chunkPath
) {
}
