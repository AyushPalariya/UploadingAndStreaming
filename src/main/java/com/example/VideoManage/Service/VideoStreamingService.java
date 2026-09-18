package com.example.VideoManage.Service;

import com.example.VideoManage.Entities.VideoUploadEntity;
import com.example.VideoManage.Interfaces.VideoStreamingInterface;
import com.example.VideoManage.Repository.VideoUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class VideoStreamingService implements VideoStreamingInterface {
    @Value("${video.upload.hls-path}")
    private String hlsPath;

    public ResponseEntity<Resource> getMasterPlaylist(String uploadId) {
        Path masterPlaylist=Paths.get(hlsPath,uploadId,"master.m3u8");
        if(!Files.exists(masterPlaylist)){
            return ResponseEntity.notFound().build();
        }
        Resource resource=new FileSystemResource(masterPlaylist);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl")).header( HttpHeaders.CACHE_CONTROL,
                "no-cache").body(resource);

    }

    public ResponseEntity<Resource> getHlsFile(String uploadId, String quality, String fileName) {
        if(!quality.matches("360p|480p|720p|1080p")||!fileName.matches("playlist\\.m3u8|segment_\\d+\\.ts")){
            return ResponseEntity.badRequest().build();
        }
        Path file =
                Paths.get(
                        hlsPath,
                        uploadId,
                        quality,
                        fileName
                );
        System.out.println(quality+" "+fileName);

        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource =
                new FileSystemResource(file);

        MediaType mediaType;

        if (fileName.endsWith(".m3u8")) {
            mediaType = MediaType.parseMediaType(
                    "application/vnd.apple.mpegurl"
            );
        } else {
            mediaType = MediaType.parseMediaType(
                    "video/mp2t"
            );
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);

    }
}
