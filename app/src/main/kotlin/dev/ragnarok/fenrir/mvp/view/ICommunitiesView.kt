package dev.ragnarok.fenrir.mvp.view

import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.DataWrapper
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.mvp.core.IMvpView
import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView

interface ICommunitiesView : IAccountDependencyView, IMvpView, IErrorView {
    fun displayData(
        own: DataWrapper<Community>,
        filtered: DataWrapper<Community>,
        search: DataWrapper<Community>
    )

    fun notifyDataSetChanged()
    fun notifyOwnDataAdded(position: Int, count: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun showCommunityWall(accountId: Int, community: Community)
    fun notifySearchDataAdded(position: Int, count: Int)
    fun showCommunityMenu(community: Community)
    fun showModCommunities(add: List<Owner>, remove: List<Owner>, accountId: Int, ownerId: Int)
}