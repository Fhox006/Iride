/*
 * Comments panel behavior adapted from SimpMusic (maxrave-dev and contributors), GPL-3.0.
 * Source: composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/component/CommentsBottomSheet.kt
 * Revision: 068d74435f0cf0e8552af3da07879eafd328fea7. Presented with Iride Folio controls.
 */
package com.metrolist.music.ui.player


import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.metrolist.innertube.pages.CommentItem
import com.metrolist.innertube.pages.CommentSortOrder
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CommentsStatus
import com.metrolist.music.viewmodels.CommentsViewModel
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.LocalArtworkContentColor
import com.metrolist.music.ui.component.IrideFolioPrimaryIconButton
import com.metrolist.music.ui.component.IrideFolioChoiceButton
import com.metrolist.music.ui.component.irideFolioGestures
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.ProvideTextStyle
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CommentsPanel(
    mediaId: String?,
    onClose: () -> Unit,
    textButtonColor: Color,
    iconButtonColor: Color,
    modifier: Modifier = Modifier,
    textScale: Float = 1f,
    commentsViewModel: CommentsViewModel = hiltViewModel(),
) {
    val status by commentsViewModel.status.collectAsStateWithLifecycle()
    val comments by commentsViewModel.comments.collectAsStateWithLifecycle()
    val isLoadingMore by commentsViewModel.isLoadingMore.collectAsStateWithLifecycle()
    val isPosting by commentsViewModel.isPosting.collectAsStateWithLifecycle()
    val postError by commentsViewModel.postError.collectAsStateWithLifecycle()
    val createCommentParams by commentsViewModel.createCommentParams.collectAsStateWithLifecycle()
    val count by commentsViewModel.commentsCountText.collectAsStateWithLifecycle()
    val sortOrder by commentsViewModel.sortOrder.collectAsStateWithLifecycle()
    val moreFailed by commentsViewModel.moreFailed.collectAsStateWithLifecycle()
    val replies by commentsViewModel.replies.collectAsStateWithLifecycle()
    val availableSortOrders by commentsViewModel.availableSortOrders.collectAsStateWithLifecycle()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }

    var commentText by rememberSaveable(mediaId) { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(mediaId, innerTubeCookie) {
        mediaId?.let { commentsViewModel.loadComments(it, force = true) }
    }
    LaunchedEffect(mediaId, sortOrder) { lazyListState.scrollToItem(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 4 && !commentsViewModel.moreFailed.value)
                    commentsViewModel.loadMore()
            }
    }

    CompositionLocalProvider(
        LocalArtworkContentColor provides textButtonColor,
        LocalContentColor provides textButtonColor,
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.bodySmall.copy(
                fontFamily = IBMPlexMonoFontFamily,
                fontSize = (11 * textScale).sp,
                lineHeight = (15 * textScale).sp,
            )
        ) {
            Column(modifier = modifier.fillMaxSize().imePadding()) {
                IridePlayerPanelHeader(
                    title = stringResource(R.string.comments),
                    subtitle = count,
                    onClose = onClose,
                    textColor = textButtonColor,
                    textScale = textScale,
                )
                if (availableSortOrders.isNotEmpty() &&
                    (status == CommentsStatus.Loaded || status == CommentsStatus.Empty)) {
                    val sortScroll = rememberScrollState()
                    Row(
                        Modifier.fillMaxWidth().rubberBandOverscroll(Orientation.Horizontal, sortScroll)
                            .horizontalScroll(sortScroll, overscrollEffect = null)
                            .padding(horizontal = 8.dp * textScale, vertical = 4.dp * textScale),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CommentSortOrder.entries.forEach { order ->
                            IrideFolioChoiceButton(
                                selected = sortOrder == order,
                                compact = true,
                                enabled = order in availableSortOrders,
                                onClick = { commentsViewModel.changeSort(order) },
                            ) {
                                Text(
                                    stringResource(if (order == CommentSortOrder.TOP) R.string.comments_sort_top else R.string.comments_sort_newest),
                                    fontSize = (10 * textScale).sp,
                                    lineHeight = (14 * textScale).sp,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = textButtonColor.copy(alpha = .12f))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (if (mediaId == null) CommentsStatus.Empty else status) {
                        CommentsStatus.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = textButtonColor)
                            }
                        }

                        CommentsStatus.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.comments_error),
                                    color = textButtonColor.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(id = R.string.try_again),
                                    color = textButtonColor,
                                    modifier = Modifier.clickable { commentsViewModel.retry() },
                                )
                            }
                        }

                        CommentsStatus.Empty -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(id = R.string.comments_empty),
                                    color = textButtonColor.copy(alpha = 0.7f),
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize().irideFolioGestures(lazyListState),
                                overscrollEffect = null,
                                contentPadding = PaddingValues(vertical = 4.dp * textScale),
                            ) {
                                items(comments, key = { it.id }) { comment ->
                                    CommentRow(
                                        comment = comment,
                                        textColor = textButtonColor,
                                        textScale = textScale,
                                        onReplies = comment.repliesToken?.let {
                                            { commentsViewModel.toggleReplies(comment) }
                                        },
                                    )
                                    val thread = replies[comment.id]
                                    if (thread?.open == true) {
                                        Column(Modifier.padding(start = 24.dp * textScale)) {
                                            thread.comments.forEach { reply ->
                                                androidx.compose.runtime.key(reply.id) {
                                                    CommentRow(comment = reply, textColor = textButtonColor, textScale = textScale)
                                                }
                                            }
                                            if (thread.loading) {
                                                CircularProgressIndicator(
                                                    Modifier.padding(8.dp).size(18.dp),
                                                    strokeWidth = 2.dp, color = textButtonColor,
                                                )
                                            } else if (thread.failed || thread.continuation != null) {
                                                Text(
                                                    stringResource(if (thread.failed) R.string.try_again else R.string.mp3_comments_more_replies),
                                                    color = textButtonColor,
                                                    modifier = Modifier.clickable { commentsViewModel.loadMoreReplies(comment) }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isLoadingMore) {
                                    item(key = "loading_more") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp * textScale, vertical = 8.dp * textScale),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = textButtonColor,
                                            )
                                        }
                                    }
                                }
                                if (moreFailed) {
                                    item(key = "retry_more") {
                                        Text(
                                            stringResource(R.string.try_again),
                                            color = textButtonColor,
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable { commentsViewModel.loadMore() }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (mediaId != null && isLoggedIn && createCommentParams != null &&
                    (status == CommentsStatus.Loaded || status == CommentsStatus.Empty)) {
                    CommentComposeBar(
                        text = commentText,
                        onTextChange = { commentText = it },
                        onSend = {
                            commentsViewModel.postComment(commentText) {
                                commentText = ""
                            }
                        },
                        isPosting = isPosting,
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        textScale = textScale,
                    )
                    if (postError) {
                        Text(
                            text = stringResource(id = R.string.comments_post_failed),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = (11 * textScale).sp,
                            lineHeight = (15 * textScale).sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                } else if (!isLoggedIn) {
                    Text(
                        text = stringResource(id = R.string.comments_sign_in_to_comment),
                        color = textButtonColor.copy(alpha = 0.6f),
                        fontSize = (11 * textScale).sp,
                        lineHeight = (15 * textScale).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp * textScale, vertical = 8.dp * textScale),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    textColor: Color,
    textScale: Float,
    onReplies: (() -> Unit)? = null,
) {
    var expanded by remember(comment.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp * textScale, vertical = 8.dp * textScale),
        horizontalArrangement = Arrangement.spacedBy(8.dp * textScale),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp * textScale)
                .clip(FolioShape(7.dp * textScale))
                .background(textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (comment.authorThumbnailUrl != null) {
                AsyncImage(
                    model = comment.authorThumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp * textScale)
                        .clip(FolioShape(7.dp * textScale)),
                )
            } else {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    color = textColor,
                    fontSize = (12 * textScale).sp,
                    lineHeight = (16 * textScale).sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!comment.publishedTimeText.isNullOrBlank()) {
                    Text(
                        text = "  •  ${comment.publishedTimeText}",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = (10 * textScale).sp,
                        lineHeight = (14 * textScale).sp,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = comment.text,
                color = textColor.copy(alpha = 0.9f),
                fontSize = (12 * textScale).sp,
                lineHeight = (17 * textScale).sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { expanded = !expanded },
            )

            if (!comment.likeCountText.isNullOrBlank() || comment.replyCount > 0 || onReplies != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val likeCountText = comment.likeCountText
                    if (!likeCountText.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.favorite_border),
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = likeCountText,
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = (10 * textScale).sp,
                                lineHeight = (14 * textScale).sp,
                            )
                        }
                    }
                    if (comment.replyCount > 0 || onReplies != null) {
                        Text(
                            text = if (comment.replyCount > 0) stringResource(id = R.string.comments_reply_count, comment.replyCount)
                                else stringResource(R.string.mp3_comments_view_replies),
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = (10 * textScale).sp,
                            lineHeight = (14 * textScale).sp,
                            modifier = if (onReplies == null) Modifier else Modifier.clickable(onClick = onReplies),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isPosting: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textScale: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp * textScale, vertical = 6.dp * textScale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(IrideFolio.ButtonShape)
                .border(IrideFolio.ButtonBorderWidth, textButtonColor.copy(alpha = IrideFolio.ButtonBorderOpacity), IrideFolio.ButtonShape)
                .background(textButtonColor.copy(alpha = 0.04f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.comments_write_hint),
                    color = textButtonColor.copy(alpha = 0.5f),
                    fontSize = (12 * textScale).sp,
                    lineHeight = (17 * textScale).sp,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textButtonColor,
                    fontSize = (12 * textScale).sp,
                    lineHeight = (17 * textScale).sp,
                    fontFamily = IBMPlexMonoFontFamily,
                ),
                maxLines = 3,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(textButtonColor),
                modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp),
            )
        }

        IrideFolioPrimaryIconButton(
            onClick = onSend,
            size = IrideFolio.ControlsTouchTargetSize,
            enabled = text.isNotBlank() && !isPosting,
        ) {
            if (isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = iconButtonColor,
                )
            } else {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.arrow_upward),
                    contentDescription = stringResource(id = R.string.comments_post),
                    modifier = Modifier.size(IrideFolio.PlayerActionIconSize),
                )
            }
        }
    }
}
