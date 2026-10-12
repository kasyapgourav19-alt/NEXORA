package com.example

import com.example.data.preferences.NexoraSettings
import com.example.domain.model.AnalysisStage
import com.example.domain.model.MediaProviderType
import com.example.domain.provider.NexoraAnalysisPipeline
import com.example.domain.security.UrlSecurityValidator
import com.example.domain.security.UrlValidationResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `validateMediaUrl allows valid public HTTPS URLs`() {
        val result = UrlSecurityValidator.validateMediaUrl("https://nexora.studio/authorized-samples/orbital-ring-4k-master.mp4")
        assertTrue(result is UrlValidationResult.Valid)
        val valid = result as UrlValidationResult.Valid
        assertEquals("nexora.studio", valid.host)
    }

    @Test
    fun `validateMediaUrl blocks insecure HTTP and SSRF targets`() {
        // HTTP blocked
        val httpRes = UrlSecurityValidator.validateMediaUrl("http://example.com/video.mp4")
        assertTrue(httpRes is UrlValidationResult.Invalid)

        // Localhost blocked
        val localhostRes = UrlSecurityValidator.validateMediaUrl("https://localhost:8080/api/secret")
        assertTrue(localhostRes is UrlValidationResult.Invalid)

        // Private RFC1918 IP blocked
        val privateIpRes = UrlSecurityValidator.validateMediaUrl("https://192.168.1.50/stream.mp4")
        assertTrue(privateIpRes is UrlValidationResult.Invalid)

        // Cloud metadata endpoint 169.254.169.254 blocked
        val metadataRes = UrlSecurityValidator.validateMediaUrl("https://169.254.169.254/latest/meta-data/")
        assertTrue(metadataRes is UrlValidationResult.Invalid)

        // Path traversal blocked
        val traversalRes = UrlSecurityValidator.validateMediaUrl("https://example.com/media/../../etc/passwd")
        assertTrue(traversalRes is UrlValidationResult.Invalid)
    }

    @Test
    fun `sanitizeFilename strips path traversal and never adds NEXORA watermark`() {
        val raw = "../../My Video: Master *4K*?.mp4"
        val clean = UrlSecurityValidator.sanitizeFilename(raw)
        assertFalse(clean.contains(".."))
        assertFalse(clean.contains("/"))
        assertFalse(clean.contains("NEXORA", ignoreCase = true))
        assertTrue(clean.isNotBlank())
    }

    @Test
    fun `analysis pipeline resolves authorized open media and applies Smart Quality and Data Saver`() = runTest {
        val pipeline = NexoraAnalysisPipeline(OkHttpClient())
        val stages = mutableListOf<AnalysisStage>()

        val result = pipeline.analyzeUrl(
            rawUrl = "https://nexora.studio/authorized-samples/orbital-ring-4k-master.mp4",
            settings = NexoraSettings(preferredVideoQuality = "1080p", dataSaverEnabled = false),
            isNetworkMetered = false,
            onStageChange = { stages.add(it) }
        )

        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals(MediaProviderType.AUTHORIZED_OPEN_MEDIA, analysis.providerType)
        assertTrue(analysis.videoFormats.isNotEmpty())
        assertTrue(analysis.audioFormats.isNotEmpty())
        assertTrue(stages.contains(AnalysisStage.VALIDATING_LINK))
        assertTrue(stages.contains(AnalysisStage.COMPLETED))

        // Verify 1080p is Smart Recommended when Data Saver is off
        val smartPick = analysis.videoFormats.firstOrNull { it.isSmartRecommended }
        assertNotNull(smartPick)
        assertEquals(1080, smartPick?.heightPx)

        // Verify Data Saver switches recommendation to 480p/360p
        val dataSaverAnalysis = pipeline.applySmartQualityRecommendations(
            analysis = analysis,
            settings = NexoraSettings(dataSaverEnabled = true),
            isNetworkMetered = true
        )
        val lowDataPick = dataSaverAnalysis.videoFormats.firstOrNull { it.isSmartRecommended }
        assertNotNull(lowDataPick)
        assertTrue((lowDataPick?.heightPx ?: 9999) <= 480)
    }

    @Test
    fun `analysis pipeline reports unsupported domain without inventing fake formats`() = runTest {
        val pipeline = NexoraAnalysisPipeline(OkHttpClient())
        val result = pipeline.analyzeUrl(
            rawUrl = "https://unknown-social-example.org/post/12345",
            settings = NexoraSettings(),
            isNetworkMetered = false,
            onStageChange = {}
        )
        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals(MediaProviderType.UNSUPPORTED, analysis.providerType)
        assertTrue(analysis.videoFormats.isEmpty())
        assertTrue(analysis.audioFormats.isEmpty())
        assertNotNull(analysis.limitationExplanation)
    }
}
