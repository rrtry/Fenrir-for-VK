package dev.ragnarok.fenrir.mvp.presenter.search

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.mvp.view.search.IDocSearchView
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class DocsSearchPresenter(
    accountId: Int,
    criteria: DocumentSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IDocSearchView, DocumentSearchCriteria, Document, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val docsInteractor: IDocsInteractor = InteractorFactory.createDocsInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Int,
        criteria: DocumentSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Document>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(50 + offset)
        return docsInteractor.search(accountId, criteria, 50, offset)
            .map { documents -> create(documents, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): DocumentSearchCriteria {
        return DocumentSearchCriteria("")
    }

    override fun canSearch(criteria: DocumentSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

}