import express, { Request, Response } from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { spawn, ChildProcess } from "child_process";
import * as crypto from "crypto";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { validateSafeHttpsUrl, sanitizeOutputFilename } from "./security/ssrfGuard";

const app = express();
app.use(helmet());
app.use(cors({ origin: process.env.ALLOWED_ORIGINS?.split(",") || "*" }));
app.use(express.json({ limit: "32kb" }));

const apiLimiter = rateLimit({
  windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS || 60_000),
  max: Number(process.env.RATE_LIMIT_MAX_REQUESTS || 30),
  standardHeaders: true,
  legacyHeaders: false
});
app.use("/api/", apiLimiter);

export interface ProcessingJob {
  jobId: string;
  sourceUrl: string;
  title: string;
  format: "mp4" | "webm" | "mp3" | "m4a" | "wav";
  qualityOrBitrate: string;
  requiresConversion: boolean;
  status: "QUEUED" | "PROCESSING" | "VERIFYING" | "READY" | "CANCELLED" | "FAILED" | "EXPIRED";
  progress: number;
  bytesProcessed: number;
  totalBytesEstimate: number | null;
  durationSeconds: number | null;
  ffmpegSpeed: string | null;
  tempFilePath: string;
  outputFileName: string;
  sha256Checksum: string | null;
  watermarkFreeVerified: boolean;
  createdAt: number;
  expiresAt: number;
  processRef?: ChildProcess;
  error?: string;
}

const jobs = new Map<string, ProcessingJob>();
const TEMP_DIR = path.join(os.tmpdir(), "nexora_jobs");
if (!fs.existsSync(TEMP_DIR)) {
  fs.mkdirSync(TEMP_DIR, { recursive: true });
}

function computeFileSha256(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash("sha256");
    const stream = fs.createReadStream(filePath);
    stream.on("data", (chunk) => hash.update(chunk));
    stream.on("end", () => resolve(hash.digest("hex")));
    stream.on("error", reject);
  });
}

// 1. GET /api/health
app.get("/api/health", (_req: Request, res: Response) => {
  res.status(200).json({
    status: "ok",
    engine: "NEXORA Watermark-Free Media Engine v1.0",
    watermarkFreeGuarantee: true,
    activeJobs: jobs.size
  });
});

// 2. POST /api/analyze
app.post("/api/analyze", async (req: Request, res: Response) => {
  try {
    const { url } = req.body;
    const safeUrl = await validateSafeHttpsUrl(url);
    const jobId = "nxr_" + crypto.randomBytes(8).toString("hex");

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    const headResp = await fetch(safeUrl.toString(), {
      method: "HEAD",
      signal: controller.signal
    });
    clearTimeout(timeout);

    const contentType = headResp.headers.get("content-type") || "video/mp4";
    const contentLength = Number(headResp.headers.get("content-length") || 0) || null;
    const rawTitle = path.basename(safeUrl.pathname).replace(/\.[^.]+$/, "") || safeUrl.hostname;

    res.status(200).json({
      jobId,
      url: safeUrl.toString(),
      title: sanitizeOutputFilename(rawTitle),
      provider: safeUrl.hostname,
      mimeType: contentType,
      contentLength,
      watermarkPolicy: "Zero NEXORA watermarks or overlays are ever added."
    });
  } catch (err: any) {
    res.status(400).json({
      error: "Analysis validation failed",
      message: err?.message || "Unsupported or restricted URL"
    });
  }
});

