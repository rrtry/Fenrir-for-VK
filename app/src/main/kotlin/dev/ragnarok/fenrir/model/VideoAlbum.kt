package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class VideoAlbum : AbsModel {
    private val id: Int
    private val ownerId: Int
    private var title: String? = null
    private var count = 0
    private var updatedTime: Long = 0
    private var image: String? = null
    private var privacy: SimplePrivacy? = null

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        title = `in`.readString()
        count = `in`.readInt()
        updatedTime = `in`.readLong()
        image = `in`.readString()
        privacy = `in`.readParcelable(SimplePrivacy::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(title)
        parcel.writeInt(count)
        parcel.writeLong(updatedTime)
        parcel.writeString(image)
        parcel.writeParcelable(privacy, i)
    }

    fun getId(): Int {
        return id
    }

    fun getOwnerId(): Int {
        return ownerId
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): VideoAlbum {
        this.title = title
        return this
    }

    fun getPrivacy(): SimplePrivacy? {
        return privacy
    }

    fun setPrivacy(privacy: SimplePrivacy?): VideoAlbum {
        this.privacy = privacy
        return this
    }

    fun getCount(): Int {
        return count
    }

    fun setCount(count: Int): VideoAlbum {
        this.count = count
        return this
    }

    fun getUpdatedTime(): Long {
        return updatedTime
    }

    fun setUpdatedTime(updatedTime: Long): VideoAlbum {
        this.updatedTime = updatedTime
        return this
    }

    fun getImage(): String? {
        return image
    }

    fun setImage(image: String?): VideoAlbum {
        this.image = image
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoAlbum> {
        override fun createFromParcel(parcel: Parcel): VideoAlbum {
            return VideoAlbum(parcel)
        }

        override fun newArray(size: Int): Array<VideoAlbum?> {
            return arrayOfNulls(size)
        }
    }
}