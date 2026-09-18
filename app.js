const API = "http://localhost:8080/api";

// Architecture:
// - Maximum 3 videos upload at the same time.
// - Maximum 3 chunks upload concurrently inside each video.
// - Each video has its own uploadId and progress bar.
// - After /complete, that video's processing is polled independently.
// - When COMPLETED, its HLS player is added below.

const MAX_VIDEO_CONCURRENCY = 3;
const MAX_CHUNK_CONCURRENCY = 3;
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;

const fileInput = document.getElementById("videoFiles");
const uploadBtn = document.getElementById("uploadBtn");
const selectionInfo = document.getElementById("selectionInfo");
const queueElement = document.getElementById("uploadQueue");
const videoList = document.getElementById("videoList");

const jobs = [];

fileInput.addEventListener("change", () => {
  const files = Array.from(fileInput.files);

  selectionInfo.textContent = files.length
    ? `${files.length} video(s) selected`
    : "";

  renderQueue(files);
});

uploadBtn.addEventListener("click", startUploads);

function renderQueue(files) {
  queueElement.innerHTML = "";

  if (!files.length) {
    queueElement.innerHTML = '<p class="empty">No videos selected.</p>';
    return;
  }

  files.forEach((file, index) => {
    const item = document.createElement("div");
    item.className = "queue-item";
    item.id = `job-${index}`;

    item.innerHTML = `
      <div class="queue-head">
        <span class="file-name"></span>
        <span class="status">Waiting...</span>
      </div>
      <div class="progress-label">
        <span class="progress-text">0%</span>
        <span class="chunk-text"></span>
      </div>
      <div class="progress">
        <div class="progress-bar"></div>
      </div>
    `;

    item.querySelector(".file-name").textContent =
      `${file.name} (${formatBytes(file.size)})`;

    queueElement.appendChild(item);
  });
}

async function startUploads() {
  const files = Array.from(fileInput.files);

  if (!files.length) {
    alert("Please select at least one video.");
    return;
  }

  uploadBtn.disabled = true;

  jobs.length = 0;

  files.forEach((file, index) => {
    jobs.push({
      file,
      index,
      uploadId: null,
      totalChunks: 0,
      completedChunks: 0
    });
  });

  // Only MAX_VIDEO_CONCURRENCY videos run at the same time.
  await runWithConcurrency(jobs, MAX_VIDEO_CONCURRENCY, uploadOneVideo);

  uploadBtn.disabled = false;
}

async function uploadOneVideo(job) {
  const ui = getJobUI(job.index);

  try {
    setStatus(ui, "Initializing...");

    // 1. Create uploadId.
    const initResponse = await fetch(`${API}/uploads/init`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        fileName: job.file.name,
        fileSize: job.file.size,
        contentType: job.file.type || "video/mp4"
      })
    });

    if (!initResponse.ok) {
      throw new Error(await readError(initResponse));
    }

    const info = await initResponse.json();

    job.uploadId = info.uploadId;
    const chunkSize = Number(info.chunkSize) || DEFAULT_CHUNK_SIZE;

    job.totalChunks = info.totalChunks ||
      Math.ceil(job.file.size / chunkSize);

    setStatus(ui, `Uploading... uploadId: ${job.uploadId}`);

    // 2. Upload this video's chunks concurrently.
    const chunks = Array.from(
      { length: job.totalChunks },
      (_, chunkNumber) => chunkNumber
    );

    await runWithConcurrency(
      chunks,
      MAX_CHUNK_CONCURRENCY,
      async (chunkNumber) => {
        const start = chunkNumber * chunkSize;
        const end = Math.min(start + chunkSize, job.file.size);
        const chunk = job.file.slice(start, end);

        await uploadChunk(job.uploadId, chunkNumber, chunk);

        job.completedChunks++;

        const percent = Math.round(
          (job.completedChunks / job.totalChunks) * 100
        );

        setProgress(
          ui,
          percent,
          `${job.completedChunks}/${job.totalChunks} chunks`
        );
      }
    );

    // 3. Tell backend all chunks are ready.
    setStatus(ui, "Starting processing...");

    const completeResponse = await fetch(
      `${API}/uploads/${job.uploadId}/complete`,
      { method: "POST" }
    );

    if (!completeResponse.ok) {
      throw new Error(await readError(completeResponse));
    }

    setProgress(ui, 100, "Upload complete");
    setStatus(ui, "FFmpeg processing...");

    // 4. Poll only this video's uploadId.
    const completedVideo = await waitForProcessing(job.uploadId, ui);

    setStatus(ui, "Completed ✓");
    addVideo(completedVideo, job.file.name);

  } catch (error) {
    console.error(error);
    setStatus(ui, "Failed");
    ui.bar.style.width = "0%";
    ui.progressText.textContent = "Failed";
    ui.chunkText.textContent = error.message;
  }
}

