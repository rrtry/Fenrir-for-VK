package dev.ragnarok.fenrir.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.adapter.MessagesAdapter
import dev.ragnarok.fenrir.adapter.MessagesAdapter.OnMessageActionListener
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.mvp.core.IPresenterFactory
import dev.ragnarok.fenrir.mvp.presenter.MessagesLookPresenter
import dev.ragnarok.fenrir.mvp.view.IMessagesLookView
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.SafeCallInt
import dev.ragnarok.fenrir.util.Utils.safeObjectCall
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom
import java.lang.ref.WeakReference

class MessagesLookFragment : PlaceSupportMvpFragment<MessagesLookPresenter, IMessagesLookView>(),
    IMessagesLookView, OnMessageActionListener, BackPressCallback {
    private var mRecyclerView: RecyclerView? = null
    private var toolbarRootView: FrameLayout? = null
    private var downMenuGroup: FrameLayout? = null
    private var mMessagesAdapter: MessagesAdapter? = null
    private var mHeaderView: View? = null
    private var mFooterView: View? = null
    private var mHeaderHelper: LoadMoreFooterHelper? = null
    private var mFooterHelper: LoadMoreFooterHelper? = null
    private var mEndlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener? = null
    private var mActionView: ActionModeHolder? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_messages_lookup, container, false)
        root.background = CurrentTheme.getChatBackground(requireActivity())
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, true)
        mRecyclerView = root.findViewById(R.id.recycleView)
        mRecyclerView?.layoutManager = layoutManager
        mHeaderView = inflater.inflate(R.layout.footer_load_more, mRecyclerView, false)
        mFooterView = inflater.inflate(R.layout.footer_load_more, mRecyclerView, false)
        mHeaderHelper =
            createFrom(mHeaderView, object : LoadMoreFooterHelper.Callback {
                override fun onLoadMoreClick() {
                    onHeaderLoadMoreClick()
                }
            })
        mFooterHelper =
            createFrom(mFooterView, object : LoadMoreFooterHelper.Callback {
                override fun onLoadMoreClick() {
                    onFooterLoadMoreClick()
                }
            })
        downMenuGroup = root.findViewById(R.id.down_menu)
        toolbarRootView = root.findViewById(R.id.toolbar_root)
        mEndlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                onFooterLoadMoreClick()
            }

            override fun onScrollToFirstElement() {
                onHeaderLoadMoreClick()
            }
        }
        mEndlessRecyclerOnScrollListener?.let { mRecyclerView?.addOnScrollListener(it) }
        return root
    }

    private fun onFooterLoadMoreClick() {
        presenter?.fireFooterLoadMoreClick()
    }

    private fun onHeaderLoadMoreClick() {
        presenter?.fireHeaderLoadMoreClick()
    }

    override fun displayMessages(messages: MutableList<Message>, lastReadId: LastReadId) {
        mMessagesAdapter = MessagesAdapter(requireActivity(), messages, lastReadId, this, true)
        mMessagesAdapter?.setOnMessageActionListener(this)
        mFooterView?.let { mMessagesAdapter?.addFooter(it) }
        mHeaderView?.let { mMessagesAdapter?.addHeader(it) }
        mRecyclerView?.adapter = mMessagesAdapter
    }

    override fun focusTo(index: Int) {
        mEndlessRecyclerOnScrollListener?.let { mRecyclerView?.removeOnScrollListener(it) }
        mRecyclerView?.scrollToPosition(index + 1) // +header
        mEndlessRecyclerOnScrollListener?.let { mRecyclerView?.addOnScrollListener(it) }
    }

    override fun notifyMessagesUpAdded(position: Int, count: Int) {
        mMessagesAdapter?.notifyItemRangeInserted(position + 1, count) //+header
    }

    override fun notifyMessagesDownAdded(count: Int) {
        mMessagesAdapter?.notifyItemRemoved(0)
        mMessagesAdapter?.notifyItemRangeInserted(0, count + 1) //+header
    }

    override fun configNowVoiceMessagePlaying(
        id: Int,
        progress: Float,
        paused: Boolean,
        amin: Boolean,
        speed: Boolean
    ) {
        mMessagesAdapter?.configNowVoiceMessagePlaying(id, progress, paused, amin, speed)
    }

    override fun bindVoiceHolderById(
        holderId: Int,
        play: Boolean,
        paused: Boolean,
        progress: Float,
        amin: Boolean,
        speed: Boolean
    ) {
        mMessagesAdapter?.bindVoiceHolderById(holderId, play, paused, progress, amin, speed)
    }

    override fun disableVoicePlaying() {
        mMessagesAdapter?.disableVoiceMessagePlaying()
    }

    override fun showActionMode(
        title: String,
        canEdit: Boolean,
        canPin: Boolean,
        canStar: Boolean,
        doStar: Boolean,
        canSpam: Boolean
    ) {
        val isDown = Settings.get().main().isMessages_menu_down
        if (if (isDown) downMenuGroup == null else toolbarRootView == null) {
            return
        }
        if (!isDown) {
            if (toolbarRootView?.childCount == Constants.FRAGMENT_CHAT_APP_BAR_VIEW_COUNT) {
                mActionView = ActionModeHolder(
                    LayoutInflater.from(requireActivity())
                        .inflate(R.layout.view_action_mode, toolbarRootView, false), this
                )
                toolbarRootView?.addView(mActionView?.rootView)
            }
        } else {
            downMenuGroup?.visibility = View.VISIBLE
            if (downMenuGroup?.childCount == Constants.FRAGMENT_CHAT_DOWN_MENU_VIEW_COUNT) {
                mActionView = ActionModeHolder(
                    LayoutInflater.from(requireActivity())
                        .inflate(R.layout.view_action_mode, downMenuGroup, false), this
                )
                downMenuGroup?.addView(mActionView?.rootView)
            }
        }
        mActionView?.show()
        mActionView?.titleView?.text = title
        mActionView?.buttonSpam?.visibility =
            if (canSpam) View.VISIBLE else View.GONE
        mActionView?.buttonEdit?.visibility = if (canEdit) View.VISIBLE else View.GONE
        mActionView?.buttonPin?.visibility =
            if (canPin) View.VISIBLE else View.GONE
        mActionView?.buttonStar?.visibility = if (canStar) View.VISIBLE else View.GONE
        mActionView?.buttonStar?.setImageResource(if (doStar) R.drawable.star_add else R.drawable.star_none)
    }

    override fun finishActionMode() {
        mActionView?.hide()
        if (Settings.get().main().isMessages_menu_down && downMenuGroup != null) {
            downMenuGroup?.visibility = View.GONE
        }
    }

    override fun notifyDataChanged() {
        mMessagesAdapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(index: Int) {
        mMessagesAdapter?.notifyItemBindableChanged(index)
    }

    override fun setupHeaders(
        @LoadMoreState upHeaderState: Int,
        @LoadMoreState downHeaderState: Int
    ) {
        mFooterHelper?.switchToState(upHeaderState)
        mHeaderHelper?.switchToState(downHeaderState)
    }

    override fun forwardMessages(accountId: Int, messages: ArrayList<Message>) {
        startForSendAttachments(requireActivity(), accountId, FwdMessages(messages))
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<MessagesLookPresenter> {
        return object : IPresenterFactory<MessagesLookPresenter> {
            override fun create(): MessagesLookPresenter {
                val aid = requireArguments().getInt(Extra.ACCOUNT_ID)
                val peerId = requireArguments().getInt(Extra.PEER_ID)
                val focusTo = requireArguments().getInt(Extra.FOCUS_TO)
                val message: Message? =
                    if (requireArguments().containsKey(Extra.MESSAGE)) requireArguments().getParcelable(
                        Extra.MESSAGE
                    ) else null
                return MessagesLookPresenter(aid, peerId, focusTo, message, saveInstanceState)
            }
        }
    }

    override fun onAvatarClick(message: Message, userId: Int, position: Int) {
        if (mActionView?.isVisible == true) {
            presenter?.fireMessageClick(
                message,
                position
            )
        } else {
            presenter?.fireOwnerClick(userId)
        }
    }

    override fun onLongAvatarClick(message: Message, userId: Int, position: Int) {
        if (mActionView?.isVisible == true) {
            presenter?.fireMessageClick(
                message,
                position
            )
        } else {
            presenter?.fireOwnerClick(
                userId
            )
        }
    }

    override fun onRestoreClick(message: Message, position: Int) {
        presenter?.fireMessageRestoreClick(
            message
        )
    }

    override fun onBotKeyboardClick(button: Keyboard.Button) {}
    override fun onMessageLongClick(message: Message, position: Int): Boolean {
        presenter?.fireMessageLongClick(
            message,
            position
        )
        return true
    }

    override fun onMessageClicked(message: Message, position: Int) {
        presenter?.fireMessageClick(
            message,
            position
        )
    }

    override fun onMessageDelete(message: Message) {
        val ids = ArrayList<Int>()
        ids.add(message.getObjectId())
        presenter?.fireDeleteForMeClick(
            ids
        )
    }

    override fun onBackPressed(): Boolean {
        if (mActionView?.isVisible == true) {
            mActionView?.hide()
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.viewing_messages)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    private inner class ActionModeHolder(val rootView: View, fragment: MessagesLookFragment) :
        View.OnClickListener {
        val buttonClose: View = rootView.findViewById(R.id.buttonClose)
        val buttonEdit: View = rootView.findViewById(R.id.buttonEdit)
        val buttonForward: View = rootView.findViewById(R.id.buttonForward)
        val buttonCopy: View = rootView.findViewById(R.id.buttonCopy)
        val buttonDelete: View = rootView.findViewById(R.id.buttonDelete)
        val buttonPin: View = rootView.findViewById(R.id.buttonPin)
        val buttonSpam: View = rootView.findViewById(R.id.buttonSpam)
        val buttonStar: ImageView = rootView.findViewById(R.id.buttonStar)
        val titleView: TextView = rootView.findViewById(R.id.actionModeTitle)
        val reference: WeakReference<MessagesLookFragment> = WeakReference(fragment)
        fun show() {
            rootView.visibility = View.VISIBLE
        }

        val isVisible: Boolean
            get() = rootView.visibility == View.VISIBLE

        fun hide() {
            rootView.visibility = View.GONE
            if (Settings.get().main().isMessages_menu_down) {
                safeObjectCall(reference.get(), object : SafeCallInt {
                    override fun call() {
                        if (reference.get()?.downMenuGroup != null) {
                            reference.get()?.downMenuGroup?.visibility = View.GONE
                        }
                    }
                })
            }
            safeObjectCall(reference.get(), object : SafeCallInt {
                override fun call() {
                    reference.get()?.presenter?.fireActionModeDestroy()
                }
            })
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.buttonClose -> {
                    hide()
                }
                R.id.buttonForward -> {
                    safeObjectCall(reference.get(), object : SafeCallInt {
                        override fun call() {
                            reference.get()?.presenter?.fireForwardClick()
                        }
                    })
                    hide()
                }
                R.id.buttonCopy -> {
                    safeObjectCall(reference.get(), object : SafeCallInt {
                        override fun call() {
                            reference.get()?.presenter?.fireActionModeCopyClick()
                        }
                    })
                    hide()
                }
                R.id.buttonDelete -> {
                    safeObjectCall(reference.get(), object : SafeCallInt {
                        override fun call() {
                            reference.get()?.presenter?.fireActionModeDeleteClick()
                        }
                    })
                    hide()
                }
                R.id.buttonSpam -> {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setIcon(R.drawable.report_red)
                        .setMessage(R.string.do_report)
                        .setTitle(R.string.select)
                        .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                            safeObjectCall(reference.get(), object : SafeCallInt {
                                override fun call() {
                                    reference.get()?.presenter?.fireActionModeSpamClick()
                                }
                            })
                            hide()
                        }
                        .setNeutralButton(R.string.delete) { _: DialogInterface?, _: Int ->
                            safeObjectCall(reference.get(), object : SafeCallInt {
                                override fun call() {
                                    reference.get()?.presenter?.fireActionModeDeleteClick()
                                }
                            })
                            hide()
                        }
                        .setCancelable(true)
                        .show()
                }
            }
        }

        init {
            buttonClose.setOnClickListener(this)
            buttonEdit.setOnClickListener(this)
            buttonForward.setOnClickListener(this)
            buttonCopy.setOnClickListener(this)
            buttonDelete.setOnClickListener(this)
            buttonPin.setOnClickListener(this)
            buttonStar.setOnClickListener(this)
            buttonSpam.setOnClickListener(this)
        }
    }

    companion object {
        fun buildArgs(accountId: Int, peerId: Int, focusMessageId: Int, message: Message?): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.PEER_ID, peerId)
            args.putInt(Extra.FOCUS_TO, focusMessageId)
            if (message != null) {
                args.putParcelable(Extra.MESSAGE, message)
            }
            return args
        }

        fun newInstance(args: Bundle?): MessagesLookFragment {
            val fragment = MessagesLookFragment()
            fragment.arguments = args
            return fragment
        }
    }
}