/*
 * Adapted from SimpMusic (maxrave-dev and contributors), licensed under GPL-3.0.
 * Source: composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/screen/home/ListenTogetherScreen.kt
 * Revision: 068d74435f0cf0e8552af3da07879eafd328fea7.
 * WorkArea, RoomCodePoster, Members and room-code validation adapted to Iride's manager and Folio UI.
 */
package com.metrolist.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.ListenTogetherEvent
import com.metrolist.music.listentogether.UserInfo
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioChoiceButton
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.LocalArtworkContentColor
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.folioMenuAction
import com.metrolist.music.ui.component.irideFolioGestures
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay

private const val ROOM_CODE_LENGTH = 8
private const val COPIED_FEEDBACK_MS = 1800L

@Composable
internal fun IridePlayerPanelHeader(
    title: String,
    onClose: () -> Unit,
    subtitle: String? = null,
    textColor: Color = Color.White,
    textScale: Float = 1f,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp * textScale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontFamily = IBMPlexMonoFontFamily,
                fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp, lineHeight = (20 * textScale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = textColor.copy(alpha = .6f), fontFamily = IBMPlexMonoFontFamily,
                    fontSize = (10 * textScale).sp, lineHeight = (14 * textScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize)) {
            Icon(painterResource(R.drawable.close), stringResource(R.string.close),
                tint = textColor, modifier = Modifier.size(IrideFolio.PlayerActionIconSize))
        }
    }
}

