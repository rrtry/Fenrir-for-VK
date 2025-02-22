package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.adapter.DocsAdapter
import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.search.DocsSearchPresenter
import dev.ragnarok.fenrir.mvp.view.search.IDocSearchView

class DocsSearchFragment :
    AbsSearchFragment<DocsSearchPresenter, IDocSearchView, Document, DocsAdapter>(),
    DocsAdapter.ActionListener, IDocSearchView {
    override fun setAdapterData(adapter: DocsAdapter, data: MutableList<Document>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Document>): DocsAdapter {
        val adapter = DocsAdapter(data)
        adapter.setActionListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DocsSearchPresenter> {
        return object : IPresenterFactory<DocsSearchPresenter> {
            override fun create(): DocsSearchPresenter {
                return DocsSearchPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelable(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    companion object {

        fun newInstance(
            accountId: Int,
            initialCriteria: DocumentSearchCriteria?
        ): DocsSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = DocsSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}