package com.metrolist.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.metrolist.innertube.pages.CommentItem
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CommentsStatus
import com.metrolist.music.viewmodels.CommentsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CommentsPanel(
    mediaId: String?,
    onClose: () -> Unit,
    textButtonColor: Color,
    iconButtonColor: Color,
    modifier: Modifier = Modifier,
    commentsViewModel: CommentsViewModel = hiltViewModel(),
) {
    val status by commentsViewModel.status.collectAsStateWithLifecycle()
    val comments by commentsViewModel.comments.collectAsStateWithLifecycle()
    val isLoadingMore by commentsViewModel.isLoadingMore.collectAsStateWithLifecycle()
    val isPosting by commentsViewModel.isPosting.collectAsStateWithLifecycle()
    val postError by commentsViewModel.postError.collectAsStateWithLifecycle()
    val createCommentParams by commentsViewModel.createCommentParams.collectAsStateWithLifecycle()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }

    var commentText by rememberSaveable(mediaId) { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(mediaId) {
        mediaId?.let { commentsViewModel.loadComments(it) }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= layoutInfo.totalItemsCount - 4 && layoutInfo.totalItemsCount > 0
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) commentsViewModel.loadMore()
            }
    }

    InlinePlayerPageFrame(
        modifier = modifier,
        isFullScreen = true,
        pills = {},
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (status) {
                        CommentsStatus.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = textButtonColor)
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
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(comments, key = { it.id }) { comment ->
                                    CommentRow(
                                        comment = comment,
                                        textColor = textButtonColor,
                                    )
                                }
                                if (isLoadingMore) {
                                    item(key = "loading_more") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
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
                            }
                        }
                    }
                }

                if (isLoggedIn && createCommentParams != null) {
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
                    )
                    if (postError) {
                        Text(
                            text = stringResource(id = R.string.comments_post_failed),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                } else if (!isLoggedIn) {
                    Text(
                        text = stringResource(id = R.string.comments_sign_in_to_comment),
                        color = textButtonColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        },
    )
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    textColor: Color,
) {
    var expanded by remember(comment.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(textColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (comment.authorThumbnailUrl != null) {
                AsyncImage(
                    model = comment.authorThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
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
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!comment.publishedTimeText.isNullOrBlank()) {
                    Text(
                        text = "  •  ${comment.publishedTimeText}",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = comment.text,
                color = textColor.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { expanded = !expanded },
            )

            if (!comment.likeCountText.isNullOrBlank() || comment.replyCount > 0) {
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
                                fontSize = 11.sp,
                            )
                        }
                    }
                    if (comment.replyCount > 0) {
                        Text(
                            text = stringResource(id = R.string.comments_reply_count, comment.replyCount),
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 11.sp,
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(textButtonColor.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.comments_write_hint),
                    color = textButtonColor.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textButtonColor,
                    fontSize = 13.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(textButtonColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IconButton(
            onClick = onSend,
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
                    tint = if (text.isNotBlank()) textButtonColor else textButtonColor.copy(alpha = 0.4f),
                )
            }
        }
    }
}
