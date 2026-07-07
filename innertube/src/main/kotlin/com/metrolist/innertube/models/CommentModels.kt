package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

/**
 * One entry inside a comments-continuation `continuationItems[]` array.
 * First page's first item is [commentsHeaderRenderer]; the rest are threads,
 * with a trailing [continuationItemRenderer] for "load more".
 */
@Serializable
data class CommentsContinuationItem(
    val commentsHeaderRenderer: CommentsHeaderRenderer? = null,
    val commentThreadRenderer: CommentThreadRenderer? = null,
    val continuationItemRenderer: ContinuationItemRenderer? = null,
)

@Serializable
data class CommentsHeaderRenderer(
    val countText: Runs? = null,
    val sortMenu: SortMenu? = null,
    val createRenderer: CreateRenderer? = null,
) {
    @Serializable
    data class SortMenu(
        val sortFilterSubMenuRenderer: SortFilterSubMenuRenderer? = null,
    ) {
        @Serializable
        data class SortFilterSubMenuRenderer(
            val subMenuItems: List<SubMenuItem>? = null,
        ) {
            @Serializable
            data class SubMenuItem(
                val title: String? = null,
                val serviceEndpoint: ServiceEndpoint? = null,
            ) {
                @Serializable
                data class ServiceEndpoint(
                    val continuationCommand: ContinuationItemRenderer.ContinuationEndpoint.ContinuationCommand? = null,
                )
            }
        }
    }

    @Serializable
    data class CreateRenderer(
        val commentSimpleboxRenderer: CommentSimpleboxRenderer? = null,
    ) {
        @Serializable
        data class CommentSimpleboxRenderer(
            val submitButton: SubmitButton? = null,
        ) {
            @Serializable
            data class SubmitButton(
                val buttonRenderer: ButtonRenderer? = null,
            ) {
                @Serializable
                data class ButtonRenderer(
                    val serviceEndpoint: ServiceEndpoint? = null,
                ) {
                    @Serializable
                    data class ServiceEndpoint(
                        val createCommentEndpoint: CreateCommentEndpoint? = null,
                    ) {
                        @Serializable
                        data class CreateCommentEndpoint(
                            val createCommentParams: String? = null,
                        )
                    }
                }
            }
        }
    }
}

@Serializable
data class CommentThreadRenderer(
    val commentViewModel: CommentViewModelWrapper? = null,
    val comment: LegacyComment? = null,
    val replies: Replies? = null,
) {
    @Serializable
    data class CommentViewModelWrapper(
        val commentViewModel: CommentViewModel? = null,
    ) {
        @Serializable
        data class CommentViewModel(
            val commentKey: String? = null,
        )
    }

    @Serializable
    data class LegacyComment(
        val commentRenderer: CommentRenderer? = null,
    )

    @Serializable
    data class CommentRenderer(
        val commentId: String? = null,
        val authorText: Runs? = null,
        val authorThumbnail: AuthorThumbnail? = null,
        val contentText: Runs? = null,
        val publishedTimeText: Runs? = null,
        val voteCount: Runs? = null,
        val replyCount: Int? = null,
    ) {
        @Serializable
        data class AuthorThumbnail(
            val thumbnails: List<Thumbnail>? = null,
        )
    }

    @Serializable
    data class Replies(
        val commentRepliesRenderer: CommentRepliesRenderer? = null,
    ) {
        @Serializable
        data class CommentRepliesRenderer(
            val contents: List<ContinuationItemWrapper>? = null,
        ) {
            @Serializable
            data class ContinuationItemWrapper(
                val continuationItemRenderer: ContinuationItemRenderer? = null,
            )
        }
    }
}

/** Modern per-comment data, delivered separately from the thread and matched by entity key. */
@Serializable
data class FrameworkUpdates(
    val entityBatchUpdate: EntityBatchUpdate? = null,
) {
    @Serializable
    data class EntityBatchUpdate(
        val mutations: List<Mutation>? = null,
    ) {
        @Serializable
        data class Mutation(
            val entityKey: String? = null,
            val payload: Payload? = null,
        ) {
            @Serializable
            data class Payload(
                val commentEntityPayload: CommentEntityPayload? = null,
            )
        }
    }
}

@Serializable
data class CommentEntityPayload(
    val properties: Properties? = null,
    val author: Author? = null,
    val toolbar: Toolbar? = null,
) {
    @Serializable
    data class Properties(
        val commentId: String? = null,
        val content: Content? = null,
        val publishedTime: String? = null,
    ) {
        @Serializable
        data class Content(
            val content: String? = null,
        )
    }

    @Serializable
    data class Author(
        val displayName: String? = null,
        val channelId: String? = null,
        val avatarThumbnailUrl: String? = null,
        val isCreator: Boolean? = null,
        val isVerified: Boolean? = null,
    )

    @Serializable
    data class Toolbar(
        val likeCountLiked: String? = null,
        val likeCountNotliked: String? = null,
        val likeCountA11y: String? = null,
        val replyCount: String? = null,
    )
}

@Serializable
data class OnResponseReceivedEndpoint(
    val reloadContinuationItemsCommand: ContinuationItemsCommand? = null,
    val appendContinuationItemsAction: ContinuationItemsCommand? = null,
) {
    @Serializable
    data class ContinuationItemsCommand(
        val continuationItems: List<CommentsContinuationItem>? = null,
    )
}
