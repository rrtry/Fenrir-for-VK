package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.ParcelUtils.readIntStringMap
import dev.ragnarok.fenrir.util.ParcelUtils.writeIntStringMap
import dev.ragnarok.fenrir.util.Utils.safeCountOf

class Message : AbsModel, Identificable, ISelectable {
    var accountId = 0
        private set
    private var id: Int
    var body: String? = null
        private set
    var peerId = 0
        private set
    var senderId = 0
        private set
    var isOut = false
        private set

    @MessageStatus
    var status = 0
        private set
    var date: Long = 0
        private set
    override var isSelected = false
    var isDeleted = false
        private set
    var isDeletedForAll = false
        private set
    var originalId = 0
        private set

    /*public String getTitle() {
        return title;
    }

    public Message setTitle(String title) {
        this.title = title;
        return this;
    }*/  var isImportant = false
        private set
    var attachments: Attachments? = null
        private set
    var fwd: ArrayList<Message>? = null
        private set
    var payload: String? = null
        private set
    var keyboard: Keyboard? = null
        private set

    //chat_columns
    @get:ChatAction
    @ChatAction
    var action = 0
        private set
    var actionMid = 0
        private set
    var actionEmail: String? = null
        private set
    var actionText: String? = null
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    private var actionUser: Owner? = null
    var randomId: Long = 0
        private set
    var extras: Map<Int, String>? = null
        private set

    @CryptStatus
    var cryptStatus = 0
    var decryptedBody: String? = null
    var sender: Owner? = null
        private set
    var forwardMessagesCount = 0
        private set
    var isHasAttachments = false
        private set
    var updateTime: Long = 0
        private set

    constructor(id: Int) {
        this.id = id
    }

    private constructor(`in`: Parcel) : super(`in`) {
        accountId = `in`.readInt()
        id = `in`.readInt()
        body = `in`.readString()
        decryptedBody = `in`.readString()
        //this.title = in.readString();
        peerId = `in`.readInt()
        senderId = `in`.readInt()
        isOut = `in`.readInt() == 1
        isImportant = `in`.readInt() == 1
        @MessageStatus val tStatus = `in`.readInt()
        status = tStatus
        @CryptStatus val cs = `in`.readInt()
        cryptStatus = cs
        date = `in`.readLong()
        isSelected = `in`.readInt() == 1
        isDeleted = `in`.readInt() == 1
        isDeletedForAll = `in`.readInt() == 1
        attachments = `in`.readParcelable(Attachments::class.java.classLoader)
        fwd = `in`.createTypedArrayList(CREATOR)
        originalId = `in`.readInt()
        @ChatAction val tmpChatAction = `in`.readInt()
        action = tmpChatAction
        actionMid = `in`.readInt()
        actionEmail = `in`.readString()
        actionText = `in`.readString()
        photo50 = `in`.readString()
        photo100 = `in`.readString()
        photo200 = `in`.readString()
        actionUser =
            `in`.readParcelable<ParcelableOwnerWrapper>(ParcelableOwnerWrapper::class.java.classLoader)!!
                .get()
        sender =
            `in`.readParcelable(if (senderId > 0) User::class.java.classLoader else Community::class.java.classLoader)
        randomId = `in`.readLong()
        extras = readIntStringMap(`in`)
        forwardMessagesCount = `in`.readInt()
        isHasAttachments = `in`.readInt() == 1
        updateTime = `in`.readLong()
        payload = `in`.readString()
        keyboard = `in`.readParcelable(Keyboard::class.java.classLoader)
    }

    fun setHasAttachments(hasAttachments: Boolean): Message {
        isHasAttachments = hasAttachments
        return this
    }

    fun setForwardMessagesCount(forwardMessagesCount: Int): Message {
        this.forwardMessagesCount = forwardMessagesCount
        return this
    }

    fun setBody(body: String?): Message {
        this.body = body
        return this
    }

    fun setRandomId(randomId: Long): Message {
        this.randomId = randomId
        return this
    }

    fun setKeyboard(keyboard: Keyboard?): Message {
        this.keyboard = keyboard
        return this
    }

    fun setSender(sender: Owner?): Message {
        this.sender = sender
        return this
    }

    fun setActionUser(actionUser: Owner?): Message {
        this.actionUser = actionUser
        return this
    }

    fun setOut(out: Boolean): Message {
        isOut = out
        return this
    }

    fun setStatus(@MessageStatus status: Int): Message {
        this.status = status
        return this
    }

    val isEditing: Boolean
        get() = status == MessageStatus.EDITING

    fun setDate(date: Long): Message {
        this.date = date
        return this
    }

    fun setAttachments(attachments: Attachments?): Message {
        this.attachments = attachments
        return this
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): Message {
        this.id = id
        return this
    }

    fun setDeleted(deleted: Boolean): Message {
        isDeleted = deleted
        return this
    }

