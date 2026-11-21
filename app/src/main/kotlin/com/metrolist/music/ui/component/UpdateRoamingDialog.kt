package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

data class UpdateRoamingPrompt(
    val releaseTag: String,
    val versionName: String,
    val downloadSize: String,
    val downloadUrl: String,
)

@Composable
fun UpdateRoamingDialog(
    prompt: UpdateRoamingPrompt,
    onDownloadNow: () -> Unit,
    onWaitForWifi: () -> Unit,
) {
    IrideFolioAlertDialog(
        onDismissRequest = onWaitForWifi,
        title = { Text(stringResource(R.string.update_roaming_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_roaming_message, prompt.versionName, prompt.downloadSize))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.update_roaming_question))
            }
        },
        confirmButton = {
            IrideFolioPrimaryButton(onClick = onDownloadNow) {
                Text(stringResource(R.string.update_roaming_download_now))
            }
        },
        dismissButton = {
            IrideFolioSecondaryButton(onClick = onWaitForWifi) {
                Text(stringResource(R.string.update_roaming_wait_wifi))
            }
        },
    )
}
