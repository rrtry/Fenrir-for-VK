package dev.ragnarok.fenrir.mvp.view

import android.net.Uri
import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.model.*

interface IUserWallView : IWallView, IProgressView {
    fun displayWallFilters(filters: MutableList<PostFilter>)
    fun notifyWallFiltersChanged()
    fun setupPrimaryActionButton(@DrawableRes resourceId: Int?)
    fun openFriends(accountId: Int, userId: Int, tab: Int, counters: FriendsCounters?)
    fun openGroups(accountId: Int, userId: Int, user: User?)
    fun openProducts(accountId: Int, ownerId: Int, owner: Owner?)
    fun openProductServices(accountId: Int, ownerId: Int)
    fun openGifts(accountId: Int, ownerId: Int, owner: Owner?)
    fun showEditStatusDialog(initialValue: String?)
    fun showAddToFriendsMessageDialog()
    fun showDeleteFromFriendsMessageDialog()
    fun showUnbanMessageDialog()
    fun showAvatarContextMenu(canUploadAvatar: Boolean)
    fun showMention(accountId: Int, ownerId: Int)
    fun displayCounters(
        friends: Int,
        mutual: Int,
        followers: Int,
        groups: Int,
        photos: Int,
        audios: Int,
        videos: Int,
        articles: Int,
        products: Int,
        gifts: Int,
        products_services: Int,
        narratives: Int
    )

    fun displayUserStatus(statusText: String?, swAudioIcon: Boolean)
    fun InvalidateOptionsMenu()
    fun displayBaseUserInfo(user: User)
    fun openUserDetails(accountId: Int, user: User, details: UserDetails)
    fun showAvatarUploadedMessage(accountId: Int, post: Post)
    fun doEditPhoto(uri: Uri)
}