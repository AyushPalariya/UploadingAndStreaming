package com.example.VideoManage.Controller;

import com.example.VideoManage.Service.FfmpegService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor

public class FFmpegTestController {

    private final FfmpegService ffmpegService;

    @PostMapping("/hls")
    public ResponseEntity<String> testHls() {

        try {

            String uploadId = "test-123";
            Path inputVideo =
                    Paths.get("E:/Videos/sample.mp4");

            ffmpegService.convertToHls(
                    uploadId,
                    inputVideo
            );

            return ResponseEntity.ok(
                    "HLS generated successfully"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }
}
