package dev.ragnarok.fenrir.fragment

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SelectProfilesActivity.Companion.createIntent
import dev.ragnarok.fenrir.adapter.ChatMembersListAdapter
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.friends.FriendsTabsFragment
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.ChatMembersPresenter
import dev.ragnarok.fenrir.mvp.view.IChatMembersView
import dev.ragnarok.fenrir.place.PlaceFactory.getFriendsFollowersPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class ChatUsersFragment : BaseMvpFragment<ChatMembersPresenter, IChatMembersView>(),
    IChatMembersView, ChatMembersListAdapter.ActionListener {
    private val requestAddUser = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val users: ArrayList<Owner>? = result.data?.getParcelableArrayListExtra(Extra.OWNERS)
            lazyPresenter {
                fireUserSelected(users)
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: ChatMembersListAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_chat_users, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mAdapter = ChatMembersListAdapter(requireActivity(), emptyList())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val fabAdd: FloatingActionButton = root.findViewById(R.id.fragment_chat_users_add)
        fabAdd.setOnClickListener {
            presenter?.fireAddUserClick()
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.chat_users)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onRemoveClick(user: AppChatUser) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(getString(R.string.remove_chat_user_commit, user.getMember()?.fullName))
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                presenter?.fireUserDeteleConfirmed(
                    user
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun displayData(users: List<AppChatUser>) {
        mAdapter?.setData(users)
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun openUserWall(accountId: Int, user: Owner) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun startSelectUsersActivity(accountId: Int) {
        val place = getFriendsFollowersPlace(
            accountId,
            accountId,
            FriendsTabsFragment.TAB_ALL_FRIENDS,
            null
        )
        val criteria =
            SelectProfileCriteria().setOwnerType(SelectProfileCriteria.OwnerType.ONLY_FRIENDS)
        val intent = createIntent(requireActivity(), place, criteria)
        requestAddUser.launch(intent)
    }

    override fun setIsOwner(isOwner: Boolean) {
        mAdapter?.setIsOwner(isOwner)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatMembersPresenter> {
        return object : IPresenterFactory<ChatMembersPresenter> {
            override fun create(): ChatMembersPresenter {
                return ChatMembersPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.CHAT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onUserClick(user: AppChatUser) {
        presenter?.fireUserClick(
            user
        )
    }

    override fun onAdminToggleClick(isAdmin: Boolean, ownerId: Int) {
        presenter?.fireAdminToggleClick(
            isAdmin,
            ownerId
        )
    }

    companion object {
        fun buildArgs(accountId: Int, chatId: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.CHAT_ID, chatId)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(args: Bundle?): ChatUsersFragment {
            val fragment = ChatUsersFragment()
            fragment.arguments = args
            return fragment
        }
    }
}