package com.example.domain.security

import java.net.URI
import java.security.SecureRandom
import java.util.Locale

sealed class UrlValidationResult {
    data class Valid(
        val normalizedUrl: String,
        val host: String,
        val path: String
    ) : UrlValidationResult()

    data class Invalid(
        val reasonTitle: String,
        val explanation: String,
        val suggestedAction: String
    ) : UrlValidationResult()
}

object UrlSecurityValidator {

    private val BLOCKED_EXACT_HOSTS = setOf(
        "localhost",
        "localhost.localdomain",
        "0.0.0.0",
        "127.0.0.1",
        "::1",
        "[::1]",
        "169.254.169.254",
        "metadata.google.internal",
        "metadata",
        "instance-data"
    )

    private val BLOCKED_HOST_SUFFIXES = listOf(
        ".localhost",
        ".local",
        ".internal",
        ".intranet",
        ".corp",
        ".home.arpa"
    )

    private val URL_EXTRACTION_REGEX = Regex("""(https?://[^\s"'<>]+)""", RegexOption.IGNORE_CASE)

    private val secureRandom = SecureRandom()

    /**
     * Validates a user-submitted media URL against malformed syntax, non-HTTPS protocols,
     * and Server-Side Request Forgery (SSRF) targets including localhost, RFC1918 private IPs,
     * link-local addresses, and cloud metadata endpoints.
     * Also smartly extracts URLs from social share text and auto-prefixes https:// when scheme is omitted.
     */
    fun validateMediaUrl(rawInput: String, allowNexoraSampleScheme: Boolean = true): UrlValidationResult {
        val initialTrim = rawInput.trim()
        if (initialTrim.isEmpty()) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Empty Media Link",
                explanation = "Please enter or paste an HTTPS media link to analyze.",
                suggestedAction = "Paste a valid https:// video or audio URL, or select an Authorized Open Media sample below."
            )
        }

        // Extract URL if user pasted multi-word share text (e.g., "Watch this video https://youtu.be/xyz")
        val extracted = URL_EXTRACTION_REGEX.find(initialTrim)?.value ?: initialTrim

        // Auto-prepend https:// if user typed a bare domain like "youtube.com/watch?v=..." or "instagram.com/reel/..."
        val candidate = if (!extracted.contains("://") && extracted.contains('.') && !extracted.contains(' ')) {
            "https://$extracted"
        } else {
            extracted
        }

        if (candidate.length > 2048) {
            return UrlValidationResult.Invalid(
                reasonTitle = "URL Exceeds Maximum Length",
                explanation = "The provided link exceeds the 2048-character security limit.",
                suggestedAction = "Provide a direct canonical HTTPS link without excessive tracking parameters."
            )
        }

        if (candidate.contains("..") || candidate.contains("\\")) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Path Traversal Blocked",
                explanation = "The URL path contains directory traversal sequences ('..' or '\\').",
                suggestedAction = "Provide a clean canonical URL path."
            )
        }

        val safeCandidate = candidate.replace(" ", "%20")

        val parsedUri = try {
            URI(safeCandidate)
        } catch (e: Exception) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Malformed URL Syntax",
                explanation = "The link contains invalid characters or malformed URI syntax.",
                suggestedAction = "Check the URL for typos or spaces and ensure it starts with https://."
            )
        }

        val scheme = parsedUri.scheme?.lowercase(Locale.US)
        if (scheme == null) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Missing HTTPS Protocol",
                explanation = "NEXORA requires an explicit https:// protocol scheme for secure transport.",
                suggestedAction = "Prefix your link with https:// and try again."
            )
        }

        if (scheme != "https") {
            return UrlValidationResult.Invalid(
                reasonTitle = "Insecure Protocol Blocked",
                explanation = "Only encrypted HTTPS sources are permitted by default (received '$scheme://').",
                suggestedAction = "Use an https:// URL to protect transport integrity."
            )
        }

        if (!parsedUri.userInfo.isNullOrEmpty()) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Embedded Credentials Blocked",
                explanation = "URLs containing embedded username/password credentials are rejected for security.",
                suggestedAction = "Remove user:password@ from the URL before analyzing."
            )
        }

        val rawHost = parsedUri.host?.lowercase(Locale.US)?.trim()
        if (rawHost.isNullOrEmpty()) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Missing Hostname",
                explanation = "The URL does not contain a valid domain or hostname.",
                suggestedAction = "Enter a full URL such as https://example.com/media/video.mp4."
            )
        }

        if (isSsrfRestrictedHost(rawHost)) {
            return UrlValidationResult.Invalid(
                reasonTitle = "SSRF Protection Triggered",
                explanation = "Access to localhost, private network ranges (RFC 1918), link-local addresses, and cloud metadata endpoints ($rawHost) is strictly blocked.",
                suggestedAction = "Submit only public, authorized HTTPS media hosts."
            )
        }

        if (!rawHost.contains('.')) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Unqualified Hostname Blocked",
                explanation = "Single-label internal hostnames ('$rawHost') are blocked to prevent internal network probing.",
                suggestedAction = "Use a fully qualified public domain name."
            )
        }

        val normalizedPath = if (parsedUri.path.isNullOrEmpty()) "/" else parsedUri.path
        if (normalizedPath.contains("..") || normalizedPath.contains("\\")) {
            return UrlValidationResult.Invalid(
                reasonTitle = "Path Traversal Blocked",
                explanation = "The URL path contains directory traversal sequences ('..' or '\\').",
                suggestedAction = "Provide a clean canonical URL path."
            )
        }

        return UrlValidationResult.Valid(
            normalizedUrl = parsedUri.toASCIIString(),
            host = rawHost,
            path = normalizedPath
        )
    }

    /**
     * Checks if a hostname or IP literal resolves to loopback, private network, link-local,
     * or cloud metadata ranges.
     */
    fun isSsrfRestrictedHost(host: String): Boolean {
        val cleanHost = host.removePrefix("[").removeSuffix("]").lowercase(Locale.US)
        if (cleanHost in BLOCKED_EXACT_HOSTS) return true
        if (BLOCKED_HOST_SUFFIXES.any { cleanHost.endsWith(it) }) return true

        if (cleanHost.contains(':')) {
            if (cleanHost == "::1" || cleanHost == "0:0:0:0:0:0:0:1") return true
            if (cleanHost.startsWith("fe80:") || cleanHost.startsWith("fc") || cleanHost.startsWith("fd")) {
                return true
            }
        }

        val ipv4Parts = cleanHost.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val o1 = ipv4Parts[0].toInt()
            val o2 = ipv4Parts[1].toInt()
            if (o1 == 0) return true
            if (o1 == 10) return true
            if (o1 == 100 && o2 in 64..127) return true
            if (o1 == 127) return true
            if (o1 == 169 && o2 == 254) return true
            if (o1 == 172 && o2 in 16..31) return true
            if (o1 == 192 && o2 == 168) return true
            if (o1 >= 224) return true
        }

        return false
    }

    /**
     * Sanitizes any media title into a safe, browser- and Android-compatible filename
     * without path traversal, control characters, or NEXORA branding/watermarks.
     */
    fun sanitizeFilename(rawTitle: String, fallback: String = "media_export"): String {
        val cleaned = rawTitle
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .replace("..", "_")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', '.', ' ')
            .take(72)
        return if (cleaned.isEmpty()) fallback else cleaned
    }

    fun generateSecureJobId(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "nxr_$hex"
    }
}
