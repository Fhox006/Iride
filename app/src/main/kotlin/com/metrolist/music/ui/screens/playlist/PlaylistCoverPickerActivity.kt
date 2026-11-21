package com.metrolist.music.ui.screens.playlist

import android.net.Uri
import android.os.Bundle
import com.metrolist.music.ui.component.IrideFolioToast as Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.metrolist.music.R
import com.metrolist.music.utils.importPlaylistCover
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The picker result survives dismissal of the menu that launched it. */
class PlaylistCoverPickerActivity : ComponentActivity() {
    private var pendingUri: Uri? = null
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) finish() else saveImage(uri)
    }

    private fun saveImage(uri: Uri) {
        pendingUri = uri
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        lifecycleScope.launch {
            try {
                val id = intent.getStringExtra("playlistId") ?: error("Missing playlist ID")
                importPlaylistCover(this@PlaylistCoverPickerActivity, id, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportException(error)
                Toast.makeText(this@PlaylistCoverPickerActivity, R.string.playlist_image_failed, Toast.LENGTH_LONG).show()
            } finally {
                if (!isChangingConfigurations) finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val restored = savedInstanceState?.getString("pendingUri")?.let(Uri::parse)
        if (restored != null) saveImage(restored)
        else if (savedInstanceState == null) {
            try {
                picker.launch("image/*")
            } catch (error: Exception) {
                reportException(error)
                Toast.makeText(this, R.string.playlist_image_failed, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingUri?.let { outState.putString("pendingUri", it.toString()) }
        super.onSaveInstanceState(outState)
    }
}
