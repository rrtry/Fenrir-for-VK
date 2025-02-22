package dev.ragnarok.fenrir.mvp.presenter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.mvp.presenter.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.mvp.view.IWallView
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.checkEditInfo
import dev.ragnarok.fenrir.util.Utils.findIndexByPredicate
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.Utils.generateQR
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.intValueNotIn
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Boolean.compare
import java.util.*
import kotlin.math.abs

abstract class AbsWallPresenter<V : IWallView> internal constructor(
    accountId: Int,
    val ownerId: Int,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState) {
    protected val wall: MutableList<Post>

    val stories: MutableList<Story>
    private val ownersRepository: IOwnersRepository
    private val walls: IWallsRepository
    private val cacheCompositeDisposable = CompositeDisposable()
    private val netCompositeDisposable = CompositeDisposable()
    protected var endOfContent = false
    var wallFilter: Int
        private set
    private var requestNow = false
    private var nowRequestOffset = 0
    private var nextOffset = 0
    private var actualDataReady = false
    open fun searchStory(ByName: Boolean) {
        throw IllegalArgumentException("Unknown story search")
    }

    fun fireNarrativesClick() {
        view?.goNarratives(accountId, ownerId)
    }

    private fun onPostInvalid(postVkid: Int) {
        val index = findIndexByPredicate(
            wall
        ) {
            it.vkid == postVkid
        }
        if (index != -1) {
            wall.removeAt(index)
            view?.notifyWallItemRemoved(index)
        }
    }

    private fun onPostChange(post: Post) {
        val found =
            findInfoByPredicate(wall) {
                it.vkid == post.vkid
            }
        if (!isMatchFilter(post, wallFilter)) {
            // например, при публикации предложенной записи. Надо ли оно тут ?

            /*if (found != null) {
                int index = found.getFirst();
                wall.remove(index);

                if(isGuiReady()){
                    callView(v -> v.notifyWallItemRemoved(index);
                }
            }*/
            return
        }
        if (found != null) {
            val index = found.first
            wall[index] = post
            view?.notifyWallItemChanged(index)
        } else {
            val targetIndex: Int = if (!post.isPinned && wall.size > 0 && wall[0].isPinned) {
                1
            } else {
                0
            }
            wall.add(targetIndex, post)
            view?.notifyWallDataAdded(targetIndex, 1)
        }
    }

    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayWallData(wall)
        viewHost.updateStory(stories)
        resolveLoadMoreFooterView()
    }

    private fun loadWallCachedData() {
        val accountId = accountId
        cacheCompositeDisposable.add(walls.getCachedWall(accountId, ownerId, wallFilter)
            .fromIOToMain()
            .subscribe({ posts -> onCachedDataReceived(posts) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(posts: List<Post>) {
        wall.clear()
        wall.addAll(posts)
        actualDataReady = false
        view?.notifyWallDataSetChanged()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    override fun onDestroyed() {
        cacheCompositeDisposable.dispose()
        super.onDestroyed()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(requestNow && nowRequestOffset == 0)
    }

    private fun safeNotifyWallDataSetChanged() {
        view?.notifyWallDataSetChanged()
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
        resolveLoadMoreFooterView()
    }

    private fun setNowLoadingOffset(offset: Int) {
        nowRequestOffset = offset
    }

    private fun requestWall(offset: Int) {
        setNowLoadingOffset(offset)
        setRequestNow(true)
        val accountId = accountId
        val nextOffset = offset + COUNT
        val append = offset > 0
        netCompositeDisposable.add(walls.getWall(accountId, ownerId, offset, COUNT, wallFilter)
            .fromIOToMain()
            .subscribe({
                onActualDataReceived(
                    nextOffset,
                    it,
                    append
                )
            }) { throwable -> onActualDataGetError(throwable) })
    }

    private fun onActualDataGetError(throwable: Throwable) {
        setRequestNow(false)
        showError(getCauseIfRuntime(throwable))
    }

    private fun isExist(post: Post): Boolean {
        for (i in wall) {
            if (i.ownerId == post.ownerId && i.vkid == post.vkid) return true
        }
        return false
    }

    private fun addAll(posts: List<Post>): Int {
        var s = 0
        for (i in posts) {
            if (!isExist(i)) {
                wall.add(i)
                s++
            }
        }
        return s
    }

    private fun onActualDataReceived(nextOffset: Int, posts: List<Post>, append: Boolean) {
        cacheCompositeDisposable.clear()
        actualDataReady = true
        this.nextOffset = nextOffset
        endOfContent = posts.isEmpty()
        if (posts.nonNullNoEmpty()) {
            if (append) {
                val sizeBefore = wall.size
                val sz = addAll(posts)
                view?.notifyWallDataAdded(sizeBefore, sz)
            } else {
                wall.clear()
                addAll(posts)
                view?.notifyWallDataSetChanged()
            }
        }
        setRequestNow(false)
    }

    private fun resolveLoadMoreFooterView() {
        @LoadMoreState val state: Int = if (requestNow) {
            if (nowRequestOffset == 0) {
                LoadMoreState.INVISIBLE
            } else {
                LoadMoreState.LOADING
            }
        } else if (endOfContent) {
            LoadMoreState.END_OF_LIST
        } else {
            LoadMoreState.CAN_LOAD_MORE
        }
        view?.setupLoadMoreFooter(state)
    }

    private fun canLoadMore(): Boolean {
        return !endOfContent && actualDataReady && wall.nonNullNoEmpty() && !requestNow
    }

    private fun requestNext() {
        requestWall(nextOffset)
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    fun fireLoadMoreClick() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    open fun fireOptionViewCreated(view: IWallView.IOptionView) {
        view.setIsMy(accountId == ownerId)
        view.typeOwnerId(ownerId)
    }

    fun fireCreateClick() {
        view?.goToPostCreation(
            accountId,
            ownerId,
            EditingPostType.DRAFT
        )
    }

    private fun fireEdit(context: Context, p: VKApiProfileInfo) {
        val root = View.inflate(context, R.layout.entry_info, null)
        (root.findViewById<View>(R.id.edit_first_name) as TextInputEditText).setText(p.first_name)
        (root.findViewById<View>(R.id.edit_last_name) as TextInputEditText).setText(p.last_name)
        (root.findViewById<View>(R.id.edit_maiden_name) as TextInputEditText).setText(p.maiden_name)
        (root.findViewById<View>(R.id.edit_screen_name) as TextInputEditText).setText(p.screen_name)
        (root.findViewById<View>(R.id.edit_bdate) as TextInputEditText).setText(p.bdate)
        (root.findViewById<View>(R.id.edit_home_town) as TextInputEditText).setText(p.home_town)
        (root.findViewById<View>(R.id.sex) as Spinner).setSelection(p.sex - 1)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                appendDisposable(InteractorFactory.createAccountInteractor().saveProfileInfo(
                    accountId,
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_first_name) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.first_name
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_last_name) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.last_name
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_maiden_name) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.maiden_name
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_screen_name) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.screen_name
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_bdate) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.bdate
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.edit_home_town) as TextInputEditText).editableText.toString()
                            .trim { it <= ' ' }, p.home_town
                    ),
                    checkEditInfo(
                        (root.findViewById<View>(R.id.sex) as Spinner).selectedItemPosition + 1,
                        p.sex
                    )
                )
                    .fromIOToMain()
                    .subscribe({ t ->
                        when (t) {
                            0 -> createCustomToast(context).showToastError(R.string.not_changed)
                            1 -> createCustomToast(context).showToastSuccessBottom(R.string.success)
                            2 -> createCustomToast(context).showToastBottom(R.string.later)
                        }
                    }) { t ->
                        showError(t)
                    })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    fun fireEdit(context: Context) {
        appendDisposable(InteractorFactory.createAccountInteractor().getProfileInfo(accountId)
            .fromIOToMain()
            .subscribe({ t -> fireEdit(context, t) }) { })
    }

    fun fireToggleMonitor() {
        if (Settings.get().other().isOwnerInChangesMonitor(ownerId)) {
            Settings.get().other().removeOwnerInChangesMonitor(ownerId)
        } else {
            Settings.get().other().putOwnerInChangesMonitor(ownerId)
        }
    }

    fun fireRefresh() {
        netCompositeDisposable.clear()
        cacheCompositeDisposable.clear()
        requestWall(0)
        if (!Settings.get().other().isDisable_history) {
            appendDisposable(ownersRepository.getStory(
                accountId,
                if (accountId == ownerId) null else ownerId
            )
                .fromIOToMain()
                .subscribe({
                    if (it.nonNullNoEmpty()) {
                        stories.clear()
                        stories.addAll(it)
                        view?.updateStory(stories)
                    }
                }) { })
        }
        onRefresh()
    }

    @Suppress("DEPRECATION")
    fun fireShowQR(context: Context) {
        val qr = generateQR(
            "https://vk.com/" + (if (ownerId < 0) "club" else "id") + abs(
                ownerId
            ), context
        )
        val view = LayoutInflater.from(context).inflate(R.layout.qr, null)
        val imageView: ShapeableImageView = view.findViewById(R.id.qr)
        imageView.setImageBitmap(qr)
        MaterialAlertDialogBuilder(context)
            .setCancelable(true)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
                val path = Environment.getExternalStorageDirectory().absolutePath
                val fOutputStream: OutputStream
                val file = File(
                    path, "qr_fenrir_" + (if (ownerId < 0) "club" else "id") + abs(
                        ownerId
                    ) + ".png"
                )
                try {
                    fOutputStream = FileOutputStream(file)
                    qr?.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream)
                    fOutputStream.flush()
                    fOutputStream.close()
                    context.sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)
                        )
                    )
                    createCustomToast(context).showToast(R.string.success)
                } catch (e: IOException) {
                    e.printStackTrace()
                    createCustomToast(context).showToastError("Save Failed")
                }
            }
            .setIcon(R.drawable.qr_code)
            .setTitle(R.string.show_qr)
            .setView(view)
            .show()
    }

    protected open fun onRefresh() {}
    fun firePostBodyClick(post: Post) {
        if (intValueIn(post.postType, VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            view?.openPostEditor(accountId, post)
            return
        }
        firePostClick(post)
    }

    fun firePostRestoreClick(post: Post) {
        appendDisposable(walls.restore(accountId, post.ownerId, post.vkid)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireLikeLongClick(post: Post) {
        view?.goToLikes(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireShareLongClick(post: Post) {
        view?.goToReposts(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireLikeClick(post: Post) {
        if (Settings.get().other().isDisable_likes || isHiddenAccount(accountId)) {
            return
        }
        val accountId = accountId
        appendDisposable(walls.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }

    fun changeWallFilter(mode: Int): Boolean {
        val changed = mode != wallFilter
        wallFilter = mode
        if (changed) {
            cacheCompositeDisposable.clear()
            netCompositeDisposable.clear()
            loadWallCachedData()
            requestWall(0)
        }
        return changed
    }

    val isMyWall: Boolean
        get() = accountId == ownerId

    private fun onPostChange(update: PostUpdate) {
        val pinStateChanged = update.pinUpdate != null
        val index = findByVkid(update.ownerId, update.postId)
        if (index != -1) {
            val post = wall[index]
            update.likeUpdate.requireNonNull {
                post.setLikesCount(it.count)
                post.setUserLikes(it.isLiked)
            }
            update.deleteUpdate.requireNonNull {
                post.setDeleted(it.isDeleted)
            }
            update.pinUpdate.requireNonNull {
                for (p in wall) {
                    p.setPinned(false)
                }
                post.setPinned(it.isPinned)
            }
            if (pinStateChanged) {
                Collections.sort(wall, COMPARATOR)
                safeNotifyWallDataSetChanged()
            } else {
                view?.notifyWallItemChanged(index)
            }
        }
    }

    private fun findByVkid(ownerId: Int, vkid: Int): Int {
        return indexOf(
            wall
        ) {
            it.ownerId == ownerId && it.vkid == vkid
        }
    }

    fun fireCopyUrlClick() {
        view?.copyToClipboard(
            getString(R.string.link),
            "https://vk.com/" + (if (isCommunity) "club" else "id") + abs(
                ownerId
            )
        )
    }

    fun fireCopyIdClick() {
        view?.copyToClipboard(
            getString(R.string.id),
            ownerId.toString()
        )
    }

    abstract fun fireAddToShortcutClick()
    private val isCommunity: Boolean
        get() = ownerId < 0

    fun fireSearchClick() {
        view?.goToWallSearch(accountId, ownerId)
    }

    fun openConversationAttachments() {
        view?.goToConversationAttachments(accountId, ownerId)
    }

    fun fireButtonRemoveClick(post: Post) {
        appendDisposable(walls.delete(accountId, ownerId, post.vkid)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    companion object {
        private const val COUNT = 20
        private val COMPARATOR = Comparator { rhs: Post, lhs: Post ->
            if (rhs.isPinned == lhs.isPinned) {
                return@Comparator lhs.vkid.compareTo(rhs.vkid)
            }
            compare(lhs.isPinned, rhs.isPinned)
        }

        private fun isMatchFilter(post: Post, filter: Int): Boolean {
            when (filter) {
                WallCriteria.MODE_ALL -> return intValueNotIn(
                    post.postType,
                    VKApiPost.Type.POSTPONE,
                    VKApiPost.Type.SUGGEST
                )
                WallCriteria.MODE_OWNER -> return (post.authorId == post.ownerId
                        && intValueNotIn(
                    post.postType,
                    VKApiPost.Type.POSTPONE,
                    VKApiPost.Type.SUGGEST
                ))
                WallCriteria.MODE_SCHEDULED -> return post.postType == VKApiPost.Type.POSTPONE
                WallCriteria.MODE_SUGGEST -> return post.postType == VKApiPost.Type.SUGGEST
            }
            throw IllegalArgumentException("Unknown filter")
        }
    }

    init {
        wall = ArrayList()
        stories = ArrayList()
        wallFilter = WallCriteria.MODE_ALL
        walls = Repository.walls
        ownersRepository = owners
        loadWallCachedData()
        requestWall(0)
        if (!Settings.get().other().isDisable_history) {
            appendDisposable(ownersRepository.getStory(
                accountId,
                if (accountId == ownerId) null else ownerId
            )
                .fromIOToMain()
                .subscribe({
                    if (it.nonNullNoEmpty()) {
                        stories.clear()
                        stories.addAll(it)
                        view?.updateStory(stories)
                    }
                }) { })
        }
        appendDisposable(walls
            .observeMinorChanges()
            .filter { it.accountId == accountId && it.ownerId == ownerId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostChange(it) })
        appendDisposable(walls
            .observeChanges()
            .filter { it.ownerId == ownerId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostChange(it) })
        appendDisposable(walls
            .observePostInvalidation()
            .filter { it.ownerId == ownerId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostInvalid(it.id) })
    }
}