package dev.ragnarok.fenrir.mvp.presenter.conversations

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.mvp.view.conversations.IChatAttachmentAudiosView
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

class ChatAttachmentAudioPresenter(peerId: Int, accountId: Int, savedInstanceState: Bundle?) :
    BaseChatAttachmentsPresenter<Audio, IChatAttachmentAudiosView>(
        peerId,
        accountId,
        savedInstanceState
    ) {
    override fun onDataChanged() {
        super.onDataChanged()
        resolveToolbar()
    }

    override fun requestAttachments(
        peerId: Int,
        nextFrom: String?
    ): Single<Pair<String?, List<Audio>>> {
        return get().vkDefault(accountId)
            .messages()
            .getHistoryAttachments(peerId, "audio", nextFrom, 0, 50, null)
            .map { response ->
                val audios: MutableList<Audio> = ArrayList(
                    Utils.safeCountOf(response.items)
                )
                response.items.nonNullNoEmpty {
                    for (one in it) {
                        if (one.entry != null && one.entry?.attachment is VKApiAudio) {
                            val dto = one.entry?.attachment as VKApiAudio
                            audios.add(Dto2Model.transform(dto))
                        }
                    }
                }
                create(response.next_from, audios)
            }
    }

    fun fireAudioPlayClick(position: Int) {
        fireAudioPlayClick(position, ArrayList(data))
    }

    override fun onGuiCreated(viewHost: IChatAttachmentAudiosView) {
        super.onGuiCreated(viewHost)
        resolveToolbar()
    }

    private fun resolveToolbar() {
        view?.setToolbarTitleString(getString(R.string.attachments_in_chat))
        view?.setToolbarTitleString(getString(R.string.audios_count, Utils.safeCountOf(data)))
    }
}