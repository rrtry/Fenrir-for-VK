package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class Chat : AbsModel {
    val id: Int
    var title: String? = null
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set

    constructor(id: Int) {
        this.id = id
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        title = `in`.readString()
        photo50 = `in`.readString()
        photo100 = `in`.readString()
        photo200 = `in`.readString()
    }

    fun setTitle(title: String?): Chat {
        this.title = title
        return this
    }

    fun setPhoto50(photo50: String?): Chat {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): Chat {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): Chat {
        this.photo200 = photo200
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(photo50)
        parcel.writeString(photo100)
        parcel.writeString(photo200)
    }

    val maxSquareAvatar: String?
        get() = firstNonEmptyString(photo200, photo100, photo50)

    companion object CREATOR : Parcelable.Creator<Chat> {
        override fun createFromParcel(parcel: Parcel): Chat {
            return Chat(parcel)
        }

        override fun newArray(size: Int): Array<Chat?> {
            return arrayOfNulls(size)
        }
    }
}