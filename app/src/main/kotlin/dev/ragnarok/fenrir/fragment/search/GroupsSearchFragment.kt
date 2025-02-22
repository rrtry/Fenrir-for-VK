package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.adapter.PeopleAdapter
import dev.ragnarok.fenrir.fragment.search.criteria.GroupSearchCriteria
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.search.CommunitiesSearchPresenter
import dev.ragnarok.fenrir.mvp.view.search.ICommunitiesSearchView
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace

class GroupsSearchFragment :
    AbsSearchFragment<CommunitiesSearchPresenter, ICommunitiesSearchView, Community, PeopleAdapter>(),
    ICommunitiesSearchView, PeopleAdapter.ClickListener {
    override fun setAdapterData(adapter: PeopleAdapter, data: MutableList<Community>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Community>): PeopleAdapter {
        val adapter = PeopleAdapter(requireActivity(), data)
        adapter.setClickListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunitiesSearchPresenter> {
        return object : IPresenterFactory<CommunitiesSearchPresenter> {
            override fun create(): CommunitiesSearchPresenter {
                return CommunitiesSearchPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelable(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    override fun onOwnerClick(owner: Owner) {
        presenter?.fireCommunityClick(
            owner as Community
        )
    }

    override fun openCommunityWall(accountId: Int, community: Community) {
        getOwnerWallPlace(accountId, community).tryOpenWith(requireActivity())
    }

    companion object {

        fun newInstance(
            accountId: Int,
            initialCriteria: GroupSearchCriteria?
        ): GroupsSearchFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            val fragment = GroupsSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}