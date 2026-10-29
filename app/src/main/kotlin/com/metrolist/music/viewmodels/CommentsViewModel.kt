/*
 * Comment loading safeguards adapted from SimpMusic (maxrave-dev and contributors), GPL-3.0.
 * Source: composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/viewModel/CommentsViewModel.kt
 * Revision: 068d74435f0cf0e8552af3da07879eafd328fea7. Adapted for Iride's InnerTube and Hilt.
 */
package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.CommentItem
import com.metrolist.innertube.pages.CommentSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CommentsStatus {
    object Idle : CommentsStatus()
    object Loading : CommentsStatus()
    object Loaded : CommentsStatus()
    object Empty : CommentsStatus()
    object Error : CommentsStatus()
}

data class CommentReplies(
    val open: Boolean = true,
    val comments: List<CommentItem> = emptyList(),
    val continuation: String? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class CommentsViewModel @Inject constructor() : ViewModel() {
    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private var generation = 0
    private val replyJobs = mutableMapOf<String, Job>()
    private val _replies = MutableStateFlow<Map<String, CommentReplies>>(emptyMap())
    val replies: StateFlow<Map<String, CommentReplies>> = _replies.asStateFlow()
    private var currentVideoId: String? = null
    private var continuation: String? = null
    private var sortTokens: Map<CommentSortOrder, String> = emptyMap()

    private val _status = MutableStateFlow<CommentsStatus>(CommentsStatus.Idle)
    val status: StateFlow<CommentsStatus> = _status.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentItem>>(emptyList())
    val comments: StateFlow<List<CommentItem>> = _comments.asStateFlow()

    private val _commentsCountText = MutableStateFlow<String?>(null)
    val commentsCountText: StateFlow<String?> = _commentsCountText.asStateFlow()

    private val _sortOrder = MutableStateFlow(CommentSortOrder.TOP)
    val sortOrder: StateFlow<CommentSortOrder> = _sortOrder.asStateFlow()

    private val _availableSortOrders = MutableStateFlow<Set<CommentSortOrder>>(emptySet())
    val availableSortOrders: StateFlow<Set<CommentSortOrder>> = _availableSortOrders.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _moreFailed = MutableStateFlow(false)
    val moreFailed: StateFlow<Boolean> = _moreFailed.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postError = MutableStateFlow(false)
    val postError: StateFlow<Boolean> = _postError.asStateFlow()

    private val _createCommentParams = MutableStateFlow<String?>(null)
    val createCommentParams: StateFlow<String?> = _createCommentParams.asStateFlow()

    fun loadComments(videoId: String, force: Boolean = false) {
        if (!force && currentVideoId == videoId && _status.value != CommentsStatus.Idle) return
        currentVideoId = videoId
        val requestGeneration = ++generation
        resetReplies()
        moreJob?.cancel()
        _isLoadingMore.value = false
        _moreFailed.value = false
        _commentsCountText.value = null
        _postError.value = false
        continuation = null
        sortTokens = emptyMap()
        _availableSortOrders.value = emptySet()
        _createCommentParams.value = null
        _comments.value = emptyList()
        _sortOrder.value = CommentSortOrder.TOP
        _status.value = CommentsStatus.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            YouTube.comments(videoId)
                .onSuccess { page ->
                    if (requestGeneration != generation) return@onSuccess
                    _comments.value = page.comments.distinctBy { it.id }
                    continuation = page.continuation
                    _commentsCountText.value = page.commentsCountText
                    sortTokens = page.sortTokens
                    _availableSortOrders.value = sortTokens.keys
                    _createCommentParams.value = page.createCommentParams
                    _status.value = if (page.comments.isEmpty()) CommentsStatus.Empty else CommentsStatus.Loaded
                }
                .onFailure {
                    if (requestGeneration != generation) return@onFailure
                    _status.value = CommentsStatus.Error
                }
        }
    }

    fun retry() {
        currentVideoId?.let { loadComments(it, force = true) }
    }

    fun loadMore() {
        val token = continuation ?: return
        if (_isLoadingMore.value || _status.value != CommentsStatus.Loaded) return
        val requestGeneration = generation
        _isLoadingMore.value = true
        _moreFailed.value = false
        moreJob = viewModelScope.launch {
            YouTube.commentsContinuation(token)
                .onSuccess { page ->
                    if (requestGeneration != generation) return@onSuccess
                    val previousSize = _comments.value.size
                    _comments.value = (_comments.value + page.comments).distinctBy { it.id }
                    continuation = page.continuation.takeIf {
                        it != token && _comments.value.size > previousSize
                    }
                }
                .onFailure {
                    if (requestGeneration != generation) return@onFailure
                    _moreFailed.value = true
                }
            if (requestGeneration == generation) _isLoadingMore.value = false
        }
    }

    fun changeSort(order: CommentSortOrder) {
        if (_sortOrder.value == order) return
        val token = sortTokens[order] ?: return
        val requestGeneration = ++generation
        resetReplies()
        moreJob?.cancel()
        _isLoadingMore.value = false
        _moreFailed.value = false
        continuation = null
        _sortOrder.value = order
        _comments.value = emptyList()
        _status.value = CommentsStatus.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            YouTube.commentsContinuation(token)
                .onSuccess { page ->
                    if (requestGeneration != generation) return@onSuccess
                    _comments.value = page.comments.distinctBy { it.id }
                    continuation = page.continuation
                    _status.value = if (page.comments.isEmpty()) CommentsStatus.Empty else CommentsStatus.Loaded
                }
                .onFailure {
                    if (requestGeneration != generation) return@onFailure
                    _status.value = CommentsStatus.Error
                }
        }
    }

    fun postComment(text: String, onPosted: () -> Unit) {
        val params = _createCommentParams.value ?: return
        if (text.isBlank() || _isPosting.value) return
        val videoId = currentVideoId
        val requestGeneration = generation
        _isPosting.value = true
        _postError.value = false
        viewModelScope.launch {
            YouTube.postComment(text, params)
                .onSuccess {
                    _isPosting.value = false
                    if (requestGeneration != generation) return@onSuccess
                    onPosted()
                    videoId?.let { loadComments(it, force = true) }
                }
                .onFailure {
                    _isPosting.value = false
                    if (requestGeneration != generation) return@onFailure
                    _postError.value = true
                }
        }
    }

    fun toggleReplies(comment: CommentItem) {
        val current = _replies.value[comment.id]
        if (current == null) {
            loadReplies(comment.id, comment.repliesToken ?: return, append = false)
        } else {
            _replies.value = _replies.value + (comment.id to current.copy(open = !current.open))
        }
    }

    fun loadMoreReplies(comment: CommentItem) {
        val current = _replies.value[comment.id] ?: return
        if (current.loading) return
        val token = current.continuation ?: comment.repliesToken?.takeIf { current.failed } ?: return
        loadReplies(comment.id, token, append = current.comments.isNotEmpty())
    }

    private fun loadReplies(commentId: String, token: String, append: Boolean) {
        val requestGeneration = generation
        val previous = _replies.value[commentId] ?: CommentReplies()
        _replies.value = _replies.value + (commentId to previous.copy(loading = true, failed = false))
        replyJobs[commentId]?.cancel()
        replyJobs[commentId] = viewModelScope.launch {
            YouTube.commentsContinuation(token)
                .onSuccess { page ->
                    if (requestGeneration != generation) return@onSuccess
                    val current = _replies.value[commentId] ?: return@onSuccess
                    val comments = ((if (append) current.comments else emptyList()) + page.comments)
                        .distinctBy { it.id }
                    _replies.value = _replies.value + (commentId to current.copy(
                        comments = comments,
                        continuation = page.continuation.takeIf {
                            it != token && (!append || comments.size > current.comments.size)
                        },
                        loading = false,
                        failed = false,
                    ))
                }
                .onFailure {
                    if (requestGeneration != generation) return@onFailure
                    val current = _replies.value[commentId] ?: return@onFailure
                    _replies.value = _replies.value + (commentId to current.copy(loading = false, failed = true))
                }
            if (requestGeneration == generation) replyJobs.remove(commentId)
        }
    }

    private fun resetReplies() {
        replyJobs.values.forEach { it.cancel() }
        replyJobs.clear()
        _replies.value = emptyMap()
    }
}
