package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FwdMessages : AbsModel {
    val fwds: ArrayList<Message>

    constructor(fwds: ArrayList<Message>) {
        this.fwds = fwds
    }

    private constructor(`in`: Parcel) : super(`in`) {
        fwds = `in`.createTypedArrayList(Message.CREATOR)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeTypedList(fwds)
    }

    companion object CREATOR : Parcelable.Creator<FwdMessages> {
        override fun createFromParcel(parcel: Parcel): FwdMessages {
            return FwdMessages(parcel)
        }

        override fun newArray(size: Int): Array<FwdMessages?> {
            return arrayOfNulls(size)
        }
    }
}