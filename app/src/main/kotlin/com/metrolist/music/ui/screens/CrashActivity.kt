/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.IrideTheme
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.utils.CrashHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {
    private val recoveryPreferences by lazy { getSharedPreferences("crash_recovery", MODE_PRIVATE) }
    private var crashLog: String? = null
    private var reportCopied by mutableStateOf(false)
    private var autoRestart by mutableStateOf(false)
    private var copyAttempted = false
    private var restartJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        crashLog = intent.getStringExtra(CrashHandler.EXTRA_CRASH_LOG)?.takeIf { it.isNotBlank() }
        autoRestart =
            crashLog != null &&
                recordCrashAndShouldAutoRestart(
                    intent.getStringExtra(CrashHandler.EXTRA_CRASH_SIGNATURE),
                    intent.getStringExtra(CrashHandler.EXTRA_CRASH_ID),
                )
        setContent {
            IrideTheme {
                CrashScreen(
                    autoRestart = autoRestart,
                    reportAvailable = crashLog != null,
                    reportCopied = reportCopied,
                    onCopy = { reportCopied = copyCrashLog() },
                    onRestart = { restart() },
                    onShare = { crashLog?.let(::shareCrashLog) },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            restartJob?.cancel()
            return
        }
        if (!copyAttempted) {
            copyAttempted = true
            reportCopied = copyCrashLog()
        }
        if (autoRestart && restartJob?.isActive != true) {
            restartJob = lifecycleScope.launch {
                delay(RestartDelayMillis)
                if (hasWindowFocus() && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    restart()
                }
            }
        }
    }

    private fun recordCrashAndShouldAutoRestart(signature: String?, crashId: String?): Boolean {
        if (signature.isNullOrBlank() || crashId.isNullOrBlank()) return false

        val previousSignature = recoveryPreferences.getString(LastCrashSignature, null)
        val previousCrashId = recoveryPreferences.getString(LastCrashId, null)
        val previousCount = recoveryPreferences.getInt(SameCrashCount, 0)
        val crashCount =
            if (previousCrashId == crashId && previousSignature == signature) {
                previousCount
            } else if (previousSignature == signature) {
                (previousCount + 1).coerceAtMost(3)
            } else {
                1
            }
        val recorded =
            recoveryPreferences.edit()
                .putString(LastCrashSignature, signature)
                .putString(LastCrashId, crashId)
                .putInt(SameCrashCount, crashCount)
                .commit()
        return recorded && crashCount != 2
    }

    private fun restart() {
        if (isFinishing || isDestroyed) return
        try {
            startActivity(Intent.makeRestartActivityTask(ComponentName(this, MainActivity::class.java)))
            finish()
        } catch (_: RuntimeException) {
            autoRestart = false
        }
    }

    private fun copyCrashLog(): Boolean {
        val report = crashLog ?: return false
        return try {
            val clipboard = getSystemService(ClipboardManager::class.java) ?: return false
            val clip = ClipData.newPlainText(getString(R.string.crash_report_subject), report)
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun shareCrashLog(crashLog: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "iride_crash_$timestamp.txt"
            val reportDirectory = File(cacheDir, "crashes").apply { mkdirs() }
            val crashFile = File(reportDirectory, fileName)
            crashFile.writeText(crashLog)
            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.FileProvider",
                    crashFile,
                )
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_subject))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.crash_share_title)))
        } catch (e: Exception) {
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, crashLog)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_subject))
                }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.crash_share_title)))
        }
    }

    private companion object {
        const val LastCrashSignature = "last_crash_signature"
        const val LastCrashId = "last_crash_id"
        const val SameCrashCount = "same_crash_count"
        const val RestartDelayMillis = 1_600L
    }
}

@Composable
private fun CrashScreen(
    autoRestart: Boolean,
    reportAvailable: Boolean,
    reportCopied: Boolean,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(IrideFolio.DialogOuterPadding),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 416.dp).fillMaxWidth(),
            shape = IrideFolio.DialogShape,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(IrideFolio.DialogBorderWidth, colors.strokeHairline),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(IrideFolio.DialogContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogSectionSpacing),
            ) {
                Icon(
                    painter = painterResource(R.drawable.iride_wordmark),
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(width = 96.dp, height = 24.dp),
                )
                Text(
                    text = stringResource(if (autoRestart) R.string.crash_restarting else R.string.crash_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (autoRestart) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onSurface,
                        trackColor = colors.outlineVariant,
                    )
                }
                if (reportCopied || !reportAvailable) {
                    Text(
                        text = stringResource(
                            if (reportCopied) R.string.crash_report_copied else R.string.crash_no_log,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (!autoRestart) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(IrideFolio.DialogActionSpacing),
                    ) {
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth(),
                            shape = IrideFolio.ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.inverseSurface,
                                contentColor = colors.inverseOnSurface,
                            ),
                        ) {
                            Text(stringResource(R.string.restart), fontFamily = IBMPlexMonoFontFamily)
                        }
                        if (reportAvailable) {
                            OutlinedButton(
                                onClick = onShare,
                                modifier = Modifier.fillMaxWidth(),
                                shape = IrideFolio.ButtonShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                            ) {
                                Text(stringResource(R.string.crash_share_title), fontFamily = IBMPlexMonoFontFamily)
                            }
                            TextButton(onClick = onCopy) {
                                Text(
                                    text = stringResource(R.string.copy),
                                    fontFamily = IBMPlexMonoFontFamily,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
