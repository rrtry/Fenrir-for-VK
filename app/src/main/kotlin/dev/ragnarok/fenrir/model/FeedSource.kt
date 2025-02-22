package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.adapter.horizontal.Entry

class FeedSource : Entry, Parcelable {
    private val value: String?
    private val title: Text?
    private var active = false
    private var custom: Boolean

    constructor(value: String?, title: String?, custom: Boolean) {
        this.value = value
        this.title = Text(title)
        this.custom = custom
    }

    constructor(value: String?, @StringRes title: Int, custom: Boolean) {
        this.value = value
        this.title = Text(title)
        this.custom = custom
    }

    private constructor(`in`: Parcel) {
        value = `in`.readString()
        title = `in`.readParcelable(Text::class.java.classLoader)
        active = `in`.readByte().toInt() != 0
        custom = `in`.readByte().toInt() != 0
    }

    fun getValue(): String? {
        return value
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(value)
        dest.writeParcelable(title, flags)
        dest.writeByte((if (active) 1 else 0).toByte())
        dest.writeByte((if (custom) 1 else 0).toByte())
    }

    override fun getTitle(context: Context): String? {
        return title?.getText(context)
    }

    override val isActive: Boolean
        get() = active

    fun setActive(active: Boolean): FeedSource {
        this.active = active
        return this
    }

    override val isCustom: Boolean
        get() = custom

    fun setCustom(custom: Boolean): FeedSource {
        this.custom = custom
        return this
    }

    companion object CREATOR : Parcelable.Creator<FeedSource> {
        override fun createFromParcel(parcel: Parcel): FeedSource {
            return FeedSource(parcel)
        }

        override fun newArray(size: Int): Array<FeedSource?> {
            return arrayOfNulls(size)
        }
    }
}