/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.metrolist.music.R
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.ui.component.IridePageFrame
import com.metrolist.music.ui.theme.ForceDarkTheme
import com.metrolist.music.ui.theme.IrideTheme
import com.metrolist.music.utils.CrashHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val crashLog =
            intent.getStringExtra(CrashHandler.EXTRA_CRASH_LOG) ?: getString(R.string.crash_no_log)
        setContent {
            val darkTheme = ForceDarkTheme || isSystemInDarkTheme()
            IrideTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides WindowInsets.navigationBars,
                ) {
                    CrashScreen(
                        crashLog = crashLog,
                        onCopy = { copyCrashLog(crashLog) },
                        onClose = { finishAffinity() },
                        onShare = { shareCrashLog(crashLog) },
                    )
                }
            }
        }
    }

    private fun copyCrashLog(crashLog: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Iride crash log", crashLog))
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun shareCrashLog(crashLog: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "metrolist_crash_$timestamp.txt"
            val crashFile = File(cacheDir, fileName)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashScreen(
    crashLog: String,
    onCopy: () -> Unit,
    onClose: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onCopy()
    }
    IridePageFrame(
        title = stringResource(R.string.crash_title),
        scrollState = rememberScrollState(),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.crash_close))
            }
        },
        actions = {
            IconButton(onClick = onCopy) {
                Icon(painterResource(R.drawable.content_copy), stringResource(R.string.copy))
            }
        },
        overlay = {
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onShare,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.crash_share_logs)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.crash_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(16.dp)
            ) {
                Text(
                    text = crashLog,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
