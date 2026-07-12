package com.maxinesworld.featureparent

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.coremodel.ContentCatalog
import com.maxinesworld.enginesync.ContentSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

// ─── Data classes ───

data class ParentContentState(
    val serverUrl: String = ContentSyncWorker.DEFAULT_CATALOG_URL,
    val channel: String = "production",
    val catalogVersion: Int? = null,
    val catalogGeneratedAt: String? = null,
    val connectionStatus: String? = null,
    val isTesting: Boolean = false,
    val lastSyncTimestamp: String? = null,
    val storageUsedBytes: Long = 0L,
    val packages: List<RemotePackageInfo> = emptyList(),
    val unvalidatedCount: Int = 0,
    val isSyncing: Boolean = false
)

data class RemotePackageInfo(
    val packageId: String,
    val version: Int,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val minimumAppVersion: Int,
    val educatorValidated: Boolean,
    val releaseStatus: String,
    val isInstalled: Boolean = false
)

// ─── ViewModel ───

@HiltViewModel
class ParentContentViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ParentContentState())
    val state: StateFlow<ParentContentState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    init {
        refreshStorageInfo()
        loadSavedSettings()
    }

    private fun loadSavedSettings() {
        val prefs = appContext.getSharedPreferences("content_mgmt", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("server_url", ContentSyncWorker.DEFAULT_CATALOG_URL)
            ?: ContentSyncWorker.DEFAULT_CATALOG_URL
        val savedChannel = prefs.getString("channel", "production") ?: "production"
        val savedTimestamp = prefs.getString("last_sync", null)
        _state.update { it.copy(serverUrl = savedUrl, channel = savedChannel, lastSyncTimestamp = savedTimestamp) }
    }

    fun onServerUrlChange(url: String) {
        _state.update { it.copy(serverUrl = url) }
        appContext.getSharedPreferences("content_mgmt", Context.MODE_PRIVATE)
            .edit().putString("server_url", url).apply()
    }

    fun onChannelChange(channel: String) {
        _state.update { it.copy(channel = channel) }
        appContext.getSharedPreferences("content_mgmt", Context.MODE_PRIVATE)
            .edit().putString("channel", channel).apply()
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, connectionStatus = null) }
            try {
                val url = _state.value.serverUrl
                val catalogJson = withContext(Dispatchers.IO) { fetchUrl(url) }
                val catalog = json.decodeFromString<ContentCatalog>(catalogJson)
                val pkgInfos = catalog.packages.map { pkg ->
                    val installedDir = File(appContext.filesDir, "content/active/${pkg.packageId}/${pkg.version}")
                    RemotePackageInfo(
                        packageId = pkg.packageId,
                        version = pkg.version,
                        url = pkg.url,
                        sha256 = pkg.sha256,
                        sizeBytes = pkg.sizeBytes,
                        minimumAppVersion = pkg.minimumAppVersion,
                        educatorValidated = pkg.educatorValidated,
                        releaseStatus = pkg.releaseStatus,
                        isInstalled = installedDir.exists() && installedDir.isDirectory
                    )
                }
                val unvalidated = pkgInfos.count { !it.educatorValidated }
                _state.update {
                    it.copy(
                        catalogVersion = catalog.catalogVersion,
                        catalogGeneratedAt = catalog.generatedAt,
                        connectionStatus = "Connected ✓",
                        packages = pkgInfos,
                        unvalidatedCount = unvalidated,
                        isTesting = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        connectionStatus = "Failed: ${e.message}",
                        isTesting = false
                    )
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            ContentSyncWorker.enqueue(appContext, _state.value.serverUrl)
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            appContext.getSharedPreferences("content_mgmt", Context.MODE_PRIVATE)
                .edit().putString("last_sync", timestamp).apply()
            _state.update { it.copy(lastSyncTimestamp = timestamp, isSyncing = false) }
        }
    }

    fun rollback(packageId: String, currentVersion: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val activeBase = File(appContext.filesDir, "content/active/$packageId")
                val currentDir = File(activeBase, currentVersion.toString())
                val prevVersion = currentVersion - 1
                val prevDir = File(activeBase, prevVersion.toString())
                if (prevDir.exists() && prevDir.isDirectory) {
                    currentDir.deleteRecursively()
                }
            }
            refreshStorageInfo()
            testConnection() // Reload package data
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                val activeDir = File(appContext.filesDir, "content/active")
                if (activeDir.exists()) getDirSize(activeDir) else 0L
            }
            _state.update { it.copy(storageUsedBytes = size) }
        }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun fetchUrl(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }
}

