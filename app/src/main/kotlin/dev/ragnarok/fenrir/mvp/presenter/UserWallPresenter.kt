package dev.ragnarok.fenrir.mvp.presenter

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.domain.*
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.friends.FriendsTabsFragment
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.mvp.view.IUserWallView
import dev.ragnarok.fenrir.mvp.view.IWallView
import dev.ragnarok.fenrir.place.PlaceFactory.getMentionsPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.*
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.ShortcutUtils.createWallShortcutRx
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getRegistrationDate
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import java.io.File

class UserWallPresenter(
    accountId: Int,
    ownerId: Int,
    owner: User?,
    private val context: Context,
    savedInstanceState: Bundle?
) : AbsWallPresenter<IUserWallView>(accountId, ownerId, savedInstanceState) {
    private val filters: MutableList<PostFilter>
    private val ownersRepository: IOwnersRepository = owners
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val accountInteractor: IAccountsInteractor = InteractorFactory.createAccountInteractor()
    private val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val wallsRepository: IWallsRepository = walls
    private val uploadManager: IUploadManager = Includes.uploadManager
    var user: User
        private set
    private var details: UserDetails
    private var loadingAvatarPhotosNow = false
    override fun onRefresh() {
        requestActualFullInfo()
    }

    fun firePhotosSelected(localPhotos: ArrayList<LocalPhoto>?, file: String?, video: LocalVideo?) {
        when {
            file.nonNullNoEmpty() -> doUploadFile(file)
            localPhotos.nonNullNoEmpty() -> {
                doUploadPhotos(localPhotos)
            }
            video != null -> {
                doUploadVideo(video.getData().toString())
            }
        }
    }

    private fun doUploadFile(file: String) {
        for (i in Settings.get().other().photoExt()) {
            if (file.endsWith(i, true)) {
                Uri.fromFile(
                    File(
                        file
                    )
                )
                return
            }
        }
        for (i in Settings.get().other().videoExt()) {
            if (file.endsWith(i, true)) {
                doUploadFile(
                    file,
                    0,
                    true
                )
                return
            }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select)
            .setNegativeButton(R.string.video) { _: DialogInterface?, _: Int ->
                doUploadFile(
                    file,
                    0,
                    true
                )
            }
            .setPositiveButton(R.string.photo) { _: DialogInterface?, _: Int ->
                view?.doEditPhoto(
                    Uri.fromFile(
                        File(
                            file
                        )
                    )
                )
            }
            .create().show()
    }

    fun doUploadFile(file: String, size: Int, isVideo: Boolean) {
        val intents: List<UploadIntent> = if (isVideo) {
            UploadUtils.createIntents(
                accountId,
                UploadDestination.forStory(MessageMethod.VIDEO),
                file,
                size,
                true
            )
        } else {
            UploadUtils.createIntents(
                accountId,
                UploadDestination.forStory(MessageMethod.PHOTO),
                file,
                size,
                true
            )
        }
        uploadManager.enqueue(intents)
    }

    private fun doUploadVideo(file: String) {
        val intents = UploadUtils.createVideoIntents(
            accountId,
            UploadDestination.forStory(MessageMethod.VIDEO),
            file,
            true
        )
        uploadManager.enqueue(intents)
    }

    private fun doUploadPhotos(photos: List<LocalPhoto>) {
        if (photos.size == 1) {
            var to_up = photos[0].getFullImageUri() ?: return
            if (to_up.path?.let { File(it).isFile } == true) {
                to_up = Uri.fromFile(to_up.path?.let { File(it) })
            }
            view?.doEditPhoto(to_up)
            return
        }
        val intents = UploadUtils.createIntents(
            accountId,
            UploadDestination.forStory(MessageMethod.PHOTO),
            photos,
            Upload.IMAGE_SIZE_FULL,
            true
        )
        uploadManager.enqueue(intents)
    }

    private fun onUploadFinished(pair: Pair<Upload, UploadResult<*>>) {
        val destination = pair.first.destination
        if (destination.method == Method.PHOTO_TO_PROFILE && destination.ownerId == ownerId) {
            requestActualFullInfo()
            val post = pair.second.result as Post
            resumedView?.showAvatarUploadedMessage(
                accountId,
                post
            )
        } else if (destination.method == Method.STORY && Settings.get()
                .accounts().current == ownerId
        ) {
            fireRefresh()
        }
    }

    private fun resolveCounters() {
        view?.displayCounters(
            details.getFriendsCount(),
            details.getMutualFriendsCount(),
            details.getFollowersCount(),
            details.getGroupsCount(),
            details.getPhotosCount(),
            details.getAudiosCount(),
            details.getVideosCount(),
            details.getArticlesCount(),
            details.getProductsCount(),
            details.getGiftCount(),
            details.getProductServicesCount(),
            details.getNarrativesCount()
        )
    }

    private fun resolveBaseUserInfoViews() {
        view?.displayBaseUserInfo(user)
    }

    private fun refreshUserDetails() {
        val accountId = accountId
        appendDisposable(
            ownersRepository.getFullUserInfo(
                accountId,
                ownerId,
                IOwnersRepository.MODE_CACHE
            )
                .fromIOToMain()
                .subscribe({
                    onFullInfoReceived(it.first, it.second)
                    requestActualFullInfo()
                }, { t -> onDetailsGetError(t) })
        )
    }

    private fun requestActualFullInfo() {
        val accountId = accountId
        appendDisposable(ownersRepository.getFullUserInfo(
            accountId,
            ownerId,
            IOwnersRepository.MODE_NET
        )
            .fromIOToMain()
            .subscribe({
                onFullInfoReceived(
                    it.first,
                    it.second
                )
            }) { t -> onDetailsGetError(t) })
    }

    private fun onFullInfoReceived(user: User?, details: UserDetails?) {
        if (user != null) {
            this.user = user
            onUserInfoUpdated()
        }
        if (details != null) {
            this.details = details
            onUserDetalsUpdated()
        }
        resolveStatusView()
        resolveMenu()
    }

    private fun onUserDetalsUpdated() {
        syncFilterCountersWithDetails()
        view?.notifyWallFiltersChanged()
        resolvePrimaryActionButton()
        resolveCounters()
    }

    private fun onUserInfoUpdated() {
        resolveBaseUserInfoViews()
    }

    private fun onDetailsGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun syncFiltersWithSelectedMode() {
        for (filter in filters) {
            filter.setActive(filter.getMode() == wallFilter)
        }
    }

    private fun syncFilterCountersWithDetails() {
        for (filter in filters) {
            when (filter.getMode()) {
                WallCriteria.MODE_ALL -> filter.setCount(details.getAllWallCount())
                WallCriteria.MODE_OWNER -> filter.setCount(details.getOwnWallCount())
                WallCriteria.MODE_SCHEDULED -> filter.setCount(details.getPostponedWallCount())
            }
        }
    }

    override fun onGuiCreated(viewHost: IUserWallView) {
        super.onGuiCreated(viewHost)
        viewHost.displayWallFilters(filters)
        resolveCounters()
        resolveBaseUserInfoViews()
        resolvePrimaryActionButton()
        resolveStatusView()
        resolveMenu()
        resolveProgressDialogView()
    }

    private fun createPostFilters(): List<PostFilter> {
        val filters: MutableList<PostFilter> = ArrayList()
        filters.add(PostFilter(WallCriteria.MODE_ALL, getString(R.string.all_posts)))
        filters.add(PostFilter(WallCriteria.MODE_OWNER, getString(R.string.owner_s_posts)))
        if (isMyWall) {
            filters.add(PostFilter(WallCriteria.MODE_SCHEDULED, getString(R.string.scheduled)))
        }
        return filters
    }

    fun fireStatusClick() {
        details.getStatusAudio().requireNonNull {
            view?.playAudioList(
                accountId, 0, singletonArrayList(
                    it
                )
            )
        }
    }

    fun fireMoreInfoClick() {
        view?.openUserDetails(
            accountId,
            user,
            details
        )
    }

    fun fireFilterClick(entry: PostFilter) {
        if (changeWallFilter(entry.getMode())) {
            syncFiltersWithSelectedMode()
            view?.notifyWallFiltersChanged()
        }
    }

    fun fireHeaderPhotosClick() {
        view?.openPhotoAlbums(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderAudiosClick() {
        view?.openAudios(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderArticlesClick() {
        view?.openArticles(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderProductsClick() {
        view?.openProducts(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderProductServicesClick() {
        view?.openProductServices(
            accountId,
            ownerId
        )
    }

    fun fireHeaderGiftsClick() {
        view?.openGifts(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderFriendsClick() {
        view?.openFriends(
            accountId,
            ownerId,
            FriendsTabsFragment.TAB_ALL_FRIENDS,
            friendsCounters
        )
    }

    private val friendsCounters: FriendsCounters
        get() = FriendsCounters(
            details.getFriendsCount(),
            details.getOnlineFriendsCount(),
            details.getFollowersCount(),
            details.getMutualFriendsCount()
        )

    fun fireHeaderGroupsClick() {
        view?.openGroups(
            accountId,
            ownerId,
            user
        )
    }

    fun fireHeaderVideosClick() {
        view?.openVideosLibrary(
            accountId,
            ownerId,
            user
        )
    }

    @SuppressLint("ResourceType")
    private fun resolvePrimaryActionButton() {
        @StringRes var title: Int? = null
        if (accountId == ownerId) {
            title = R.string.edit_status
        } else {
            when (user.friendStatus) {
                VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND -> title = R.string.add_to_friends
                VKApiUser.FRIEND_STATUS_REQUEST_SENT -> title = R.string.cancel_request
                VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST -> title = R.string.accept_request
                VKApiUser.FRIEND_STATUS_IS_FRIEDND -> title = R.string.delete_from_friends
            }
            if (user.blacklisted_by_me) {
                title = R.string.is_to_blacklist
            }
        }
        val finalTitle = title
        view?.setupPrimaryActionButton(
            finalTitle
        )
    }

    fun firePrimaryActionsClick() {
        if (accountId == ownerId) {
            view?.showEditStatusDialog(user.status)
            return
        }
        if (user.blacklisted_by_me) {
            view?.showUnbanMessageDialog()
            return
        }
        when (user.friendStatus) {
            VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND -> view?.showAddToFriendsMessageDialog()
            VKApiUser.FRIEND_STATUS_REQUEST_SENT -> fireDeleteFromFriends()
            VKApiUser.FRIEND_STATUS_IS_FRIEDND -> view?.showDeleteFromFriendsMessageDialog()
            VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST -> executeAddToFriendsRequest(null, false)
        }
    }

    private fun DisplayUserProfileAlbum(photos: List<Photo>) {
        setLoadingAvatarPhotosNow(false)
        if (photos.isEmpty()) {
            view?.showSnackbar(
                R.string.no_photos_found,
                true
            )
            return
        }
        val currentAvatarPhotoId = details.getPhotoId()?.id
        val currentAvatarOwner_id = details.getPhotoId()?.ownerId
        var sel = 0
        if (currentAvatarPhotoId != null && currentAvatarOwner_id != null) {
            var ut = 0
            for (i in photos) {
                if (i.ownerId == currentAvatarOwner_id && i.getObjectId() == currentAvatarPhotoId) {
                    sel = ut
                    break
                }
                ut++
            }
        }
        val curr = sel
        view?.openPhotoAlbum(
            accountId,
            ownerId,
            -6,
            ArrayList(photos),
            curr
        )
    }

    private fun onAddFriendResult(resultCode: Int) {
        var strRes: Int? = null
        var newFriendStatus: Int? = null
        when (resultCode) {
            IRelationshipInteractor.FRIEND_ADD_REQUEST_SENT -> {
                strRes = R.string.friend_request_sent
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT
            }
            IRelationshipInteractor.FRIEND_ADD_REQUEST_FROM_USER_APPROVED -> {
                strRes = R.string.friend_request_from_user_approved
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_FRIEDND
            }
            IRelationshipInteractor.FRIEND_ADD_RESENDING -> {
                strRes = R.string.request_resending
                newFriendStatus = VKApiUser.FRIEND_STATUS_REQUEST_SENT
            }
        }
        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus)
        }
        if (strRes != null) {
            val finalStrRes: Int = strRes
            view?.showSnackbar(
                finalStrRes,
                true
            )
        }
        resolvePrimaryActionButton()
    }

    fun fireDeleteFromFriends() {
        val accountId = accountId
        appendDisposable(relationshipInteractor.deleteFriends(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ responseCode -> onFriendsDeleteResult(responseCode) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun fireNewStatusEntered(newValue: String?) {
        val accountId = accountId
        appendDisposable(accountInteractor.changeStatus(accountId, newValue)
            .fromIOToMain()
            .subscribe({ onStatusChanged(newValue) }) { t ->
                showError(
                    getCauseIfRuntime(t)
                )
            })
    }

    private fun onStatusChanged(status: String?) {
        user.setStatus(status)
        view?.showSnackbar(
            R.string.status_was_changed,
            true
        )
        resolveStatusView()
    }

    private fun resolveStatusView() {
        val statusText: String? = if (details.getStatusAudio() != null) {
            details.getStatusAudio()?.artistAndTitle
        } else {
            user.status
        }
        view?.displayUserStatus(
            statusText,
            details.getStatusAudio() != null
        )
    }

    private fun resolveMenu() {
        view?.InvalidateOptionsMenu()
    }

    fun fireAddToFrindsClick(message: String?) {
        executeAddToFriendsRequest(message, false)
    }

    fun fireAddToBookmarks() {
        val accountId = accountId
        appendDisposable(faveInteractor.addPage(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireRemoveFromBookmarks() {
        val accountId = accountId
        appendDisposable(faveInteractor.removePage(accountId, ownerId, true)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireSubscribe() {
        val accountId = accountId
        appendDisposable(wallsRepository.subscribe(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireUnSubscribe() {
        val accountId = accountId
        appendDisposable(wallsRepository.unsubscribe(accountId, ownerId)
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    private fun executeAddToFriendsRequest(text: String?, follow: Boolean) {
        val accountId = accountId
        appendDisposable(relationshipInteractor.addFriend(accountId, ownerId, text, follow)
            .fromIOToMain()
            .subscribe({ resultCode -> onAddFriendResult(resultCode) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun onFriendsDeleteResult(responseCode: Int) {
        var strRes: Int? = null
        var newFriendStatus: Int? = null
        when (responseCode) {
            IRelationshipInteractor.DeletedCodes.FRIEND_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_HAS_INPUT_REQUEST
                strRes = R.string.friend_deleted
            }
            IRelationshipInteractor.DeletedCodes.OUT_REQUEST_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.out_request_deleted
            }
            IRelationshipInteractor.DeletedCodes.IN_REQUEST_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.in_request_deleted
            }
            IRelationshipInteractor.DeletedCodes.SUGGESTION_DELETED -> {
                newFriendStatus = VKApiUser.FRIEND_STATUS_IS_NOT_FRIEDND
                strRes = R.string.suggestion_deleted
            }
        }
        if (newFriendStatus != null) {
            user.setFriendStatus(newFriendStatus)
        }
        if (strRes != null) {
            val finalStrRes: Int = strRes
            view?.showSnackbar(
                finalStrRes,
                true
            )
            resolvePrimaryActionButton()
        }
    }

    private fun prepareUserAvatarsAndShow() {
        setLoadingAvatarPhotosNow(true)
        val accountId = accountId
        appendDisposable(photosInteractor[accountId, ownerId, -6, 100, 0, true]
            .fromIOToMain()
            .subscribe({ photos -> DisplayUserProfileAlbum(photos) }) { t ->
                onAvatarAlbumPrepareFailed(
                    t
                )
            })
    }

    private fun onAvatarAlbumPrepareFailed(t: Throwable) {
        setLoadingAvatarPhotosNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun resolveProgressDialogView() {
        if (loadingAvatarPhotosNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.loading_owner_photo_album,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private fun setLoadingAvatarPhotosNow(loadingAvatarPhotosNow: Boolean) {
        this.loadingAvatarPhotosNow = loadingAvatarPhotosNow
        resolveProgressDialogView()
    }

    fun fireAvatarClick() {
        view?.showAvatarContextMenu(isMyWall)
    }

    fun fireAvatarLongClick() {
        view?.showMention(
            accountId,
            ownerId
        )
    }

    fun fireOpenAvatarsPhotoAlbum() {
        prepareUserAvatarsAndShow()
    }

    fun fireAddToBlacklistClick() {
        val accountId = accountId
        appendDisposable(InteractorFactory.createAccountInteractor()
            .banUsers(accountId, listOf(user))
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    fun fireMentions() {
        getMentionsPlace(accountId, ownerId).tryOpenWith(context)
    }

    override fun fireOptionViewCreated(view: IWallView.IOptionView) {
        super.fireOptionViewCreated(view)
        view.setIsBlacklistedByMe(user.blacklisted_by_me)
        view.setIsFavorite(details.isSetFavorite())
        view.setIsSubscribed(details.isSetSubscribed())
    }

    fun renameLocal(name: String?) {
        Settings.get().other().setUserNameChanges(ownerId, name)
        onUserInfoUpdated()
    }

    fun fireGetRegistrationDate() {
        getRegistrationDate(context, ownerId)
    }

    fun fireReport() {
        val values = arrayOf<CharSequence>("porn", "spam", "insult", "advertisement")
        val items = arrayOf<CharSequence>(
            "Порнография",
            "Спам, Мошенничество",
            "Оскорбительное поведение",
            "Рекламная страница"
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.report)
            .setItems(items) { dialog: DialogInterface, item: Int ->
                val report = values[item].toString()
                appendDisposable(ownersRepository.report(accountId, ownerId, report, null)
                    .fromIOToMain()
                    .subscribe({ p ->
                        if (p == 1) view?.customToast?.showToast(
                            R.string.success
                        )
                        else view?.customToast?.showToast(
                            R.string.error
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
                dialog.dismiss()
            }
            .show()
    }

    fun fireRemoveBlacklistClick() {
        val accountId = accountId
        appendDisposable(InteractorFactory.createAccountInteractor()
            .unbanUser(accountId, user.getObjectId())
            .fromIOToMain()
            .subscribe({ onExecuteComplete() }) { t -> onExecuteError(t) })
    }

    private fun onExecuteError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun onExecuteComplete() {
        onRefresh()
        view?.customToast?.showToast(R.string.success)
    }

    fun fireChatClick() {
        val accountId = accountId
        val peer = Peer(Peer.fromUserId(user.getObjectId()))
            .setAvaUrl(user.maxSquareAvatar)
            .setTitle(user.fullName)
        view?.openChatWith(
            accountId,
            accountId,
            peer
        )
    }

    fun fireNewAvatarPhotoSelected(file: String?) {
        val intent = UploadIntent(accountId, UploadDestination.forProfilePhoto(ownerId))
            .setAutoCommit(true)
            .setFileUri(Uri.parse(file))
            .setSize(Upload.IMAGE_SIZE_FULL)
        uploadManager.enqueue(listOf(intent))
    }

    override fun fireAddToShortcutClick() {
        appendDisposable(
            createWallShortcutRx(
                context,
                accountId,
                user.ownerId,
                user.fullName,
                user.maxSquareAvatar
            )
                .fromIOToMain().subscribe({
                    view?.showSnackbar(
                        R.string.success,
                        true
                    )
                }) { t ->
                    view?.showError(t.localizedMessage)
                })
    }

    override fun searchStory(ByName: Boolean) {
        appendDisposable(ownersRepository.searchStory(
            accountId,
            if (ByName) user.fullName else null,
            if (ByName) null else ownerId
        )
            .fromIOToMain()
            .subscribe({
                if (it.nonNullNoEmpty()) {
                    stories.clear()
                    stories.addAll(it)
                    view?.updateStory(
                        stories
                    )
                }
            }) { })
    }

    init {
        filters = ArrayList()
        filters.addAll(createPostFilters())
        user = owner ?: User(ownerId)
        details = UserDetails()
        syncFiltersWithSelectedMode()
        syncFilterCountersWithDetails()
        refreshUserDetails()
        appendDisposable(
            uploadManager.observeResults()
                .observeOn(provideMainThreadScheduler())
                .subscribe(
                    { pair: Pair<Upload, UploadResult<*>> -> onUploadFinished(pair) },
                    ignore()
                )
        )
    }
}