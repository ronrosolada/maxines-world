package com.maxinesworld.corenetwork

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var cacheDir: File
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    /** Deterministic fake APK payload. */
    private val apkBytes = ByteArray(4096) { (it % 251).toByte() }
    private val expectedSha256 = MessageDigest.getInstance("SHA-256").digest(apkBytes)
        .joinToString("") { "%02x".format(it) }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cacheDir = createTempDirectory()
        packageManager = mockk()
        context = mockk(relaxed = true)
        every { context.externalCacheDir } returns cacheDir
        every { context.cacheDir } returns cacheDir
    }

    @After
    fun tearDown() {
        server.shutdown()
        cacheDir.deleteRecursively()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────────

    private fun createTempDirectory(): File =
        File.createTempFile("update-test", "").let { marker ->
            marker.delete()
            marker.mkdirs().let { marker }
        }

    private fun installPackageManager(installedVersionCode: Int, candidateVersionCode: Int?) {
        every { context.packageManager } returns packageManager
        every {
            packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns PackageInfo().apply { versionCode = installedVersionCode }
        val candidate = candidateVersionCode?.let { PackageInfo().apply { versionCode = it } }
        every { packageManager.getPackageArchiveInfo(any<String>(), any<Int>()) } returns candidate
    }

    private fun serveApk(sidecarBodyProvider: () -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == APK_PATH -> MockResponse().setBody(Buffer().write(apkBytes))
                request.path == "$APK_PATH.sha256" -> sidecarBodyProvider()
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun manager() = AppUpdateManager(context)

    private fun apkUrl() = server.url(APK_PATH).toString()

    // ─── Tests ────────────────────────────────────────────────────────────────────────

    @Test
    fun `verified download with matching sidecar checksum and newer version is ready to install`() = runTest {
        serveApk { MockResponse().setBody("$expectedSha256  app-release.apk\n") }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        val result = manager().downloadVerifiedApk(listOf(apkUrl()))

        assertTrue(result is AppUpdateResult.ReadyToInstall)
        val file = (result as AppUpdateResult.ReadyToInstall).apkFile
        assertEquals(apkBytes.size.toLong(), file.length())
        assertEquals(expectedSha256, AppUpdateManager.sha256Hex(file))
    }

    @Test
    fun `pinned checksum overrides an unavailable or wrong sidecar`() = runTest {
        serveApk { MockResponse().setBody("deadbeef  tampered.txt") }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        val result = manager().downloadVerifiedApk(
            candidateEndpoints = listOf(apkUrl()),
            pinnedSha256Hex = expectedSha256,
        )

        assertTrue(result is AppUpdateResult.ReadyToInstall)
    }

    @Test
    fun `mismatching checksum is rejected and the downloaded file deleted`() = runTest {
        serveApk { MockResponse().setBody("${"ab".repeat(32)}  app-release.apk") }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        val result = manager().downloadVerifiedApk(listOf(apkUrl()))

        assertTrue(result is AppUpdateResult.Rejected)
        assertTrue((result as AppUpdateResult.Rejected).reason.contains("SHA-256 mismatch"))
        assertFalse(File(cacheDir, "maxines_world_update.apk").exists())
    }

    @Test
    fun `no checksum available is rejected (fail-closed against MITM)`() = runTest {
        serveApk { MockResponse().setResponseCode(404) }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        val result = manager().downloadVerifiedApk(listOf(apkUrl()))

        assertTrue(result is AppUpdateResult.Rejected)
        assertTrue((result as AppUpdateResult.Rejected).reason.contains("no checksum"))
        assertFalse(File(cacheDir, "maxines_world_update.apk").exists())
    }

    @Test
    fun `candidate that is not newer than the installed build is rejected`() = runTest {
        serveApk { MockResponse().setBody("$expectedSha256  app-release.apk\n") }
        installPackageManager(installedVersionCode = 200, candidateVersionCode = 200)

        val result = manager().downloadVerifiedApk(listOf(apkUrl()))

        assertTrue(result is AppUpdateResult.Rejected)
        assertTrue((result as AppUpdateResult.Rejected).reason.contains("not newer"))
    }

    @Test
    fun `unreadable candidate APK metadata is rejected`() = runTest {
        serveApk { MockResponse().setBody("$expectedSha256  app-release.apk\n") }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = null)

        val result = manager().downloadVerifiedApk(listOf(apkUrl()))

        assertTrue(result is AppUpdateResult.Rejected)
        assertTrue((result as AppUpdateResult.Rejected).reason.contains("could not be read"))
    }

    @Test
    fun `download falls back to later endpoints after earlier failures`() = runTest {
        serveApk { MockResponse().setBody("$expectedSha256  app-release.apk\n") }
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        // Port 1 is reserved and unreachable, forcing the first endpoint to fail.
        val deadEndpoint = "http://127.0.0.1:1$APK_PATH"
        val result = manager().downloadVerifiedApk(listOf(deadEndpoint, apkUrl()))

        assertTrue(result is AppUpdateResult.ReadyToInstall)
    }

    @Test
    fun `all endpoints failing yields Failed`() = runTest {
        installPackageManager(installedVersionCode = 100, candidateVersionCode = 101)

        val deadEndpoint = "http://127.0.0.1:1$APK_PATH"
        val result = manager().downloadVerifiedApk(listOf(deadEndpoint))

        assertTrue(result is AppUpdateResult.Failed)
        assertFalse(File(cacheDir, "maxines_world_update.apk").exists())
    }

    companion object {
        private const val APK_PATH = "/app-release.apk"
    }
}
