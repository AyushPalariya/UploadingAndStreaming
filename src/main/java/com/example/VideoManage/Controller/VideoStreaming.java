package com.example.VideoManage.Controller;

import com.example.VideoManage.Entities.VideoUploadEntity;
import com.example.VideoManage.Service.VideoStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@CrossOrigin("*")
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoStreaming {
    private final VideoStreamingService videoStreamingService;

//    @GetMapping("/stream/range/{uploadId}")
//    public ResponseEntity<Resource> streamVideoRange(@PathVariable String uploadId,
//                                                     @RequestHeader(value = "Range", required = false) String range) throws IOException {
//        System.out.println(range);//bytes=1001-1002
//        return videoStreamingService.calculate(uploadId,range);
//    }
    @GetMapping(value="/hls/{uploadId}/master.m3u8",produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<Resource> masterPlaylist(@PathVariable String uploadId){
        return videoStreamingService.getMasterPlaylist(uploadId);
    }
    @GetMapping("/hls/{uploadId}/{quality}/{fileName}")
    public ResponseEntity<Resource> hlsFile(
            @PathVariable String uploadId,
            @PathVariable String quality,
            @PathVariable String fileName
    ) {
        return videoStreamingService.getHlsFile(uploadId, quality, fileName);
    }
}
