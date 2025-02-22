package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwner
import dev.ragnarok.fenrir.module.parcel.ParcelNative

class Story : AbsModel, ParcelNative.ParcelableNative {
    var id = 0
        private set
    var ownerId = 0
        private set
    var date: Long = 0
        private set
    var expires: Long = 0
        private set
    var isIs_expired = false
        private set
    var accessKey: String? = null
        private set
    var target_url: String? = null
        private set
    var photo: Photo? = null
        private set
    var video: Video? = null
        private set
    var owner: Owner? = null
        private set

    constructor()
    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        date = `in`.readLong()
        expires = `in`.readLong()
        isIs_expired = `in`.readByte().toInt() != 0
        accessKey = `in`.readString()
        target_url = `in`.readString()
        video = `in`.readParcelable(Video::class.java.classLoader)
        photo = `in`.readParcelable(Photo::class.java.classLoader)
        owner = readOwner(`in`)
    }

    private constructor(`in`: ParcelNative) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        date = `in`.readLong()
        expires = `in`.readLong()
        isIs_expired = `in`.readBoolean()
        accessKey = `in`.readString()
        target_url = `in`.readString()
        video = `in`.readParcelable(Video.NativeCreator)
        photo = `in`.readParcelable(Photo.NativeCreator)
        owner = readOwner(`in`)
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeLong(date)
        parcel.writeLong(expires)
        parcel.writeByte((if (isIs_expired) 1 else 0).toByte())
        parcel.writeString(accessKey)
        parcel.writeString(target_url)
        parcel.writeParcelable(video, i)
        parcel.writeParcelable(photo, i)
        writeOwner(parcel, i, owner)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeLong(date)
        dest.writeLong(expires)
        dest.writeBoolean(isIs_expired)
        dest.writeString(accessKey)
        dest.writeString(target_url)
        dest.writeParcelable(video)
        dest.writeParcelable(photo)
        writeOwner(dest, owner)
    }

    fun setPhoto(photo: Photo?): Story {
        this.photo = photo
        return this
    }

    fun setId(id: Int): Story {
        this.id = id
        return this
    }

    fun setVideo(video: Video?): Story {
        this.video = video
        return this
    }

    fun setOwnerId(ownerId: Int): Story {
        this.ownerId = ownerId
        return this
    }

    fun setDate(date: Long): Story {
        this.date = date
        return this
    }

    fun setExpires(expires_at: Long): Story {
        expires = expires_at
        return this
    }

    fun setIs_expired(is_expired: Boolean): Story {
        isIs_expired = is_expired
        return this
    }

    fun setAccessKey(access_key: String?): Story {
        accessKey = access_key
        return this
    }

    fun setOwner(author: Owner?): Story {
        owner = author
        return this
    }

    fun setTarget_url(target_url: String?): Story {
        this.target_url = target_url
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Story) return false
        return id == other.id && ownerId == other.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Story> = object : Parcelable.Creator<Story> {
            override fun createFromParcel(`in`: Parcel): Story {
                return Story(`in`)
            }

            override fun newArray(size: Int): Array<Story?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Story> =
            object : ParcelNative.Creator<Story> {
                override fun readFromParcelNative(dest: ParcelNative): Story {
                    return Story(dest)
                }
            }
    }
}