package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero

class WallReply : AbsModel, Identificable {
    private var id = 0
    var fromId = 0
        private set
    var postId = 0
        private set
    var author: Owner? = null
        private set
    var ownerId = 0
        private set
    var text: String? = null
        private set
    var attachments: Attachments? = null
        private set

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        fromId = `in`.readInt()
        postId = `in`.readInt()
        ownerId = `in`.readInt()
        text = `in`.readString()
        attachments = `in`.readParcelable(Attachments::class.java.classLoader)
        author = readOwner(`in`)
    }

    constructor()

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(fromId)
        parcel.writeInt(postId)
        parcel.writeInt(ownerId)
        parcel.writeString(text)
        parcel.writeParcelable(attachments, i)
        writeOwner(parcel, i, author)
    }

    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun buildFromComment(comment: Comment, commented: Commented?): WallReply {
        id = comment.getObjectId()
        fromId = comment.fromId
        author = comment.author
        text = comment.text
        ownerId = commented?.sourceOwnerId ?: comment.commented.sourceOwnerId
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    fun hasAttachments(): Boolean {
        return attachments?.hasAttachments == true
    }

    fun hasStickerOnly(): Boolean {
        return attachments?.stickers.nonNullNoEmpty()
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): WallReply {
        this.id = id
        return this
    }

    fun setOwnerId(owner_id: Int): WallReply {
        ownerId = owner_id
        return this
    }

    fun setAuthor(author: Owner?): WallReply {
        this.author = author
        return this
    }

    val authorPhoto: String?
        get() = author?.maxSquareAvatar
    val authorName: String?
        get() = author?.fullName

    fun setPostId(post_id: Int): WallReply {
        postId = post_id
        return this
    }

    fun setFromId(from_id: Int): WallReply {
        fromId = from_id
        return this
    }

    fun setText(text: String?): WallReply {
        this.text = text
        return this
    }

    fun setAttachments(attachments: Attachments?): WallReply {
        this.attachments = attachments
        return this
    }

    companion object CREATOR : Parcelable.Creator<WallReply> {
        override fun createFromParcel(parcel: Parcel): WallReply {
            return WallReply(parcel)
        }

        override fun newArray(size: Int): Array<WallReply?> {
            return arrayOfNulls(size)
        }
    }
}