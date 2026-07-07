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

@HiltViewModel
class CommentsViewModel @Inject constructor() : ViewModel() {
    private var loadJob: Job? = null
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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postError = MutableStateFlow(false)
    val postError: StateFlow<Boolean> = _postError.asStateFlow()

    private val _createCommentParams = MutableStateFlow<String?>(null)
    val createCommentParams: StateFlow<String?> = _createCommentParams.asStateFlow()

    fun loadComments(videoId: String, force: Boolean = false) {
        if (!force && currentVideoId == videoId && _status.value != CommentsStatus.Idle) return
        currentVideoId = videoId
        continuation = null
        sortTokens = emptyMap()
        _createCommentParams.value = null
        _comments.value = emptyList()
        _sortOrder.value = CommentSortOrder.TOP
        _status.value = CommentsStatus.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            YouTube.comments(videoId)
                .onSuccess { page ->
                    _comments.value = page.comments
                    continuation = page.continuation
                    _commentsCountText.value = page.commentsCountText
                    sortTokens = page.sortTokens
                    _createCommentParams.value = page.createCommentParams
                    _status.value = if (page.comments.isEmpty()) CommentsStatus.Empty else CommentsStatus.Loaded
                }
                .onFailure {
                    _status.value = CommentsStatus.Error
                }
        }
    }

    fun retry() {
        currentVideoId?.let { loadComments(it, force = true) }
    }

    fun loadMore() {
        val token = continuation ?: return
        if (_isLoadingMore.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            YouTube.commentsContinuation(token)
                .onSuccess { page ->
                    _comments.value = _comments.value + page.comments
                    continuation = page.continuation
                }
                .onFailure {
                    continuation = null
                }
            _isLoadingMore.value = false
        }
    }

    fun changeSort(order: CommentSortOrder) {
        if (_sortOrder.value == order) return
        val token = sortTokens[order] ?: return
        _sortOrder.value = order
        _comments.value = emptyList()
        _status.value = CommentsStatus.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            YouTube.commentsContinuation(token)
                .onSuccess { page ->
                    _comments.value = page.comments
                    continuation = page.continuation
                    _status.value = if (page.comments.isEmpty()) CommentsStatus.Empty else CommentsStatus.Loaded
                }
                .onFailure {
                    _status.value = CommentsStatus.Error
                }
        }
    }

    fun postComment(text: String, onPosted: () -> Unit) {
        val params = _createCommentParams.value ?: return
        if (text.isBlank() || _isPosting.value) return
        _isPosting.value = true
        _postError.value = false
        viewModelScope.launch {
            YouTube.postComment(text, params)
                .onSuccess {
                    _isPosting.value = false
                    onPosted()
                    currentVideoId?.let { loadComments(it, force = true) }
                }
                .onFailure {
                    _isPosting.value = false
                    _postError.value = true
                }
        }
    }
}
