package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R

class PhotoAlbum : AbsModel, ISomeones {
    private val id: Int
    override var ownerId = 0
        private set
    private var size = 0
    private var title: String? = null
    private var description: String? = null
    private var canUpload = false
    private var updatedTime: Long = 0
    private var createdTime: Long = 0
    private var sizes: PhotoSizes? = null
    private var uploadByAdminsOnly = false
    private var commentsDisabled = false
    private var privacyView: SimplePrivacy? = null
    private var privacyComment: SimplePrivacy? = null

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        size = `in`.readInt()
        title = `in`.readString()
        description = `in`.readString()
        canUpload = `in`.readByte().toInt() != 0
        updatedTime = `in`.readLong()
        createdTime = `in`.readLong()
        sizes = `in`.readParcelable(PhotoSizes::class.java.classLoader)
        uploadByAdminsOnly = `in`.readByte().toInt() != 0
        commentsDisabled = `in`.readByte().toInt() != 0
        privacyView = `in`.readParcelable(SimplePrivacy::class.java.classLoader)
        privacyComment = `in`.readParcelable(SimplePrivacy::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeInt(size)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeByte((if (canUpload) 1 else 0).toByte())
        parcel.writeLong(updatedTime)
        parcel.writeLong(createdTime)
        parcel.writeParcelable(sizes, i)
        parcel.writeByte((if (uploadByAdminsOnly) 1 else 0).toByte())
        parcel.writeByte((if (commentsDisabled) 1 else 0).toByte())
        parcel.writeParcelable(privacyView, i)
        parcel.writeParcelable(privacyComment, i)
    }

    override fun getObjectId(): Int {
        return id
    }

    fun getSize(): Int {
        return size
    }

    fun setSize(size: Int): PhotoAlbum {
        this.size = size
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): PhotoAlbum {
        this.title = title
        return this
    }

    fun getDisplayTitle(context: Context): String? {
        return if (id == -9001 || id == -311) {
            if (id == -9001) {
                context.getString(R.string.all_photos)
            } else {
                context.getString(R.string.on_server)
            }
        } else title
    }

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?): PhotoAlbum {
        this.description = description
        return this
    }

    fun isCanUpload(): Boolean {
        return canUpload
    }

    fun setCanUpload(canUpload: Boolean): PhotoAlbum {
        this.canUpload = canUpload
        return this
    }

    fun getUpdatedTime(): Long {
        return updatedTime
    }

    fun setUpdatedTime(updatedTime: Long): PhotoAlbum {
        this.updatedTime = updatedTime
        return this
    }

    fun getCreatedTime(): Long {
        return createdTime
    }

    fun setCreatedTime(createdTime: Long): PhotoAlbum {
        this.createdTime = createdTime
        return this
    }

    fun getSizes(): PhotoSizes? {
        return sizes
    }

    fun setSizes(sizes: PhotoSizes?): PhotoAlbum {
        this.sizes = sizes
        return this
    }

    fun isUploadByAdminsOnly(): Boolean {
        return uploadByAdminsOnly
    }

    fun setUploadByAdminsOnly(uploadByAdminsOnly: Boolean): PhotoAlbum {
        this.uploadByAdminsOnly = uploadByAdminsOnly
        return this
    }

    fun isCommentsDisabled(): Boolean {
        return commentsDisabled
    }

    fun setCommentsDisabled(commentsDisabled: Boolean): PhotoAlbum {
        this.commentsDisabled = commentsDisabled
        return this
    }

    fun getPrivacyView(): SimplePrivacy? {
        return privacyView
    }

    fun setPrivacyView(privacyView: SimplePrivacy?): PhotoAlbum {
        this.privacyView = privacyView
        return this
    }

    fun getPrivacyComment(): SimplePrivacy? {
        return privacyComment
    }

    fun setPrivacyComment(privacyComment: SimplePrivacy?): PhotoAlbum {
        this.privacyComment = privacyComment
        return this
    }

    fun isSystem(): Boolean {
        return id < 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PhotoAlbum) return false
        return id == other.id && ownerId == other.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PhotoAlbum> {
        override fun createFromParcel(parcel: Parcel): PhotoAlbum {
            return PhotoAlbum(parcel)
        }

        override fun newArray(size: Int): Array<PhotoAlbum?> {
            return arrayOfNulls(size)
        }
    }
}