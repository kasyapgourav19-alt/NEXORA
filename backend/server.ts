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

interface ProcessingJob {
  jobId: string;
  sourceUrl: string;
  title: string;
  format: string;
  status: "QUEUED" | "PROCESSING" | "READY" | "CANCELLED" | "FAILED" | "EXPIRED";
  progress: number;
  tempFilePath: string;
  outputFileName: string;
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

    // Perform HEAD request to inspect direct media headers
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

// 3. POST /api/download
app.post("/api/download", async (req: Request, res: Response) => {
  try {
    const { url, format = "mp4", title = "media_export", audioBitrate = "320k" } = req.body;
    const safeUrl = await validateSafeHttpsUrl(url);
    const allowedFormats = new Set(["mp4", "webm", "mp3", "m4a", "wav"]);
    const normalizedFmt = String(format).toLowerCase();
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
      status: "PROCESSING",
      progress: 10,
      tempFilePath,
      outputFileName,
      createdAt: now,
      expiresAt: now + Number(process.env.JOB_EXPIRATION_SECONDS || 3600) * 1000
    };
    jobs.set(jobId, job);

    // Spawn quality-preserving FFmpeg pipeline with ZERO watermark filters
    const ffmpegBin = process.env.FFMPEG_PATH || "ffmpeg";
    const args: string[] = ["-y", "-i", safeUrl.toString()];
    if (normalizedFmt === "mp4") {
      args.push("-c:v", "copy", "-c:a", "copy", "-movflags", "+faststart", tempFilePath);
    } else if (normalizedFmt === "mp3") {
      args.push("-vn", "-c:a", "libmp3lame", "-b:a", String(audioBitrate), tempFilePath);
    } else if (normalizedFmt === "m4a") {
      args.push("-vn", "-c:a", "copy", tempFilePath);
    } else if (normalizedFmt === "wav") {
      args.push("-vn", "-c:a", "pcm_s16le", "-ar", "44100", "-ac", "2", tempFilePath);
    } else {
      args.push("-c", "copy", tempFilePath);
    }

    const child = spawn(ffmpegBin, args, { stdio: "ignore" });
    job.processRef = child;

    child.on("close", (code) => {
      if (job.status === "CANCELLED") return;
      if (code === 0 && fs.existsSync(tempFilePath) && fs.statSync(tempFilePath).size > 0) {
        job.status = "READY";
        job.progress = 100;
      } else {
        job.status = "FAILED";
        job.error = `Media pipeline exited with status ${code}`;
      }
    });

    res.status(202).json({
      jobId,
      status: job.status,
      outputFileName
    });
  } catch (err: any) {
    res.status(400).json({ error: err?.message || "Download request rejected" });
  }
});

// 4. GET /api/download/status/:jobId
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
    status: job.status,
    progress: job.progress,
    outputFileName: job.outputFileName,
    error: job.error || null
  });
});

// 5. DELETE /api/download/:jobId
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
