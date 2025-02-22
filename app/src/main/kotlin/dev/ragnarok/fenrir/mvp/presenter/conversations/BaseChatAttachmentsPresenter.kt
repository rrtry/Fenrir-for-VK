package dev.ragnarok.fenrir.mvp.presenter.conversations

import android.os.Bundle
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.mvp.presenter.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.mvp.view.conversations.IBaseChatAttachmentsView
import dev.ragnarok.fenrir.util.DisposableHolder
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

abstract class BaseChatAttachmentsPresenter<T, V : IBaseChatAttachmentsView<T>> internal constructor(
    private val peerId: Int, accountId: Int, savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState) {
    @JvmField
    val data: MutableList<T>
    private var nextFrom: String? = null
    private var endOfContent = false
    private var loadingHolder: DisposableHolder<Void> = DisposableHolder()
    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayAttachments(data)
        resolveEmptyTextVisibility()
    }

    override fun onDestroyed() {
        loadingHolder.dispose()
        super.onDestroyed()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.showLoading(loadingHolder.isActive)
    }

    private fun initLoading() {
        load(null)
    }

    private fun load(startFrom: String?) {
        loadingHolder.append(
            requestAttachments(peerId, startFrom)
                .fromIOToMain()
                .subscribe(
                    {
                        onDataReceived(
                            startFrom,
                            it
                        )
                    },
                    { throwable ->
                        onRequestError(
                            Utils.getCauseIfRuntime(
                                throwable
                            )
                        )
                    })
        )
        resolveLoadingView()
    }

    private fun onRequestError(throwable: Throwable) {
        loadingHolder.dispose()
        resolveLoadingView()
        view?.showError(throwable.message)
    }

    private fun onDataReceived(startFrom: String?, result: Pair<String?, List<T>>) {
        loadingHolder.dispose()
        resolveLoadingView()
        nextFrom = result.first
        endOfContent = nextFrom.isNullOrEmpty()
        val newData = result.second
        if (startFrom != null) {
            val startSize = data.size
            data.addAll(newData)
            view?.notifyDataAdded(startSize, newData.size)
        } else {
            data.clear()
            data.addAll(newData)
            view?.notifyDatasetChanged()
        }
        resolveEmptyTextVisibility()
        onDataChanged()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(data.isEmpty())
    }

    open fun onDataChanged() {}
    private fun canLoadMore(): Boolean {
        return !endOfContent && !loadingHolder.isActive
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            load(nextFrom)
        }
    }

    fun fireRefresh() {
        loadingHolder.dispose()
        nextFrom = null
        initLoading()
    }

    abstract fun requestAttachments(
        peerId: Int,
        nextFrom: String?
    ): Single<Pair<String?, List<T>>>

    init {
        data = ArrayList()
        initLoading()
    }
}