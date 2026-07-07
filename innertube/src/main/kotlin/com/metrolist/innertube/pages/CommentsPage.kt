package com.metrolist.innertube.pages

enum class CommentSortOrder {
    TOP,
    NEWEST,
}

data class CommentItem(
    val id: String,
    val authorName: String,
    val authorThumbnailUrl: String?,
    val authorChannelId: String?,
    val isAuthorCreator: Boolean,
    val text: String,
    val publishedTimeText: String?,
    val likeCountText: String?,
    val replyCount: Int,
)

data class CommentsPage(
    val comments: List<CommentItem>,
    val continuation: String?,
    val commentsCountText: String? = null,
    val sortTokens: Map<CommentSortOrder, String> = emptyMap(),
    val createCommentParams: String? = null,
)
