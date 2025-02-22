package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.adapter.VideosAdapter
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.search.VideosSearchPresenter
import dev.ragnarok.fenrir.mvp.view.search.IVideosSearchView

class VideoSearchFragment :
    AbsSearchFragment<VideosSearchPresenter, IVideosSearchView, Video, VideosAdapter>(),
    VideosAdapter.VideoOnClickListener {
    override fun setAdapterData(adapter: VideosAdapter, data: MutableList<Video>) {
        adapter.setData(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Video>): VideosAdapter {
        val adapter = VideosAdapter(requireActivity(), data)
        adapter.setVideoOnClickListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        val columns = resources.getInteger(R.integer.videos_column_count)
        return StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onVideoClick(position: Int, video: Video) {
        presenter?.fireVideoClick(
            video
        )
    }

    override fun onVideoLongClick(position: Int, video: Video): Boolean {
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideosSearchPresenter> {
        return object : IPresenterFactory<VideosSearchPresenter> {
            override fun create(): VideosSearchPresenter {
                return VideosSearchPresenter(
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
            initialCriteria: VideoSearchCriteria?
        ): VideoSearchFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            val fragment = VideoSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}