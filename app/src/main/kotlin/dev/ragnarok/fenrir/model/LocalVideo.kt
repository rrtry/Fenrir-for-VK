package dev.ragnarok.fenrir.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

class LocalVideo : Parcelable, Comparable<LocalVideo>, ISelectable {
    private val id: Long
    private val data: Uri?
    private var size: Long = 0
    private var selected = false
    private var duration = 0
    private var index = 0
    private var title: String? = null

    constructor(id: Long, data: Uri?) {
        this.id = id
        this.data = data
    }

    private constructor(`in`: Parcel) {
        id = `in`.readLong()
        data = Uri.parse(`in`.readString())
        selected = `in`.readInt() == 1
        index = `in`.readInt()
        size = `in`.readLong()
        duration = `in`.readInt()
        title = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        if (data == null) {
            dest.writeString(null)
        } else {
            dest.writeString(data.toString())
        }
        dest.writeInt(if (selected) 1 else 0)
        dest.writeInt(index)
        dest.writeLong(size)
        dest.writeLong(duration.toLong())
        dest.writeString(title)
    }

    fun getId(): Long {
        return id
    }

    fun getData(): Uri? {
        return data
    }

    fun getSize(): Long {
        return size
    }

    fun setSize(size: Long): LocalVideo {
        this.size = size
        return this
    }

    fun getDuration(): Int {
        return duration
    }

    fun setDuration(duration: Int): LocalVideo {
        this.duration = duration
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): LocalVideo {
        this.title = title
        return this
    }

    fun getIndex(): Int {
        return index
    }

    fun setIndex(index: Int): LocalVideo {
        this.index = index
        return this
    }

    override fun compareTo(other: LocalVideo): Int {
        return index - other.index
    }

    override val isSelected: Boolean
        get() = selected

    fun setSelected(selected: Boolean): LocalVideo {
        this.selected = selected
        return this
    }

    companion object CREATOR : Parcelable.Creator<LocalVideo> {
        override fun createFromParcel(parcel: Parcel): LocalVideo {
            return LocalVideo(parcel)
        }

        override fun newArray(size: Int): Array<LocalVideo?> {
            return arrayOfNulls(size)
        }
    }
}