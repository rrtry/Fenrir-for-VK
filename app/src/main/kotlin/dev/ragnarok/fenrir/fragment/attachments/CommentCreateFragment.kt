package dev.ragnarok.fenrir.fragment.attachments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.CommentCreatePresenter
import dev.ragnarok.fenrir.mvp.view.ICreateCommentView


class CommentCreateFragment :
    AbsAttachmentsEditFragment<CommentCreatePresenter, ICreateCommentView>(), ICreateCommentView,
    MenuProvider {

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommentCreatePresenter> {
        return object : IPresenterFactory<CommentCreatePresenter> {
            override fun create(): CommentCreatePresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val commentDbid = requireArguments().getInt(Extra.COMMENT_ID)
                val sourceOwnerId = requireArguments().getInt(Extra.COMMENT_ID)
                val body = requireArguments().getString(Extra.BODY)
                return CommentCreatePresenter(
                    accountId,
                    commentDbid,
                    sourceOwnerId,
                    body,
                    saveInstanceState
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_attchments, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.ready) {
            presenter?.fireReadyClick()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.new_comment)
        setToolbarSubtitle(this, null)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPressed() ?: false
    }

    override fun returnDataToParent(textBody: String?) {
        val data = Bundle()
        data.putString(Extra.BODY, textBody)
        parentFragmentManager.setFragmentResult(REQUEST_CREATE_COMMENT, data)
    }

    override fun goBack() {
        requireActivity().onBackPressed()
    }

    companion object {
        const val REQUEST_CREATE_COMMENT = "request_create_comment"
        fun newInstance(
            accountId: Int,
            commentDbid: Int,
            sourceOwnerId: Int,
            body: String?
        ): CommentCreateFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.COMMENT_ID, commentDbid)
            args.putInt(Extra.OWNER_ID, sourceOwnerId)
            args.putString(Extra.BODY, body)
            val fragment = CommentCreateFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResult() {
        presenter?.fireReadyClick()
    }
}