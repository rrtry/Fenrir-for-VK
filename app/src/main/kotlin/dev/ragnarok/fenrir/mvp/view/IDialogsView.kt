package dev.ragnarok.fenrir.mvp.view

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.model.Dialog
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.mvp.core.IMvpView
import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView

interface IDialogsView : IAccountDependencyView, IMvpView, IErrorView, IToastView {
    fun displayData(data: List<Dialog>, accountId: Int)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToChat(
        accountId: Int,
        messagesOwnerId: Int,
        peerId: Int,
        title: String?,
        ava_url: String?
    )

    fun goToSearch(accountId: Int)
    fun goToImportant(accountId: Int)
    fun showSnackbar(@StringRes res: Int, isLong: Boolean)
    fun askToReload()
    fun showEnterNewGroupChatTitle(users: List<User>)
    fun showNotificationSettings(accountId: Int, peerId: Int)
    fun goToOwnerWall(accountId: Int, ownerId: Int, owner: Owner?)
    fun setCreateGroupChatButtonVisible(visible: Boolean)
    fun notifyHasAttachments(has: Boolean)
    fun updateAccountIdNoRefresh(accountId: Int)

    interface IContextView {
        fun setCanDelete(can: Boolean)
        fun setCanAddToHomescreen(can: Boolean)
        fun setCanConfigNotifications(can: Boolean)
        fun setCanAddToShortcuts(can: Boolean)
        fun setIsHidden(can: Boolean)
        fun setCanRead(can: Boolean)
        fun setPinned(pinned: Boolean)
    }

    interface IOptionView {
        fun setCanSearch(can: Boolean)
    }
}