// ─── Composable ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentContentScreen(
    childId: String,
    onBack: () -> Unit,
    viewModel: ParentContentViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Content Management", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VillageTeal,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Server URL ──
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Server URL", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.serverUrl,
                            onValueChange = viewModel::onServerUrlChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        Button(
                            onClick = viewModel::testConnection,
                            enabled = !state.isTesting,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal)
                        ) {
                            if (state.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test Connection", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // ── Connection Status ──
            state.connectionStatus?.let { status ->
                val isSuccess = status.startsWith("Connected")
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) SuccessGreen.copy(alpha = 0.08f) else Coral.copy(alpha = 0.08f)
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isSuccess) SuccessGreen else Coral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(status, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                    }
                }
            }

            // ── Catalog Info ──
            if (state.catalogVersion != null) {
                Card(colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.06f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InfoChip("Version", "v${state.catalogVersion}")
                        InfoChip("Generated", state.catalogGeneratedAt?.take(10) ?: "—")
                        InfoChip("Packages", "${state.packages.size}")
                    }
                }
            }

            // ── Channel Selector ──
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Channel", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = state.channel == "production",
                            onClick = { viewModel.onChannelChange("production") },
                            label = { Text("Production") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SuccessGreen.copy(alpha = 0.15f),
                                selectedLabelColor = SuccessGreen
                            )
                        )
                        FilterChip(
                            selected = state.channel == "preview",
                            onClick = { viewModel.onChannelChange("preview") },
                            label = { Text("Preview") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SunshineGold.copy(alpha = 0.15f),
                                selectedLabelColor = SunshineGold.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }

            // ── Sync Controls ──
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Last Sync",
                                fontSize = 13.sp,
                                color = Ink.copy(alpha = 0.6f)
                            )
                            Text(
                                state.lastSyncTimestamp ?: "Never",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (state.lastSyncTimestamp != null) Ink else Ink.copy(alpha = 0.4f)
                            )
                        }
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = !state.isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }

            // ── Storage Usage ──
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Storage Usage", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    val sizeMb = state.storageUsedBytes / (1024.0 * 1024.0)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Installed packages",
                            fontSize = 14.sp,
                            color = Ink.copy(alpha = 0.7f)
                        )
                        Text(
                            "%.1f MB".format(sizeMb),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sizeMb > 500) Coral else VillageTeal
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ((sizeMb / 1024.0).coerceIn(0.0, 1.0)).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (sizeMb > 500) Coral else VillageTeal,
                        trackColor = VillageTeal.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "of ~1 GB device storage",
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Unvalidated Content Warning ──
            if (state.unvalidatedCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.08f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Unvalidated Content",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Warning
                            )
                            Text(
                                "${state.unvalidatedCount} package(s) have not been educator-validated. Review before assigning to learners.",
                                fontSize = 13.sp,
                                color = Ink.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ── Package List ──
            if (state.packages.isNotEmpty()) {
                Text(
                    "Packages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Ink
                )

                state.packages.forEach { pkg ->
                    PackageCard(
                        pkg = pkg,
                        onRollback = {
                            viewModel.rollback(pkg.packageId, pkg.version)
                        }
                    )
                }
            } else if (state.catalogVersion != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(
                        "No packages found in catalog.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        color = Ink.copy(alpha = 0.5f)
                    )
                }
            } else if (!state.isTesting) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(
                        "Test the connection above to load package information.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        color = Ink.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Sub-components ───

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VillageTeal)
    }
}

@Composable
private fun PackageCard(pkg: RemotePackageInfo, onRollback: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (pkg.isInstalled) Color.White else Color.White.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (pkg.isInstalled) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (pkg.isInstalled) SuccessGreen else Ink.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            pkg.packageId,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "v${pkg.version}",
                            fontSize = 13.sp,
                            color = Ink.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!pkg.educatorValidated) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text("Unvalidated", fontSize = 11.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = Warning)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Warning.copy(alpha = 0.1f),
                                labelColor = Warning
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PackageStat("Size", formatBytes(pkg.sizeBytes))
                        PackageStat("Status", pkg.releaseStatus.replaceFirstChar { it.uppercase() })
                        PackageStat("Min App", "v${pkg.minimumAppVersion}")
                    }
                }

                if (pkg.isInstalled && pkg.version > 1) {
                    OutlinedButton(
                        onClick = onRollback,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                    ) {
                        Icon(Icons.Default.History, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rollback", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageStat(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Ink.copy(alpha = 0.4f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink.copy(alpha = 0.7f))
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
