package dev.ragnarok.fenrir.mvp.view

import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.mvp.core.IMvpView
import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView
import dev.ragnarok.fenrir.upload.Upload

interface IDocListView : IAccountDependencyView, IMvpView, IErrorView {
    fun displayData(documents: MutableList<Document>, asImages: Boolean)
    fun showRefreshing(refreshing: Boolean)
    fun notifyDataSetChanged()
    fun notifyDataAdd(position: Int, count: Int)
    fun notifyDataRemoved(position: Int)
    fun openDocument(accountId: Int, document: Document)
    fun returnSelection(docs: ArrayList<Document>)
    fun goToGifPlayer(accountId: Int, gifs: ArrayList<Document>, selected: Int)
    fun requestReadExternalStoragePermission()
    fun startSelectUploadFileActivity(accountId: Int)
    fun setUploadDataVisible(visible: Boolean)
    fun displayUploads(data: List<Upload>)
    fun notifyUploadItemsAdded(position: Int, count: Int)
    fun notifyUploadItemChanged(position: Int)
    fun notifyUploadItemRemoved(position: Int)
    fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean)
    fun displayFilterData(filters: MutableList<DocFilter>)
    fun notifyFiltersChanged()
    fun setAdapterType(imagesOnly: Boolean)
}