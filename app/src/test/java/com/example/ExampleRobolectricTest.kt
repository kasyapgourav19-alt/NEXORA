package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.media.WatermarkFreeMediaEngine
import com.example.domain.model.MediaTab
import com.example.domain.provider.AuthorizedOpenMediaAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app_name and creator branding resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("NEXORA", context.getString(R.string.app_name))
        assertEquals("Your Media. Your Quality.", context.getString(R.string.tagline))
        assertEquals("Designed & Built by Gaurav", context.getString(R.string.creator_branding))
        assertEquals("@avkaxofficial", context.getString(R.string.creator_ig_1))
        assertEquals("@gxr.avka", context.getString(R.string.creator_ig_2))
    }

    @Test
    fun `verify watermark-free WAV audio export produces valid RIFF WAVE file with SHA-256`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = OkHttpClient()
        val engine = WatermarkFreeMediaEngine(context, client)
        val adapter = AuthorizedOpenMediaAdapter()

        val analysis = adapter.analyze(
            normalizedUrl = "https://nexora.studio/authorized-samples/cybernetic-pulse-lossless-audio.wav",
            host = "nexora.studio",
            path = "/authorized-samples/cybernetic-pulse-lossless-audio.wav",
            okHttpClient = client
        )
        val wavOption = analysis.audioFormats.first { it.containerFormat == "WAV" }

        val exportResult = engine.executeWatermarkFreeDownload(
            jobId = "nxr_test_wav",
            analysis = analysis,
            mediaTab = MediaTab.AUDIO,
            selectedVideo = null,
            selectedAudio = wavOption,
            onProgress = { _, _, _, _ -> }
        )

        assertTrue(exportResult.isSuccess)
        val exported = exportResult.getOrThrow()
        assertTrue(exported.file.exists())
        assertTrue(exported.sizeBytes > 44L)
        assertEquals(64, exported.sha256Hex.length)
        // Verify filename has no NEXORA branding injected
        assertFalse(exported.fileName.contains("watermark", ignoreCase = true))

        // Verify RIFF WAVE header signature
        val headerBytes = ByteArray(12)
        exported.file.inputStream().use { it.read(headerBytes) }
        assertEquals("RIFF", String(headerBytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(headerBytes, 8, 4, Charsets.US_ASCII))

        exported.file.delete()
    }
}