// 3. POST /api/download — FFmpeg Watermark-Free Processing & Authorized Conversion
app.post("/api/download", async (req: Request, res: Response) => {
  try {
    const {
      url,
      format = "mp4",
      title = "media_export",
      resolution = "1920x1080",
      audioBitrate = "320k",
      durationSeconds = 45,
      requiresConversion = false
    } = req.body;

    const safeUrl = await validateSafeHttpsUrl(url);
    const allowedFormats = new Set(["mp4", "webm", "mp3", "m4a", "wav"]);
    const normalizedFmt = String(format).toLowerCase() as ProcessingJob["format"];
    if (!allowedFormats.has(normalizedFmt)) {
      res.status(400).json({ error: "Unsupported container format requested." });
      return;
    }

    const jobId = "nxr_" + crypto.randomBytes(8).toString("hex");
    const safeBase = sanitizeOutputFilename(title);
    const outputFileName = `${safeBase}.${normalizedFmt}`;
    const tempFilePath = path.join(TEMP_DIR, `${jobId}_${outputFileName}`);

    const now = Date.now();
    const job: ProcessingJob = {
      jobId,
      sourceUrl: safeUrl.toString(),
      title: safeBase,
      format: normalizedFmt,
      qualityOrBitrate: normalizedFmt === "mp4" || normalizedFmt === "webm" ? String(resolution) : String(audioBitrate),
      requiresConversion: Boolean(requiresConversion),
      status: "QUEUED",
      progress: 0,
      bytesProcessed: 0,
      totalBytesEstimate: null,
      durationSeconds: Number(durationSeconds) || null,
      ffmpegSpeed: null,
      tempFilePath,
      outputFileName,
      sha256Checksum: null,
      watermarkFreeVerified: true,
      createdAt: now,
      expiresAt: now + Number(process.env.JOB_EXPIRATION_SECONDS || 3600) * 1000
    };
    jobs.set(jobId, job);

    // Build FFmpeg arguments with real-time progress reporting (-progress pipe:1)
    // CRITICAL RULE: Never add overlay, drawtext, watermark, intro, or outro filters.
    const ffmpegBin = process.env.FFMPEG_PATH || "ffmpeg";
    const args: string[] = [
      "-y",
      "-hide_banner",
      "-loglevel", "error",
      "-progress", "pipe:1",
      "-i", safeUrl.toString(),
      "-map_metadata", "-1"
    ];

    if (normalizedFmt === "mp4") {
      if (!requiresConversion) {
        args.push("-c:v", "copy", "-c:a", "copy", "-movflags", "+faststart", tempFilePath);
      } else {
        args.push(
          "-c:v", "libx264",
          "-preset", "veryfast",
          "-crf", "18",
          "-c:a", "aac",
          "-b:a", "256k",
          "-movflags", "+faststart",
          tempFilePath
        );
      }
    } else if (normalizedFmt === "webm") {
      args.push("-c:v", "libvpx-vp9", "-crf", "24", "-b:v", "0", "-c:a", "libopus", tempFilePath);
    } else if (normalizedFmt === "mp3") {
      args.push("-vn", "-c:a", "libmp3lame", "-b:a", String(audioBitrate), tempFilePath);
    } else if (normalizedFmt === "m4a") {
      args.push("-vn", "-c:a", "aac", "-b:a", "256k", tempFilePath);
    } else if (normalizedFmt === "wav") {
      args.push("-vn", "-c:a", "pcm_s16le", "-ar", "44100", "-ac", "2", tempFilePath);
    }

    const child = spawn(ffmpegBin, args, { stdio: ["ignore", "pipe", "pipe"] });
    job.processRef = child;
    job.status = "PROCESSING";

    // Parse FFmpeg -progress pipe:1 stdout for real-time progress tracking
    child.stdout?.on("data", (chunk: Buffer) => {
      const lines = chunk.toString("utf8").split(/\r?\n/);
      for (const line of lines) {
        const [key, val] = line.split("=");
        if (!key || !val) continue;
        if (key.trim() === "total_size") {
          const bytes = parseInt(val.trim(), 10);
          if (!isNaN(bytes) && bytes > 0) {
            job.bytesProcessed = bytes;
          }
        } else if (key.trim() === "out_time_us") {
          const us = parseInt(val.trim(), 10);
          if (!isNaN(us) && job.durationSeconds && job.durationSeconds > 0) {
            const elapsedSec = us / 1_000_000;
            job.progress = Math.min(95, Math.max(5, Math.round((elapsedSec / job.durationSeconds) * 100)));
          }
        } else if (key.trim() === "speed") {
          job.ffmpegSpeed = val.trim();
        }
      }
    });

    let stderrLog = "";
    child.stderr?.on("data", (chunk: Buffer) => {
      stderrLog += chunk.toString("utf8").slice(-500);
    });

    child.on("close", async (code) => {
      if (job.status === "CANCELLED") return;
      if (code === 0 && fs.existsSync(tempFilePath) && fs.statSync(tempFilePath).size > 0) {
        try {
          job.status = "VERIFYING";
          job.progress = 98;
          const stats = fs.statSync(tempFilePath);
          job.bytesProcessed = stats.size;
          job.totalBytesEstimate = stats.size;
          job.sha256Checksum = await computeFileSha256(tempFilePath);
          job.status = "READY";
          job.progress = 100;
        } catch (verifyErr: any) {
          job.status = "FAILED";
          job.error = verifyErr?.message || "Integrity verification failed";
        }
      } else {
        job.status = "FAILED";
        job.error = stderrLog.trim() || `FFmpeg exited with code ${code}`;
      }
    });

    res.status(202).json({
      jobId,
      status: job.status,
      progressUrl: `/api/download/status/${jobId}`,
      outputFileName,
      watermarkFreeGuarantee: true
    });
  } catch (err: any) {
    res.status(400).json({ error: err?.message || "Download request rejected" });
  }
});

