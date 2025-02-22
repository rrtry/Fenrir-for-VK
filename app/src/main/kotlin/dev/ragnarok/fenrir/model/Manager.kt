package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Manager : Parcelable {
    val user: User?
    val role: String?
    var isDisplayAsContact = false
        private set
    var contactInfo: ContactInfo? = null
        private set

    constructor(user: User?, role: String?) {
        this.user = user
        this.role = role
    }

    private constructor(`in`: Parcel) {
        user = `in`.readParcelable(User::class.java.classLoader)
        isDisplayAsContact = `in`.readByte().toInt() != 0
        role = `in`.readString()
    }

    fun setContactInfo(contactInfo: ContactInfo?): Manager {
        this.contactInfo = contactInfo
        return this
    }

    fun setDisplayAsContact(displayAsContact: Boolean): Manager {
        isDisplayAsContact = displayAsContact
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(user, flags)
        dest.writeByte((if (isDisplayAsContact) 1 else 0).toByte())
        dest.writeString(role)
    }

    companion object CREATOR : Parcelable.Creator<Manager> {
        override fun createFromParcel(parcel: Parcel): Manager {
            return Manager(parcel)
        }

        override fun newArray(size: Int): Array<Manager?> {
            return arrayOfNulls(size)
        }
    }
}