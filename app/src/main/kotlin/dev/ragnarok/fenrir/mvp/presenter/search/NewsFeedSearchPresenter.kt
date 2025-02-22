package dev.ragnarok.fenrir.mvp.presenter.search

import android.os.Bundle
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.StringNextFrom
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.mvp.view.search.INewsFeedSearchView
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.core.Single

class NewsFeedSearchPresenter(
    accountId: Int,
    criteria: NewsFeedCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<INewsFeedSearchView, NewsFeedCriteria, Post, StringNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val feedInteractor: IFeedInteractor = InteractorFactory.createFeedInteractor()
    private val walls: IWallsRepository = Repository.walls

    override val initialNextFrom: StringNextFrom
        get() = StringNextFrom(null)

    override fun isAtLast(startFrom: StringNextFrom): Boolean {
        return startFrom.nextFrom.isNullOrEmpty()
    }

    override fun doSearch(
        accountId: Int,
        criteria: NewsFeedCriteria,
        startFrom: StringNextFrom
    ): Single<Pair<List<Post>, StringNextFrom>> {
        return feedInteractor.search(accountId, criteria, 50, startFrom.nextFrom)
            .map {
                create(
                    it.first,
                    StringNextFrom(it.second)
                )
            }
    }

    override fun instantiateEmptyCriteria(): NewsFeedCriteria {
        return NewsFeedCriteria("")
    }

    override fun firePostClick(post: Post) {
        if (post.postType == VKApiPost.Type.REPLY) {
            view?.openComments(
                accountId,
                Commented.from(post),
                post.vkid
            )
        } else {
            view?.openPost(
                accountId,
                post
            )
        }
    }

    override fun canSearch(criteria: NewsFeedCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty() || (criteria?.findOptionByKey(
            NewsFeedCriteria.KEY_GPS
        ) as SimpleGPSOption?)?.has() == true
    }

    fun fireLikeClick(post: Post) {
        val accountId = accountId
        appendDisposable(walls.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }
}