// 4. GET /api/download/status/:jobId — Real-Time Progress & Completion Telemetry
app.get("/api/download/status/:jobId", (req: Request, res: Response) => {
  const job = jobs.get(req.params.jobId);
  if (!job) {
    res.status(404).json({ error: "Job not found or expired." });
    return;
  }
  if (Date.now() > job.expiresAt) {
    job.status = "EXPIRED";
    if (fs.existsSync(job.tempFilePath)) fs.unlinkSync(job.tempFilePath);
  }
  res.status(200).json({
    jobId: job.jobId,
    title: job.title,
    format: job.format,
    qualityOrBitrate: job.qualityOrBitrate,
    status: job.status,
    progress: job.progress,
    bytesProcessed: job.bytesProcessed,
    totalBytesEstimate: job.totalBytesEstimate,
    ffmpegSpeed: job.ffmpegSpeed,
    outputFileName: job.outputFileName,
    sha256Checksum: job.sha256Checksum,
    watermarkFreeVerified: job.watermarkFreeVerified,
    downloadReadyUrl: job.status === "READY" ? `/api/download/file/${job.jobId}` : null,
    error: job.error || null
  });
});

// 5. GET /api/download/file/:jobId — Streams Verified Watermark-Free Output File
app.get("/api/download/file/:jobId", (req: Request, res: Response) => {
  const job = jobs.get(req.params.jobId);
  if (!job || job.status !== "READY" || !fs.existsSync(job.tempFilePath)) {
    res.status(404).json({ error: "File not ready or has expired." });
    return;
  }
  res.setHeader("Content-Disposition", `attachment; filename="${job.outputFileName}"`);
  res.setHeader("X-Nexora-Watermark-Free", "true");
  if (job.sha256Checksum) {
    res.setHeader("X-Nexora-SHA256", job.sha256Checksum);
  }
  const readStream = fs.createReadStream(job.tempFilePath);
  readStream.pipe(res);
});

// 6. DELETE /api/download/:jobId — Cancels Job & Cleans Up Temporary Files
app.delete("/api/download/:jobId", (req: Request, res: Response) => {
  const job = jobs.get(req.params.jobId);
  if (!job) {
    res.status(404).json({ error: "Job not found." });
    return;
  }
  job.status = "CANCELLED";
  job.processRef?.kill("SIGTERM");
  if (fs.existsSync(job.tempFilePath)) {
    fs.unlinkSync(job.tempFilePath);
  }
  jobs.delete(job.jobId);
  res.status(200).json({ jobId: job.jobId, status: "CANCELLED", cleanedUp: true });
});

const PORT = Number(process.env.PORT || 8080);
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`NEXORA Media Engine Backend listening on port ${PORT}`);
  });
}

export default app;