function uploadChunk(uploadId, chunkNumber, chunk) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();

    // IMPORTANT:
    // Keep these parameter names identical to your Spring controller.
    formData.append("chunkNumber", chunkNumber);
    formData.append("file", chunk, `chunk-${chunkNumber}`);

    const xhr = new XMLHttpRequest();

    xhr.open(
      "POST",
      `${API}/uploads/${encodeURIComponent(uploadId)}/chunk`
    );

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve();
      } else {
        reject(
          new Error(`Chunk ${chunkNumber} failed (${xhr.status})`)
        );
      }
    };

    xhr.onerror = () => {
      reject(new Error("Network error while uploading chunk."));
    };

    xhr.send(formData);
  });
}

async function waitForProcessing(uploadId, ui) {
  const maxAttempts = 600;

  for (let attempt = 0; attempt < maxAttempts; attempt++) {

    const response = await fetch(
      `${API}/uploads/${encodeURIComponent(uploadId)}`
    );

    if (!response.ok) {
      throw new Error("Could not read processing status.");
    }

    const video = await response.json();

    const status = String(video.status || "").toUpperCase();

    if (status === "COMPLETED") {
      return video;
    }

    if (status === "FAILED") {
      throw new Error("FFmpeg/video processing failed.");
    }

    setStatus(ui, `Processing... ${video.status || "IN_PROGRESS"}`);

    await sleep(2000);
  }

  throw new Error("Processing timeout.");
}

function addVideo(video, fallbackName) {
  const empty = videoList.querySelector(".empty");
  if (empty) empty.remove();

  const card = document.createElement("div");
  card.className = "video-card";

  const title = document.createElement("h3");
  title.textContent = video.fileName || fallbackName || "Uploaded video";

  const meta = document.createElement("div");
  meta.className = "video-meta";
  meta.textContent =
    `Upload ID: ${video.uploadId} | Status: ${video.status}`;

  const player = document.createElement("video");
  player.controls = true;
  player.playsInline = true;

  const masterUrl =
    `${API}/video/hls/${encodeURIComponent(video.uploadId)}/master.m3u8`;

  attachHls(player, masterUrl);

  const url = document.createElement("div");
  url.className = "url";
  url.textContent = masterUrl;

  card.appendChild(title);
  card.appendChild(meta);
  card.appendChild(player);
  card.appendChild(url);

  videoList.prepend(card);
}

function attachHls(videoElement, url) {
  if (window.Hls && Hls.isSupported()) {
    const hls = new Hls();

    hls.loadSource(url);
    hls.attachMedia(videoElement);

    hls.on(Hls.Events.ERROR, (event, data) => {
      if (data.fatal) {
        console.error("HLS fatal error:", data);
      }
    });

    return;
  }

  // Safari/native HLS.
  if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
    videoElement.src = url;
    return;
  }

  console.error("This browser does not support HLS.");
}

async function runWithConcurrency(items, limit, worker) {
  let nextIndex = 0;

  async function runner() {
    while (true) {
      const index = nextIndex++;

      if (index >= items.length) return;

      await worker(items[index]);
    }
  }

  const workerCount = Math.min(limit, items.length);

  await Promise.all(
    Array.from({ length: workerCount }, () => runner())
  );
}

function getJobUI(index) {
  const root = document.getElementById(`job-${index}`);

  return {
    root,
    bar: root.querySelector(".progress-bar"),
    progressText: root.querySelector(".progress-text"),
    chunkText: root.querySelector(".chunk-text"),
    status: root.querySelector(".status")
  };
}

function setProgress(ui, percent, chunkText) {
  ui.bar.style.width = `${percent}%`;
  ui.progressText.textContent = `${percent}%`;
  ui.chunkText.textContent = chunkText || "";
}

function setStatus(ui, text) {
  ui.status.textContent = text;
}

async function readError(response) {
  try {
    return (await response.text()) || `HTTP ${response.status}`;
  } catch {
    return `HTTP ${response.status}`;
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function formatBytes(bytes) {
  if (!bytes) return "0 Bytes";

  const units = ["Bytes", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));

  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
}
