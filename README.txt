# Multi Video HLS Frontend

This frontend implements:

1. Multiple video selection.
2. Maximum 3 videos uploading simultaneously.
3. Maximum 3 chunks uploading simultaneously per video.
4. Each video gets its own uploadId.
5. Each video has an independent progress bar.
6. Each video calls /complete independently.
7. Each video is polled independently for processing status.
8. When COMPLETED, its HLS master.m3u8 is played below.
9. Uses hls.js for Chrome/Edge/Firefox and native HLS for Safari.

## Backend APIs expected

POST /api/uploads/init

Expected response should contain at least:

{
  "uploadId": "...",
  "totalChunks": 10,
  "chunkSize": 5242880
}

POST /api/uploads/{uploadId}/chunk

Multipart fields:

file
chunkNumber

POST /api/uploads/{uploadId}/complete

GET /api/uploads/{uploadId}

The GET status endpoint is needed for the frontend to know when
Spring Batch + FFmpeg have completed.

HLS endpoint expected:

GET /api/video/hls/{uploadId}/master.m3u8

## Start

Backend:

http://localhost:8080

Frontend:

python -m http.server 5500

Then open:

http://localhost:5500

If your backend URL is different, edit API in app.js.

## Important backend consideration

The frontend can upload 3 videos concurrently and 3 chunks per video.
That means up to 9 chunk HTTP requests can be active at the same time.

After uploads finish, your Spring Batch configuration should control
how many FFmpeg jobs run simultaneously. This prevents too many FFmpeg
processes from consuming RAM/CPU.

Because your backend deletes temporary chunk directories after merging,
that is still fine. The frontend only needs the final HLS files for playback.
