package dev.ragnarok.fenrir.mvp.view

import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.mvp.core.IMvpView
import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView

interface IWallPostView : IAttachmentsPlacesView, IAccountDependencyView, IMvpView, IErrorView,
    IToastView {
    fun displayDefaultToolbarTitle()
    fun displayToolbarTitle(title: String?)
    fun displayToolbarSubtitle(subtitleType: Int, datetime: Long)
    fun displayPostInfo(post: Post)
    fun displayLoading()
    fun displayLoadingFail()
    fun displayLikes(count: Int, userLikes: Boolean)
    fun setCommentButtonVisible(visible: Boolean)
    fun displayCommentCount(count: Int)
    fun displayReposts(count: Int, userReposted: Boolean)
    fun goToPostEditing(accountId: Int, post: Post)
    fun showPostNotReadyToast()
    fun copyLinkToClipboard(link: String?)
    fun showSuccessToast()
    fun copyTextToClipboard(text: String?)
    fun displayDefaultToolbarSubtitle()
    fun displayPinComplete(pin: Boolean)
    fun displayDeleteOrRestoreComplete(deleted: Boolean)
    fun goToNewsSearch(accountId: Int, hashTag: String?)
    interface IOptionView {
        fun setCanDelete(can: Boolean)
        fun setCanRestore(can: Boolean)
        fun setCanPin(can: Boolean)
        fun setCanUnpin(can: Boolean)
        fun setCanEdit(can: Boolean)
        fun setInFave(inTo: Boolean)
    }

    companion object {
        const val SUBTITLE_NORMAL = 1
        const val SUBTITLE_STATUS_UPDATE = 2
        const val SUBTITLE_PHOTO_UPDATE = 3
    }
}