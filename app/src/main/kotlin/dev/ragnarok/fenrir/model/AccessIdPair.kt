package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class AccessIdPair : Parcelable {
    private val id: Int
    private val ownerId: Int
    private val accessKey: String?

    constructor(id: Int, ownerId: Int, accessKey: String?) {
        this.id = id
        this.ownerId = ownerId
        this.accessKey = accessKey
    }

    private constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        accessKey = `in`.readString()
    }

    fun getId(): Int {
        return id
    }

    fun getOwnerId(): Int {
        return ownerId
    }

    fun getAccessKey(): String? {
        return accessKey
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(accessKey)
    }

    companion object CREATOR : Parcelable.Creator<AccessIdPair> {
        override fun createFromParcel(parcel: Parcel): AccessIdPair {
            return AccessIdPair(parcel)
        }

        override fun newArray(size: Int): Array<AccessIdPair?> {
            return arrayOfNulls(size)
        }
    }
}