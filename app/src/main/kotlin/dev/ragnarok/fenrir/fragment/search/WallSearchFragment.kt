package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.adapter.WallAdapter
import dev.ragnarok.fenrir.fragment.search.criteria.WallSearchCriteria
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.search.WallSearchPresenter
import dev.ragnarok.fenrir.mvp.view.search.IWallSearchView
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.Utils.isLandscape

class WallSearchFragment :
    AbsSearchFragment<WallSearchPresenter, IWallSearchView, Post, WallAdapter>(),
    IWallSearchView, WallAdapter.ClickListener {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<WallSearchPresenter> {
        return object : IPresenterFactory<WallSearchPresenter> {
            override fun create(): WallSearchPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val c: WallSearchCriteria? = requireArguments().getParcelable(Extra.CRITERIA)
                return WallSearchPresenter(accountId, c, saveInstanceState)
            }
        }
    }

    override fun setAdapterData(adapter: WallAdapter, data: MutableList<Post>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun onAvatarClick(ownerId: Int) {
        super.onOwnerClick(ownerId)
    }

    override fun createAdapter(data: MutableList<Post>): WallAdapter {
        return WallAdapter(requireActivity(), data, this, this)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val manager: RecyclerView.LayoutManager = if (is600dp(requireActivity())) {
            val land = isLandscape(requireActivity())
            StaggeredGridLayoutManager(if (land) 2 else 1, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        }
        return manager
    }

    override fun onOwnerClick(ownerId: Int) {
        presenter?.fireOwnerClick(
            ownerId
        )
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(
            post
        )
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostClick(
            post
        )
    }

    override fun onRestoreClick(post: Post) {}
    override fun onCommentsClick(post: Post) {
        presenter?.fireCommentsClick(
            post
        )
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireShowLikesClick(
            post
        )
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireShowCopiesClick(
            post
        )
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(
            post
        )
    }

    companion object {
        fun newInstance(accountId: Int, criteria: WallSearchCriteria?): WallSearchFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, criteria)
            val fragment = WallSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}