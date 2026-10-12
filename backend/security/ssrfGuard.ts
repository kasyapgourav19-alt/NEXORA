import { URL } from "url";
import * as dns from "dns/promises";
import * as net from "net";

const BLOCKED_HOSTNAMES = new Set([
  "localhost",
  "localhost.localdomain",
  "0.0.0.0",
  "127.0.0.1",
  "::1",
  "169.254.169.254",
  "metadata.google.internal",
  "metadata"
]);

export function isPrivateOrReservedIp(ip: string): boolean {
  if (net.isIPv4(ip)) {
    const parts = ip.split(".").map((p) => parseInt(p, 10));
    const [o1, o2] = parts;
    if (o1 === 0 || o1 === 10 || o1 === 127) return true;
    if (o1 === 169 && o2 === 254) return true;
    if (o1 === 172 && o2 >= 16 && o2 <= 31) return true;
    if (o1 === 192 && o2 === 168) return true;
    if (o1 === 100 && o2 >= 64 && o2 <= 127) return true;
    if (o1 >= 224) return true;
    return false;
  }
  if (net.isIPv6(ip)) {
    const normalized = ip.toLowerCase();
    if (normalized === "::1" || normalized.startsWith("fe80:") || normalized.startsWith("fc") || normalized.startsWith("fd")) {
      return true;
    }
  }
  return false;
}

export async function validateSafeHttpsUrl(rawUrl: string): Promise<URL> {
  if (!rawUrl || typeof rawUrl !== "string" || rawUrl.length > 2048) {
    throw new Error("Invalid or oversized URL.");
  }
  const parsed = new URL(rawUrl.trim());
  if (parsed.protocol !== "https:") {
    throw new Error("Only encrypted https:// URLs are permitted.");
  }
  if (parsed.username || parsed.password) {
    throw new Error("Embedded credentials in URLs are blocked.");
  }
  const host = parsed.hostname.toLowerCase();
  if (BLOCKED_HOSTNAMES.has(host) || host.endsWith(".local") || host.endsWith(".internal") || !host.includes(".")) {
    throw new Error(`SSRF Guard blocked restricted host: ${host}`);
  }
  if (net.isIP(host) && isPrivateOrReservedIp(host)) {
    throw new Error(`SSRF Guard blocked private/reserved IP literal: ${host}`);
  }
  const records = await dns.lookup(host, { all: true });
  for (const rec of records) {
    if (isPrivateOrReservedIp(rec.address)) {
      throw new Error(`SSRF Guard blocked DNS resolution to private IP (${rec.address}).`);
    }
  }
  return parsed;
}

export function sanitizeOutputFilename(rawName: string): string {
  const cleaned = rawName
    .replace(/[\\/:*?"<>|\x00-\x1F]/g, "_")
    .replace(/\.\./g, "_")
    .replace(/\s+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^[_.\s]+|[_.\s]+$/g, "")
    .slice(0, 72);
  return cleaned || "nexora_clean_export";
}
