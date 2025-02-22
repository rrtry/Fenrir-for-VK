package dev.ragnarok.fenrir.fragment.conversation

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.adapter.LinksAdapter
import dev.ragnarok.fenrir.adapter.LinksAdapter.LinkConversationListener
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.conversations.ChatAttachmentLinksPresenter
import dev.ragnarok.fenrir.mvp.view.conversations.IChatAttachmentLinksView

class ConversationLinksFragment :
    AbsChatAttachmentsFragment<Link, ChatAttachmentLinksPresenter, IChatAttachmentLinksView>(),
    LinksAdapter.ActionListener, LinkConversationListener, IChatAttachmentLinksView {
    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        val simpleDocRecycleAdapter = LinksAdapter(mutableListOf())
        simpleDocRecycleAdapter.setActionListener(this)
        simpleDocRecycleAdapter.setLinkConversationListener(this)
        return simpleDocRecycleAdapter
    }

    override fun displayAttachments(data: MutableList<Link>) {
        if (adapter is LinksAdapter) {
            (adapter as LinksAdapter).setItems(data)
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatAttachmentLinksPresenter> {
        return object : IPresenterFactory<ChatAttachmentLinksPresenter> {
            override fun create(): ChatAttachmentLinksPresenter {
                return ChatAttachmentLinksPresenter(
                    requireArguments().getInt(Extra.PEER_ID),
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onLinkClick(index: Int, doc: Link) {
        presenter?.fireLinkClick(
            doc
        )
    }

    override fun onGoLinkConversation(doc: Link) {
        presenter?.fireGoToMessagesLookup(
            doc.msgPeerId,
            doc.msgId
        )
    }
}