package dev.ragnarok.fenrir.mvp.presenter

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Gift
import dev.ragnarok.fenrir.mvp.presenter.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.mvp.view.IGiftsView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class GiftsPresenter(accountId: Int, private val owner_id: Int, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IGiftsView>(accountId, savedInstanceState) {
    private val ownersRepository: IOwnersRepository = owners
    private val mGifts: ArrayList<Gift> = ArrayList()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(netLoadingNow)
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        val accountId = accountId
        netDisposable.add(ownersRepository.getGifts(accountId, owner_id, COUNT_PER_REQUEST, offset)
            .fromIOToMain()
            .subscribe({ gifts ->
                onNetDataReceived(
                    offset,
                    gifts
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(offset: Int, gifts: List<Gift>) {
        cacheLoadingNow = false
        mEndOfContent = gifts.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mGifts.clear()
            mGifts.addAll(gifts)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mGifts.size
            mGifts.addAll(gifts)
            view?.notifyDataAdded(
                startSize,
                gifts.size
            )
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mGifts.size)
    }

    override fun onGuiCreated(viewHost: IGiftsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mGifts)
        resolveRefreshingView()
    }

    private fun canLoadMore(): Boolean {
        return mGifts.isNotEmpty() && !cacheLoadingNow && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    fun fireOpenWall(ownerId: Int) {
        view?.onOpenWall(accountId, ownerId)
    }

    companion object {
        private const val COUNT_PER_REQUEST = 25
    }

    init {
        requestAtLast()
    }
}