package dev.ragnarok.fenrir.fragment

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SelectProfilesActivity.Companion.createIntent
import dev.ragnarok.fenrir.adapter.CommunityManagersAdapter
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.CommunityManagersPresenter
import dev.ragnarok.fenrir.mvp.view.ICommunityManagersView
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityManagerAddPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityManagerEditPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class CommunityManagersFragment :
    BaseMvpFragment<CommunityManagersPresenter, ICommunityManagersView>(),
    ICommunityManagersView {
    private val requestSelectProfile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val users: ArrayList<Owner> = (result.data
                ?: return@registerForActivityResult).getParcelableArrayListExtra(Extra.OWNERS)
                ?: return@registerForActivityResult
            lazyPresenter {
                fireProfilesSelected(users)
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommunityManagersAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_managers, container, false)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mAdapter = CommunityManagersAdapter(emptyList())
        mAdapter?.setActionListener(object : CommunityManagersAdapter.ActionListener {
            override fun onManagerClick(manager: Manager) {
                presenter?.fireManagerClick(
                    manager
                )
            }

            override fun onManagerLongClick(manager: Manager) {
                showManagerContextMenu(manager)
            }
        })
        recyclerView.adapter = mAdapter
        root.findViewById<View>(R.id.button_add).setOnClickListener {
            presenter?.fireButtonAddClick()
        }
        return root
    }

    private fun showManagerContextMenu(manager: Manager) {
        val items = arrayOf(getString(R.string.delete))
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(manager.user?.fullName)
            .setItems(items) { _: DialogInterface?, _: Int ->
                presenter?.fireRemoveClick(
                    manager
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityManagersPresenter> {
        return object : IPresenterFactory<CommunityManagersPresenter> {
            override fun create(): CommunityManagersPresenter {
                return CommunityManagersPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelable(Extra.GROUP_ID)!!,
                    saveInstanceState
                )
            }
        }
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun displayRefreshing(loadingNow: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loadingNow
    }

    override fun displayData(managers: List<Manager>) {
        mAdapter?.setData(managers)
    }

    override fun goToManagerEditing(accountId: Int, groupId: Int, manager: Manager) {
        getCommunityManagerEditPlace(accountId, groupId, manager).tryOpenWith(requireActivity())
    }

    override fun showUserProfile(accountId: Int, user: User) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun startSelectProfilesActivity(accountId: Int, groupId: Int) {
        val criteria = PeopleSearchCriteria("").setGroupId(groupId)
        val c = SelectProfileCriteria()
        val place = getSingleTabSearchPlace(accountId, SearchContentType.PEOPLE, criteria)
        val intent = createIntent(requireActivity(), place, c)
        requestSelectProfile.launch(intent)
    }

    override fun startAddingUsersToManagers(accountId: Int, groupId: Int, users: ArrayList<User>) {
        getCommunityManagerAddPlace(accountId, groupId, users).tryOpenWith(requireActivity())
    }

    override fun notifyItemRemoved(index: Int) {
        mAdapter?.notifyItemRemoved(index)
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index)
    }

    override fun notifyItemAdded(index: Int) {
        mAdapter?.notifyItemInserted(index)
    }

    companion object {

        fun newInstance(accountId: Int, groupId: Community?): CommunityManagersFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.GROUP_ID, groupId)
            val fragment = CommunityManagersFragment()
            fragment.arguments = args
            return fragment
        }
    }
}