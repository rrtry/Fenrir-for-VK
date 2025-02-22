package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Topic : AbsModel {
    val id: Int
    val ownerId: Int
    var title: String? = null
        private set
    var creationTime: Long = 0
        private set
    var createdByOwnerId = 0
        private set
    var lastUpdateTime: Long = 0
        private set
    var updatedByOwnerId = 0
        private set
    var isClosed = false
        private set
    var isFixed = false
        private set
    var commentsCount = 0
        private set
    var firstCommentBody: String? = null
        private set
    var lastCommentBody: String? = null
        private set
    var creator: Owner? = null
        private set
    var updater: Owner? = null
        private set

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        title = `in`.readString()
        creationTime = `in`.readLong()
        createdByOwnerId = `in`.readInt()
        lastUpdateTime = `in`.readLong()
        updatedByOwnerId = `in`.readInt()
        isClosed = `in`.readByte().toInt() != 0
        isFixed = `in`.readByte().toInt() != 0
        commentsCount = `in`.readInt()
        firstCommentBody = `in`.readString()
        lastCommentBody = `in`.readString()
        creator =
            (`in`.readParcelable<Parcelable>(ParcelableOwnerWrapper::class.java.classLoader) as ParcelableOwnerWrapper?)!!.get()
        updater =
            (`in`.readParcelable<Parcelable>(ParcelableOwnerWrapper::class.java.classLoader) as ParcelableOwnerWrapper?)!!.get()
    }

    fun setTitle(title: String?): Topic {
        this.title = title
        return this
    }

    fun setCreationTime(creationTime: Long): Topic {
        this.creationTime = creationTime
        return this
    }

    fun setCreatedByOwnerId(createdByOwnerId: Int): Topic {
        this.createdByOwnerId = createdByOwnerId
        return this
    }

    fun setLastUpdateTime(lastUpdateTime: Long): Topic {
        this.lastUpdateTime = lastUpdateTime
        return this
    }

    fun setUpdatedByOwnerId(updatedByOwnerId: Int): Topic {
        this.updatedByOwnerId = updatedByOwnerId
        return this
    }

    fun setClosed(closed: Boolean): Topic {
        isClosed = closed
        return this
    }

    fun setFixed(fixed: Boolean): Topic {
        isFixed = fixed
        return this
    }

    fun setCommentsCount(commentsCount: Int): Topic {
        this.commentsCount = commentsCount
        return this
    }

    fun setFirstCommentBody(firstCommentBody: String?): Topic {
        this.firstCommentBody = firstCommentBody
        return this
    }

    fun setLastCommentBody(lastCommentBody: String?): Topic {
        this.lastCommentBody = lastCommentBody
        return this
    }

    fun setCreator(creator: Owner?): Topic {
        this.creator = creator
        return this
    }

    fun setUpdater(updater: Owner?): Topic {
        this.updater = updater
        return this
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(title)
        parcel.writeLong(creationTime)
        parcel.writeInt(createdByOwnerId)
        parcel.writeLong(lastUpdateTime)
        parcel.writeInt(updatedByOwnerId)
        parcel.writeByte((if (isClosed) 1 else 0).toByte())
        parcel.writeByte((if (isFixed) 1 else 0).toByte())
        parcel.writeInt(commentsCount)
        parcel.writeString(firstCommentBody)
        parcel.writeString(lastCommentBody)
        parcel.writeParcelable(ParcelableOwnerWrapper(creator), i)
        parcel.writeParcelable(ParcelableOwnerWrapper(updater), i)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Topic> {
        override fun createFromParcel(parcel: Parcel): Topic {
            return Topic(parcel)
        }

        override fun newArray(size: Int): Array<Topic?> {
            return arrayOfNulls(size)
        }
    }
}