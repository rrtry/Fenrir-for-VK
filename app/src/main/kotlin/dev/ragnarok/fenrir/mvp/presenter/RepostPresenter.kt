package dev.ragnarok.fenrir.mvp.presenter

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.mvp.view.IRepostView

class RepostPresenter(
    accountId: Int, private val post: Post,
    private val targetGroupId: Int?, savedInstanceState: Bundle?
) :
    AbsAttachmentsEditPresenter<IRepostView>(accountId, savedInstanceState) {
    private val walls: IWallsRepository = Repository.walls
    private var publishingNow = false
    override fun onGuiCreated(viewHost: IRepostView) {
        super.onGuiCreated(viewHost)
        viewHost.setSupportedButtons(
            photo = false,
            audio = false,
            video = false,
            doc = false,
            poll = false,
            timer = false
        )
        resolveProgressDialog()
    }

    private fun resolveProgressDialog() {
        if (publishingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.publication,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private fun setPublishingNow(publishingNow: Boolean) {
        this.publishingNow = publishingNow
        resolveProgressDialog()
    }

    private fun onPublishError(throwable: Throwable) {
        setPublishingNow(false)
        showError(throwable)
    }

    private fun onPublishComplete() {
        setPublishingNow(false)
        view?.goBack()
    }

    fun fireReadyClick() {
        setPublishingNow(true)
        val accountId = accountId
        val body = getTextBody()
        appendDisposable(walls.repost(accountId, post.vkid, post.ownerId, targetGroupId, body)
            .fromIOToMain()
            .subscribe({ onPublishComplete() }) { throwable ->
                onPublishError(
                    throwable
                )
            })
    }

    init {
        data.add(AttachmentEntry(false, post))
    }
}