@Composable
internal fun ListenTogetherPanel(
    onClose: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val manager = LocalListenTogetherManager.current
    if (manager == null) {
        Column(modifier.fillMaxSize()) {
            IridePlayerPanelHeader(stringResource(R.string.listen_together), onClose)
            Text(stringResource(R.string.listen_together_not_configured), color = Color.White,
                modifier = Modifier.padding(10.dp))
            PanelAction(R.string.settings, onSettings)
        }
        return
    }

    val context = LocalContext.current
    val connection by manager.connectionState.collectAsStateWithLifecycle()
    val room by manager.roomState.collectAsStateWithLifecycle()
    val selfId by manager.userId.collectAsStateWithLifecycle()
    val requests by manager.pendingJoinRequests.collectAsStateWithLifecycle()
    val suggestions by manager.pendingSuggestions.collectAsStateWithLifecycle()
    val buffering by manager.bufferingUsers.collectAsStateWithLifecycle()
    var savedUsername by rememberPreference(ListenTogetherUsernameKey, "")
    var username by rememberSaveable { mutableStateOf(savedUsername) }
    var roomCode by rememberSaveable { mutableStateOf("") }
    var pending by rememberSaveable { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isConnected = connection == ConnectionState.CONNECTED
    val isHost = room?.hostId == selfId && selfId != null
    val copyLabel = stringResource(R.string.copy_code)
    val shareLabel = stringResource(R.string.listen_together)

    LaunchedEffect(savedUsername) {
        if (username.isBlank()) username = savedUsername
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }
    LaunchedEffect(room) { if (room != null) { pending = false; error = null } }
    LaunchedEffect(manager) {
        manager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> { error = event.reason; pending = false }
                is ListenTogetherEvent.ConnectionError -> { error = event.error; pending = false }
                is ListenTogetherEvent.ServerError -> { error = event.message; pending = false }
                is ListenTogetherEvent.Kicked -> { error = event.reason; pending = false }
                is ListenTogetherEvent.RoomCreated, is ListenTogetherEvent.JoinApproved -> {
                    pending = false
                    error = null
                }
                ListenTogetherEvent.Disconnected -> pending = false
                else -> Unit
            }
        }
    }

    CompositionLocalProvider(LocalArtworkContentColor provides Color.White, LocalContentColor provides Color.White) {
        ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(fontFamily = IBMPlexMonoFontFamily)) {
            Column(modifier.fillMaxSize().imePadding()) {
                IridePlayerPanelHeader(stringResource(R.string.listen_together), onClose)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().irideFolioGestures(listState),
                    overscrollEffect = null,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    item(key = "connection") {
                        PanelCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(when (connection) {
                                    ConnectionState.CONNECTED -> R.string.listen_together_connected
                                    ConnectionState.CONNECTING -> R.string.listen_together_connecting
                                    ConnectionState.RECONNECTING -> R.string.listen_together_reconnecting
                                    ConnectionState.ERROR -> R.string.listen_together_error
                                    ConnectionState.DISCONNECTED -> R.string.listen_together_disconnected
                                }), modifier = Modifier.weight(1f), fontSize = 11.sp)
                                PanelAction(
                                    if (isConnected) R.string.disconnect else R.string.connect,
                                    onClick = { if (isConnected) manager.disconnect() else manager.connect() },
                                    enabled = connection != ConnectionState.CONNECTING && connection != ConnectionState.RECONNECTING,
                                )
                            }
                        }
                    }
                    error?.let { message ->
                        item(key = "error") {
                            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    val activeRoom = room
                    if (activeRoom != null) {
                        item(key = "room_code") {
                            PanelCard {
                                Text(stringResource(R.string.room_code), fontSize = 10.sp, color = Color.White.copy(alpha = .6f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(activeRoom.roomCode.chunked(4).joinToString(" "),
                                        modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp, letterSpacing = 1.sp)
                                    IconButton(onClick = {
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                            .setPrimaryClip(ClipData.newPlainText(copyLabel, activeRoom.roomCode))
                                        copied = true
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(painterResource(if (copied) R.drawable.check else R.drawable.content_copy),
                                            stringResource(if (copied) R.string.copied_to_clipboard else R.string.copy_code),
                                            modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, activeRoom.roomCode)
                                        }
                                        context.startActivity(Intent.createChooser(share, shareLabel))
                                    }, modifier = Modifier.size(40.dp)) {
                                        Icon(painterResource(R.drawable.share), stringResource(R.string.share), modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (isHost) Text(stringResource(R.string.listen_together_you_are_host), fontSize = 10.sp)
                            }
                        }
                        if (buffering.isNotEmpty()) {
                            item(key = "buffering") {
                                Text(
                                    stringResource(R.string.mp3_listen_together_buffering,
                                        activeRoom.users.filter { it.userId in buffering }.joinToString { it.username }),
                                    fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                        if (isHost && requests.isNotEmpty()) {
                            item(key = "request_title") { PanelSectionTitle(R.string.pending_requests) }
                            items(requests, key = { "request_${it.userId}" }) { request ->
                                PanelCard {
                                    Text(request.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PanelAction(R.string.approve, { manager.approveJoin(request.userId) })
                                        PanelAction(R.string.reject, { manager.rejectJoin(request.userId) })
                                    }
                                }
                            }
                        }
                        if (isHost && suggestions.isNotEmpty()) {
                            item(key = "suggestion_title") { PanelSectionTitle(R.string.pending_suggestions) }
                            items(suggestions, key = { "suggestion_${it.suggestionId}" }) { suggestion ->
                                PanelCard {
                                    Text(suggestion.trackInfo.title, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(suggestion.fromUsername, fontSize = 10.sp, color = Color.White.copy(alpha = .6f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PanelAction(R.string.approve, { manager.approveSuggestion(suggestion.suggestionId) })
                                        PanelAction(R.string.reject, { manager.rejectSuggestion(suggestion.suggestionId) })
                                    }
                                }
                            }
                        }
                        item(key = "members_title") { PanelSectionTitle(R.string.connected_users) }
                        items(activeRoom.users, key = { "member_${it.userId}" }) { member ->
                            PanelMember(member, member.userId == selfId, isHost,
                                onKick = { manager.kickUser(member.userId) },
                                onBlock = { manager.blockUser(member.username); manager.kickUser(member.userId) },
                                onTransfer = { manager.transferHost(member.userId) })
                        }
                        item(key = "leave") {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PanelAction(R.string.leave_room, { manager.leaveRoom() })
                                if (!isHost) PanelAction(R.string.resync, { manager.requestSync() })
                            }
                        }
                    } else if (pending) {
                        item(key = "pending") {
                            PanelCard {
                                Text(stringResource(if (joining) R.string.waiting_for_approval else R.string.creating_room), fontSize = 11.sp)
                                if (joining) Text(roomCode.chunked(4).joinToString(" "), fontWeight = FontWeight.Bold)
                                PanelAction(android.R.string.cancel, { manager.disconnect(); pending = false })
                            }
                        }
                    } else {
                        item(key = "create_join") {
                            PanelCard {
                                PanelField(username, { username = it }, R.string.enter_username)
                                PanelAction(R.string.listen_together_create_room, {
                                    savedUsername = username.trim()
                                    pending = true
                                    joining = false
                                    error = null
                                    manager.createRoom(username.trim())
                                }, enabled = isConnected && username.isNotBlank())
                                PanelField(roomCode, { value ->
                                    roomCode = value.uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_LENGTH)
                                }, R.string.enter_room_code, uppercase = true)
                                PanelAction(R.string.listen_together_join_room, {
                                    savedUsername = username.trim()
                                    pending = true
                                    joining = true
                                    error = null
                                    manager.joinRoom(roomCode, username.trim())
                                }, enabled = isConnected && username.isNotBlank() && roomCode.length == ROOM_CODE_LENGTH)
                            }
                        }
                        item(key = "background_note") {
                            Text(stringResource(R.string.listen_together_background_disconnect_note),
                                color = Color.White.copy(alpha = .6f), fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    item(key = "settings") { PanelAction(R.string.settings, onSettings) }
                }
            }
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(FolioShape(8.dp))
            .background(Color.White.copy(alpha = .04f))
            .border(.5.dp, Color.White.copy(alpha = .12f), FolioShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp), content = content,
    )
}

@Composable
private fun PanelAction(label: Int, onClick: () -> Unit, enabled: Boolean = true) {
    IrideFolioChoiceButton(selected = false, onClick = onClick, enabled = enabled, compact = true,
        modifier = Modifier.semantics { role = Role.Button }) {
        Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanelSectionTitle(label: Int) {
    Text(stringResource(label), color = Color.White.copy(alpha = .6f),
        fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
}

@Composable
private fun PanelField(value: String, onValueChange: (String) -> Unit, hint: Int, uppercase: Boolean = false) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = IBMPlexMonoFontFamily),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(capitalization = if (uppercase) KeyboardCapitalization.Characters else KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth().clip(FolioShape(6.dp))
            .background(Color.White.copy(alpha = .06f)).padding(horizontal = 10.dp, vertical = 10.dp),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(stringResource(hint), fontSize = 11.sp, color = Color.White.copy(alpha = .5f))
                field()
            }
        },
    )
}

@Composable
private fun PanelMember(
    member: UserInfo,
    isSelf: Boolean,
    canManage: Boolean,
    onKick: () -> Unit,
    onBlock: () -> Unit,
    onTransfer: () -> Unit,
) {
    val menu = LocalMenuState.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(FolioShape(6.dp)).background(Color.White.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
            Text(member.username.trim().firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(member.username, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
            if (member.isHost || isSelf || !member.isConnected) {
                Text(stringResource(when {
                    member.isHost -> R.string.host_label
                    isSelf -> R.string.you_label
                    else -> R.string.listen_together_disconnected
                }), fontSize = 9.sp, color = Color.White.copy(alpha = .6f))
            }
        }
        if (canManage && !isSelf && !member.isHost) {
            SongMenuIconButton(onClick = {
                menu.show(anchored = true) {
                    IrideFolioContextMenu(
                        items = listOf(
                            folioMenuAction(R.string.kick_user, R.drawable.logout, onKick),
                            folioMenuAction(R.string.permanently_kick_user, R.drawable.close, onBlock),
                            folioMenuAction(R.string.transfer_ownership, R.drawable.crown, onTransfer),
                        ), onDismiss = menu::dismiss,
                    )
                }
            }) {
                Icon(painterResource(R.drawable.more_vert), stringResource(R.string.manage_user),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
