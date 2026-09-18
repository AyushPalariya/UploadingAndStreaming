package com.example.VideoManage.Service;

import com.example.VideoManage.Interfaces.FfmpegInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FfmpegService implements FfmpegInterface {
    @Value("${video.upload.ffmpeg.path}")
    private String ffmpegPath;

    @Value("${video.upload.hls-path}")
    private String hlsPath;

    public void convertToHls(String uploadId,Path inputVideo)  {
    try {
        Path outputDir = Paths.get(hlsPath, uploadId);
        Files.createDirectories(outputDir);

        Path _360p = outputDir.resolve("360p");
        Path _480p = outputDir.resolve("480p");
        Path _720p = outputDir.resolve("720p");
        Path _1080p = outputDir.resolve("1080p");
        Files.createDirectories(_360p);
        Files.createDirectories(_480p);
        Files.createDirectories(_720p);
        Files.createDirectories(_1080p);

        Path playlist360 = _360p.resolve("playlist.m3u8");
        Path playlist480 = _480p.resolve("playlist.m3u8");
        Path playlist720 = _720p.resolve("playlist.m3u8");
        Path playlist1080 = _1080p.resolve("playlist.m3u8");
        Path segment360 = _360p.resolve("segment_%03d.ts");
        Path segment480 = _480p.resolve("segment_%03d.ts");
        Path segment720 = _720p.resolve("segment_%03d.ts");
        Path segment1080 = _1080p.resolve("segment_%03d.ts");

        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,

                "-i", inputVideo.toString(),

                "-filter_complex",

                "[0:v]split=4[v360][v480][v720][v1080];" +

                        "[v360]scale=640:360:force_original_aspect_ratio=decrease," +
                        "pad=640:360:(ow-iw)/2:(oh-ih)/2[v360out];" +

                        "[v480]scale=854:480:force_original_aspect_ratio=decrease," +
                        "pad=854:480:(ow-iw)/2:(oh-ih)/2[v480out];" +

                        "[v720]scale=1280:720:force_original_aspect_ratio=decrease," +
                        "pad=1280:720:(ow-iw)/2:(oh-ih)/2[v720out];" +

                        "[v1080]scale=1920:1080:force_original_aspect_ratio=decrease," +
                        "pad=1920:1080:(ow-iw)/2:(oh-ih)/2[v1080out]",

                // 360p
                "-map", "[v360out]",
                "-map", "0:a:0?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-b:v", "800k",
                "-maxrate", "856k",
                "-bufsize", "1200k",
                "-c:a", "aac",
                "-b:a", "96k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segment360.toString(),
                playlist360.toString(),

                // 480p
                "-map", "[v480out]",
                "-map", "0:a:0?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-b:v", "1400k",
                "-maxrate", "1498k",
                "-bufsize", "2100k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segment480.toString(),
                playlist480.toString(),

                // 720p
                "-map", "[v720out]",
                "-map", "0:a:0?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-b:v", "2800k",
                "-maxrate", "2996k",
                "-bufsize", "4200k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segment720.toString(),
                playlist720.toString(),

                // 1080p
                "-map", "[v1080out]",
                "-map", "0:a:0?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-b:v", "5000k",
                "-maxrate", "5350k",
                "-bufsize", "7500k",
                "-c:a", "aac",
                "-b:a", "192k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segment1080.toString(),
                playlist1080.toString()
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg: " + line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed. Exit code: " + exitCode);
        }

        createMasterPlaylist(outputDir);

        System.out.println(
                "HLS generation completed for uploadId: " + uploadId
        );
    }
    catch (IOException | InterruptedException e){
        e.printStackTrace();
        System.out.println(e);
    }
    }

    public void createMasterPlaylist(Path outputDir) {
        try {
            String masterPlaylist =
                    "#EXTM3U\n" +
                            "#EXT-X-VERSION:3\n\n" +

                            "#EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=640x360\n" +
                            "360p/playlist.m3u8\n\n" +

                            "#EXT-X-STREAM-INF:BANDWIDTH=1600000,RESOLUTION=854x480\n" +
                            "480p/playlist.m3u8\n\n" +

                            "#EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720\n" +
                            "720p/playlist.m3u8\n\n" +

                            "#EXT-X-STREAM-INF:BANDWIDTH=5200000,RESOLUTION=1920x1080\n" +
                            "1080p/playlist.m3u8\n";

            Files.writeString(
                    outputDir.resolve("master.m3u8"),
                    masterPlaylist
            );
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(e);
        }
    }

}
