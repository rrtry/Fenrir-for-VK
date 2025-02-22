package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class City : Parcelable {
    val id: Int
    val title: String?
    var isImportant = false
        private set
    var area: String? = null
        private set
    var region: String? = null
        private set

    constructor(id: Int, title: String?) {
        this.id = id
        this.title = title
    }

    private constructor(`in`: Parcel) {
        id = `in`.readInt()
        title = `in`.readString()
        isImportant = `in`.readByte().toInt() != 0
        area = `in`.readString()
        region = `in`.readString()
    }

    fun setImportant(important: Boolean): City {
        isImportant = important
        return this
    }

    fun setArea(area: String?): City {
        this.area = area
        return this
    }

    fun setRegion(region: String?): City {
        this.region = region
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeByte((if (isImportant) 1 else 0).toByte())
        parcel.writeString(area)
        parcel.writeString(region)
    }

    companion object CREATOR : Parcelable.Creator<City> {
        override fun createFromParcel(parcel: Parcel): City {
            return City(parcel)
        }

        override fun newArray(size: Int): Array<City?> {
            return arrayOfNulls(size)
        }
    }
}