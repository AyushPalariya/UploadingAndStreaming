package com.example.VideoManage.Interfaces;

import java.nio.file.Path;

public interface FfmpegInterface {
    void convertToHls(String uploadId, Path inputVideo);
    void createMasterPlaylist(Path outputDir);
}
