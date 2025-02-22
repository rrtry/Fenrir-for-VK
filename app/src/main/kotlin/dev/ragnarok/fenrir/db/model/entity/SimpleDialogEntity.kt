package dev.ragnarok.fenrir.db.model.entity

class SimpleDialogEntity(val peerId: Int) {
    var title: String? = null
        private set
    var unreadCount = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var inRead = 0
        private set
    var outRead = 0
        private set
    var pinned: MessageDboEntity? = null
        private set
    var currentKeyboard: KeyboardEntity? = null
        private set
    var lastMessageId = 0
        private set
    var isGroupChannel = false
        private set
    var acl = 0
        private set
    var major_id = 0
        private set
    var minor_id = 0
        private set

    fun setAcl(acl: Int): SimpleDialogEntity {
        this.acl = acl
        return this
    }

    fun setLastMessageId(lastMessageId: Int): SimpleDialogEntity {
        this.lastMessageId = lastMessageId
        return this
    }

    fun setPinned(pinned: MessageDboEntity?): SimpleDialogEntity {
        this.pinned = pinned
        return this
    }

    fun setTitle(title: String?): SimpleDialogEntity {
        this.title = title
        return this
    }

    fun setCurrentKeyboard(currentKeyboard: KeyboardEntity?): SimpleDialogEntity {
        this.currentKeyboard = currentKeyboard
        return this
    }

    fun setUnreadCount(unreadCount: Int): SimpleDialogEntity {
        this.unreadCount = unreadCount
        return this
    }

    fun setPhoto50(photo50: String?): SimpleDialogEntity {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): SimpleDialogEntity {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): SimpleDialogEntity {
        this.photo200 = photo200
        return this
    }

    fun setInRead(inRead: Int): SimpleDialogEntity {
        this.inRead = inRead
        return this
    }

    fun setOutRead(outRead: Int): SimpleDialogEntity {
        this.outRead = outRead
        return this
    }

    fun setGroupChannel(groupChannel: Boolean): SimpleDialogEntity {
        isGroupChannel = groupChannel
        return this
    }

    fun setMajor_id(major_id: Int): SimpleDialogEntity {
        this.major_id = major_id
        return this
    }

    fun setMinor_id(minor_id: Int): SimpleDialogEntity {
        this.minor_id = minor_id
        return this
    }
}