    fun setPeerId(peerId: Int): Message {
        this.peerId = peerId
        return this
    }

    fun setFwd(fwd: ArrayList<Message>?): Message {
        this.fwd = fwd
        return this
    }

    fun prepareFwd(capacity: Int): ArrayList<Message> {
        if (fwd == null) {
            fwd = ArrayList(capacity)
        }
        return fwd!!
    }

    fun setOriginalId(originalId: Int): Message {
        this.originalId = originalId
        return this
    }

    fun setImportant(important: Boolean): Message {
        isImportant = important
        return this
    }

    fun setAction(@ChatAction action: Int): Message {
        this.action = action
        return this
    }

    fun setActionMid(actionMid: Int): Message {
        this.actionMid = actionMid
        return this
    }

    fun setActionEmail(actionEmail: String?): Message {
        this.actionEmail = actionEmail
        return this
    }

    fun setActionText(actionText: String?): Message {
        this.actionText = actionText
        return this
    }

    fun setPhoto50(photo50: String?): Message {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): Message {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): Message {
        this.photo200 = photo200
        return this
    }

    val isServiseMessage: Boolean
        get() = action != ChatAction.NO_ACTION

    fun setSenderId(senderId: Int): Message {
        this.senderId = senderId
        return this
    }

    fun getServiceText(context: Context): String? {
        val senderUserName = sender?.fullName ?: ("id$senderId")
        val actionSubject = if (actionEmail.isNullOrEmpty()) actionUser?.fullName else actionEmail
        val itself = sender?.ownerId == actionMid
        var result: String? = null
        when (action) {
            ChatAction.PHOTO_UPDATE -> result =
                context.getString(R.string.service_update_chat_photo, senderUserName)
            ChatAction.PHOTO_REMOVE -> result =
                context.getString(R.string.service_remove_chat_photo, senderUserName)
            ChatAction.CREATE -> result =
                context.getString(R.string.service_create_chat, senderUserName, actionText)
            ChatAction.TITLE_UPDATE -> result = context.getString(
                R.string.service_changed_chat_name,
                senderUserName,
                actionText
            )
            ChatAction.INVITE_USER -> result = if (itself) {
                context.getString(R.string.service_return_to_chat, senderUserName)
            } else {
                context.getString(R.string.service_invited, senderUserName, actionSubject)
            }
            ChatAction.KICK_USER -> result = if (itself) {
                context.getString(R.string.service_left_this_chat, senderUserName)
            } else {
                context.getString(R.string.service_removed, senderUserName, actionSubject)
            }
            ChatAction.PIN_MESSAGE -> result =
                context.getString(R.string.service_pinned_message, senderUserName)
            ChatAction.UNPIN_MESSAGE -> result =
                context.getString(R.string.service_unpinned_message, senderUserName)
            ChatAction.INVITE_USER_BY_LINK -> result =
                context.getString(R.string.service_invite_user_by_link, senderUserName)
            ChatAction.NO_ACTION -> {}
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Message
        return id == that.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setDeletedForAll(deletedForAll: Boolean): Message {
        isDeletedForAll = deletedForAll
        return this
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(accountId)
        parcel.writeInt(id)
        parcel.writeString(body)
        parcel.writeString(decryptedBody)
        //dest.writeString(title);
        parcel.writeInt(peerId)
        parcel.writeInt(senderId)
        parcel.writeInt(if (isOut) 1 else 0)
        parcel.writeInt(if (isImportant) 1 else 0)
        parcel.writeInt(status)
        parcel.writeInt(cryptStatus)
        parcel.writeLong(date)
        parcel.writeInt(if (isSelected) 1 else 0)
        parcel.writeInt(if (isDeleted) 1 else 0)
        parcel.writeInt(if (isDeletedForAll) 1 else 0)
        parcel.writeParcelable(attachments, i)
        parcel.writeTypedList(fwd)
        parcel.writeInt(originalId)
        parcel.writeInt(action)
        parcel.writeInt(actionMid)
        parcel.writeString(actionEmail)
        parcel.writeString(actionText)
        parcel.writeString(photo50)
        parcel.writeString(photo100)
        parcel.writeString(photo200)
        parcel.writeParcelable(ParcelableOwnerWrapper(actionUser), i)
        parcel.writeParcelable(sender, i)
        parcel.writeLong(randomId)
        writeIntStringMap(parcel, extras)
        parcel.writeInt(forwardMessagesCount)
        parcel.writeInt(if (isHasAttachments) 1 else 0)
        parcel.writeLong(updateTime)
        parcel.writeString(payload)
        parcel.writeParcelable(keyboard, i)
    }

    fun setUpdateTime(updateTime: Long): Message {
        this.updateTime = updateTime
        return this
    }

    fun setAccountId(accountId: Int): Message {
        this.accountId = accountId
        return this
    }

    val isSent: Boolean
        get() = status == MessageStatus.SENT
    val isSticker: Boolean
        get() = safeCountOf(attachments?.stickers) > 0
    val isGraffity: Boolean
        get() = safeCountOf(attachments?.graffity) > 0
    val isCall: Boolean
        get() = safeCountOf(attachments?.calls) > 0
    val isGift: Boolean
        get() = safeCountOf(attachments?.gifts) > 0
    val isVoiceMessage: Boolean
        get() = safeCountOf(attachments?.voiceMessages) > 0

    @MessageType
    val messageTypeByAttachments: Int
        get() {
            when {
                !isHasAttachments -> {
                    return MessageType.NO
                }
                attachments == null || attachments?.isEmptyAttachments == true -> {
                    return MessageType.OTHERS
                }
                isSticker -> {
                    return MessageType.STICKER
                }
                isGraffity -> {
                    return MessageType.GRAFFITY
                }
                isCall -> {
                    return MessageType.CALL
                }
                isGift -> {
                    return MessageType.GIFT
                }
                isVoiceMessage -> {
                    return MessageType.VOICE
                }
                attachments?.audios.nonNullNoEmpty() && attachments?.photos.isNullOrEmpty() && attachments?.videos.isNullOrEmpty() && attachments?.docs.isNullOrEmpty() && attachments?.posts.isNullOrEmpty() -> {
                    return MessageType.AUDIO
                }
                attachments?.audios.isNullOrEmpty() && attachments?.photos.nonNullNoEmpty() && attachments?.videos.isNullOrEmpty() && attachments?.docs.isNullOrEmpty() && attachments?.posts.isNullOrEmpty() -> {
                    return MessageType.PHOTO
                }
                attachments?.audios.isNullOrEmpty() && attachments?.photos.isNullOrEmpty() && attachments?.videos.nonNullNoEmpty() && attachments?.docs.isNullOrEmpty() && attachments?.posts.isNullOrEmpty() -> {
                    return MessageType.VIDEO
                }
                attachments?.audios.isNullOrEmpty() && attachments?.photos.isNullOrEmpty() && attachments?.videos.isNullOrEmpty() && attachments?.docs.nonNullNoEmpty() && attachments?.posts.isNullOrEmpty() -> {
                    return MessageType.DOC
                }
                attachments?.audios.isNullOrEmpty() && attachments?.photos.isNullOrEmpty() && attachments?.videos.isNullOrEmpty() && attachments?.docs.isNullOrEmpty() && attachments?.posts.nonNullNoEmpty() -> {
                    return MessageType.WALL
                }
                else -> return MessageType.OTHERS
            }
        }

    fun setExtras(extras: Map<Int, String>?): Message {
        this.extras = extras
        return this
    }

    val isChatTitleUpdate: Boolean
        get() = action == ChatAction.TITLE_UPDATE

    fun setCryptStatus(@CryptStatus cryptStatus: Int): Message {
        this.cryptStatus = cryptStatus
        return this
    }

    fun setDecryptedBody(decryptedBody: String?): Message {
        this.decryptedBody = decryptedBody
        return this
    }

    fun setPayload(payload: String?): Message {
        this.payload = payload
        return this
    }

    object Extra {
        const val VOICE_RECORD = 1
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Message> = object : Parcelable.Creator<Message> {
            override fun createFromParcel(source: Parcel): Message {
                return Message(source)
            }

            override fun newArray(size: Int): Array<Message?> {
                return arrayOfNulls(size)
            }
        }

        @ChatAction
        fun fromApiChatAction(action: String?): Int {
            if (action.isNullOrEmpty()) {
                return ChatAction.NO_ACTION
            }
            return if ("chat_photo_update".equals(action, ignoreCase = true)) {
                ChatAction.PHOTO_UPDATE
            } else if ("chat_photo_remove".equals(action, ignoreCase = true)) {
                ChatAction.PHOTO_REMOVE
            } else if ("chat_create".equals(action, ignoreCase = true)) {
                ChatAction.CREATE
            } else if ("chat_title_update".equals(action, ignoreCase = true)) {
                ChatAction.TITLE_UPDATE
            } else if ("chat_invite_user".equals(action, ignoreCase = true)) {
                ChatAction.INVITE_USER
            } else if ("chat_kick_user".equals(action, ignoreCase = true)) {
                ChatAction.KICK_USER
            } else if ("chat_pin_message".equals(action, ignoreCase = true)) {
                ChatAction.PIN_MESSAGE
            } else if ("chat_unpin_message".equals(action, ignoreCase = true)) {
                ChatAction.UNPIN_MESSAGE
            } else if ("chat_invite_user_by_link".equals(action, ignoreCase = true)) {
                ChatAction.INVITE_USER_BY_LINK
            } else {
                ChatAction.NO_ACTION
            }
        }
    }
}