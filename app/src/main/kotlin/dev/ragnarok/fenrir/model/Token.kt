package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Token : Parcelable {
    val ownerId: Int
    val accessToken: String?

    constructor(ownerId: Int, accessToken: String?) {
        this.ownerId = ownerId
        this.accessToken = accessToken
    }

    private constructor(`in`: Parcel) {
        ownerId = `in`.readInt()
        accessToken = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ownerId)
        dest.writeString(accessToken)
    }

    override fun toString(): String {
        return "Token{" +
                "ownerId=" + ownerId +
                ", accessToken='" + accessToken + '\'' +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<Token> {
        override fun createFromParcel(parcel: Parcel): Token {
            return Token(parcel)
        }

        override fun newArray(size: Int): Array<Token?> {
            return arrayOfNulls(size)
        }
    }
}