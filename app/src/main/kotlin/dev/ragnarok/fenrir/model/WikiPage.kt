package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class WikiPage : AbsModel {
    val id: Int
    val ownerId: Int
    var creatorId = 0
        private set
    var title: String? = null
        private set
    var source: String? = null
        private set
    var editionTime: Long = 0
        private set
    var creationTime: Long = 0
        private set
    var parent: String? = null
        private set
    var parent2: String? = null
        private set
    var views = 0
        private set
    var viewUrl: String? = null
        private set

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        creatorId = `in`.readInt()
        title = `in`.readString()
        source = `in`.readString()
        editionTime = `in`.readLong()
        creationTime = `in`.readLong()
        parent = `in`.readString()
        parent2 = `in`.readString()
        views = `in`.readInt()
        viewUrl = `in`.readString()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeInt(creatorId)
        parcel.writeString(title)
        parcel.writeString(source)
        parcel.writeLong(editionTime)
        parcel.writeLong(creationTime)
        parcel.writeString(parent)
        parcel.writeString(parent2)
        parcel.writeInt(views)
        parcel.writeString(viewUrl)
    }

    fun setCreatorId(creatorId: Int): WikiPage {
        this.creatorId = creatorId
        return this
    }

    fun setTitle(title: String?): WikiPage {
        this.title = title
        return this
    }

    fun setSource(source: String?): WikiPage {
        this.source = source
        return this
    }

    fun setEditionTime(editionTime: Long): WikiPage {
        this.editionTime = editionTime
        return this
    }

    fun setCreationTime(creationTime: Long): WikiPage {
        this.creationTime = creationTime
        return this
    }

    fun setParent(parent: String?): WikiPage {
        this.parent = parent
        return this
    }

    fun setParent2(parent2: String?): WikiPage {
        this.parent2 = parent2
        return this
    }

    fun setViews(views: Int): WikiPage {
        this.views = views
        return this
    }

    fun setViewUrl(viewUrl: String?): WikiPage {
        this.viewUrl = viewUrl
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WikiPage> {
        override fun createFromParcel(parcel: Parcel): WikiPage {
            return WikiPage(parcel)
        }

        override fun newArray(size: Int): Array<WikiPage?> {
            return arrayOfNulls(size)
        }
    }
}