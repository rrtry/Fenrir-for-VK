package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.orZero

class Keyboard : Parcelable {
    var one_time = false
        private set
    var inline = false
        private set
    var author_id = 0
        private set
    var buttons: ArrayList<List<Button>>? = null
        private set

    constructor()
    private constructor(`in`: Parcel) {
        one_time = `in`.readByte().toInt() != 0
        inline = `in`.readByte().toInt() != 0
        author_id = `in`.readInt()
        val size = `in`.readInt()
        buttons = ArrayList(size)
        for (i in 0 until size) {
            `in`.createTypedArrayList(Button.CREATOR)?.let { buttons?.add(i, it) }
        }
    }

    fun setOne_time(one_time: Boolean): Keyboard {
        this.one_time = one_time
        return this
    }

    fun setInline(inline: Boolean): Keyboard {
        this.inline = inline
        return this
    }

    fun setAuthor_id(author_id: Int): Keyboard {
        this.author_id = author_id
        return this
    }

    fun setButtons(buttons: ArrayList<List<Button>>?): Keyboard {
        this.buttons = buttons
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (one_time) 1 else 0).toByte())
        dest.writeByte((if (inline) 1 else 0).toByte())
        dest.writeInt(author_id)
        if (buttons == null) {
            dest.writeInt(0)
        } else {
            dest.writeInt(buttons?.size.orZero())
            for (i in buttons.orEmpty()) {
                dest.writeTypedList(i)
            }
        }
    }

    class Button : Parcelable {
        var color: String? = null
            private set
        var type: String? = null
            private set
        var label: String? = null
            private set
        var link: String? = null
            private set
        var payload: String? = null
            private set

        constructor()
        private constructor(`in`: Parcel) {
            color = `in`.readString()
            type = `in`.readString()
            label = `in`.readString()
            link = `in`.readString()
            payload = `in`.readString()
        }

        fun setColor(color: String?): Button {
            this.color = color
            return this
        }

        fun setType(type: String?): Button {
            this.type = type
            return this
        }

        fun setLabel(label: String?): Button {
            this.label = label
            return this
        }

        fun setLink(link: String?): Button {
            this.link = link
            return this
        }

        fun setPayload(payload: String?): Button {
            this.payload = payload
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(color)
            dest.writeString(type)
            dest.writeString(label)
            dest.writeString(link)
            dest.writeString(payload)
        }

        companion object CREATOR : Parcelable.Creator<Button> {
            override fun createFromParcel(parcel: Parcel): Button {
                return Button(parcel)
            }

            override fun newArray(size: Int): Array<Button?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Keyboard> {
        override fun createFromParcel(parcel: Parcel): Keyboard {
            return Keyboard(parcel)
        }

        override fun newArray(size: Int): Array<Keyboard?> {
            return arrayOfNulls(size)
        }
    }
}