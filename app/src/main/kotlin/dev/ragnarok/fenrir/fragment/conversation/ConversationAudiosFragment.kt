package dev.ragnarok.fenrir.fragment.conversation

import android.Manifest
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.adapter.AudioRecyclerAdapter
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.conversations.ChatAttachmentAudioPresenter
import dev.ragnarok.fenrir.mvp.view.conversations.IChatAttachmentAudiosView
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class ConversationAudiosFragment :
    AbsChatAttachmentsFragment<Audio, ChatAttachmentAudioPresenter, IChatAttachmentAudiosView>(),
    AudioRecyclerAdapter.ClickListener, IChatAttachmentAudiosView {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        val audioRecyclerAdapter =
            AudioRecyclerAdapter(
                requireActivity(), mutableListOf(),
                not_show_my = false,
                iSSelectMode = false,
                iCatalogBlock = 0,
                playlist_id = null
            )
        audioRecyclerAdapter.setClickListener(this)
        return audioRecyclerAdapter
    }

    override fun onClick(position: Int, catalog: Int, audio: Audio) {
        presenter?.fireAudioPlayClick(
            position
        )
    }

    override fun onEdit(position: Int, audio: Audio) {}
    override fun onDelete(position: Int) {}
    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    override fun displayAttachments(data: MutableList<Audio>) {
        (adapter as AudioRecyclerAdapter?)?.setData(data)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatAttachmentAudioPresenter> {
        return object : IPresenterFactory<ChatAttachmentAudioPresenter> {
            override fun create(): ChatAttachmentAudioPresenter {
                return ChatAttachmentAudioPresenter(
                    requireArguments().getInt(Extra.PEER_ID),
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }

        }
    }
}