package com.example.VideoManage.Interfaces;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface VideoStreamingInterface {
    ResponseEntity<Resource> getHlsFile(String uploadId, String quality, String fileName);
    ResponseEntity<Resource> getMasterPlaylist(String uploadId);
}
