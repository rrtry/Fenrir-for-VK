package dev.ragnarok.fenrir.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.CommunityBanEditPresenter
import dev.ragnarok.fenrir.mvp.view.ICommunityBanEditView
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.FormatUtil.formatCommunityBanInfo
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.MySpinnerView
import dev.ragnarok.fenrir.view.OnlineView

class CommunityBanEditFragment :
    BaseMvpFragment<CommunityBanEditPresenter, ICommunityBanEditView>(), ICommunityBanEditView,
    MenuProvider {
    private var mAvatar: ImageView? = null
    private var mOnlineView: OnlineView? = null
    private var mName: TextView? = null
    private var mDomain: TextView? = null
    private var mBanStatus: TextView? = null
    private var mBlockFor: MySpinnerView? = null
    private var mReason: MySpinnerView? = null
    private var mComment: TextInputEditText? = null
    private var mShowComment: MaterialCheckBox? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_ban_edit, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mAvatar = root.findViewById(R.id.avatar)
        mAvatar?.setOnClickListener {
            presenter?.fireAvatarClick()
        }
        mOnlineView = root.findViewById(R.id.online)
        mName = root.findViewById(R.id.name)
        mDomain = root.findViewById(R.id.domain)
        mBanStatus = root.findViewById(R.id.status)
        mBlockFor = root.findViewById(R.id.spinner_block_for)
        mBlockFor?.setIconOnClickListener {
            presenter?.fireBlockForClick()
        }
        mReason = root.findViewById(R.id.spinner_reason)
        mReason?.setIconOnClickListener {
            presenter?.fireResonClick()
        }
        mComment = root.findViewById(R.id.community_ban_comment)
        mComment?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireCommentEdit(
                    s
                )
            }
        })
        mShowComment = root.findViewById(R.id.community_ban_show_comment_to_user)
        mShowComment?.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            presenter?.fireShowCommentCheck(
                checked
            )
        }
        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.community_ban_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_save) {
            presenter?.fireButtonSaveClick()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.block_user)
        setToolbarSubtitle(this, null)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityBanEditPresenter> {
        return object : IPresenterFactory<CommunityBanEditPresenter> {
            override fun create(): CommunityBanEditPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val groupId = requireArguments().getInt(Extra.GROUP_ID)
                val banned: Banned? = requireArguments().getParcelable(Extra.BANNED)
                if (banned != null) {
                    return CommunityBanEditPresenter(
                        accountId,
                        groupId,
                        banned,
                        saveInstanceState
                    )
                }
                val users: ArrayList<User>? = requireArguments().getParcelableArrayList(Extra.USERS)
                val owners = ArrayList<Owner>()
                if (users.nonNullNoEmpty()) {
                    owners.addAll(users)
                }
                return CommunityBanEditPresenter(accountId, groupId, owners, saveInstanceState)
            }
        }
    }

    override fun displayUserInfo(user: Owner) {
        if (mAvatar != null) {
            displayAvatar(mAvatar, RoundTransformation(), user.maxSquareAvatar, null)
        }
        safelySetText(mName, user.fullName)
        var iconRes: Int? = null
        if (user is User) {
            iconRes =
                getOnlineIcon(user.isOnline, user.isOnlineMobile, user.platform, user.onlineApp)
        }
        if (mOnlineView != null) {
            mOnlineView?.visibility = if (iconRes != null) View.VISIBLE else View.INVISIBLE
            if (iconRes != null) {
                mOnlineView?.setIcon(iconRes)
            }
        }
        when {
            user.domain.nonNullNoEmpty() -> {
                safelySetText(mDomain, "@" + user.domain)
            }
            user is User -> {
                safelySetText(mDomain, "@id" + user.getObjectId())
            }
            user is Community -> {
                safelySetText(mDomain, "@club" + user.id)
            }
        }
    }

    override fun displayBanStatus(adminId: Int, adminName: String?, endDate: Long) {
        mBanStatus?.let {
            try {
                val context = it.context
                val spannable =
                    formatCommunityBanInfo(context, adminId, adminName, endDate, null)
                it.setText(spannable, TextView.BufferType.SPANNABLE)
                it.movementMethod = LinkMovementMethod.getInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun displayBlockFor(blockFor: String?) {
        mBlockFor?.setValue(blockFor)
    }

    override fun displayReason(reason: String?) {
        mReason?.setValue(reason)
    }

    override fun diplayComment(comment: String?) {
        safelySetText(mComment, comment)
    }

    override fun setShowCommentChecked(checked: Boolean) {
        safelySetChecked(mShowComment, checked)
    }

    override fun goBack() {
        requireActivity().onBackPressed()
    }

    override fun displaySelectOptionDialog(requestCode: Int, options: List<IdOption>) {
        val strings = arrayOfNulls<String>(options.size)
        for (i in options.indices) {
            strings[i] = options[i].title
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select_from_list_title)
            .setItems(strings) { _: DialogInterface?, which: Int ->
                presenter?.fireOptionSelected(
                    requestCode,
                    options[which]
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun openProfile(accountId: Int, owner: Owner) {
        getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity())
    }

    companion object {
        fun newInstance(accountId: Int, groupId: Int, banned: Banned?): CommunityBanEditFragment {
            return newInstance(accountId, groupId, banned, null)
        }

        fun newInstance(
            accountId: Int,
            groupId: Int,
            users: ArrayList<User>?
        ): CommunityBanEditFragment {
            return newInstance(accountId, groupId, null, users)
        }

        private fun newInstance(
            accountId: Int,
            groupId: Int,
            banned: Banned?,
            users: ArrayList<User>?
        ): CommunityBanEditFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.GROUP_ID, groupId)
            args.putParcelableArrayList(Extra.USERS, users)
            args.putParcelable(Extra.BANNED, banned)
            val fragment = CommunityBanEditFragment()
            fragment.arguments = args
            return fragment
        }
    }
}