package dev.ragnarok.fenrir.fragment.attachments

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.WallEditorAttrs
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.PostEditPresenter
import dev.ragnarok.fenrir.mvp.view.IPostEditView

class PostEditFragment : AbsPostEditFragment<PostEditPresenter, IPostEditView>(), IPostEditView,
    MenuProvider {

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PostEditPresenter> {
        return object : IPresenterFactory<PostEditPresenter> {
            override fun create(): PostEditPresenter {
                val post: Post = requireArguments().getParcelable(Extra.POST)!!
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val attrs: WallEditorAttrs = requireArguments().getParcelable(Extra.ATTRS)!!
                return PostEditPresenter(accountId, post, attrs, saveInstanceState)
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

    override fun onResult() {
        presenter?.fireReadyClick()
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun closeAsSuccess() {
        requireActivity().onBackPressed()
    }

    override fun showConfirmExitDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(R.string.save_changes_question)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireExitWithSavingConfirmed()
            }
            .setNegativeButton(R.string.button_no) { _: DialogInterface?, _: Int ->
                presenter?.fireExitWithoutSavingClick()
            }
            .setNeutralButton(R.string.button_cancel, null)
            .show()
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPressed() ?: false
    }

    companion object {
        fun newInstance(args: Bundle?): PostEditFragment {
            val fragment = PostEditFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Int, post: Post, attrs: WallEditorAttrs): Bundle {
            val args = Bundle()
            args.putParcelable(Extra.POST, post)
            args.putParcelable(Extra.ATTRS, attrs)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            return args
        }
    }
}