package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class DialogsSearchCriteria : BaseSearchCriteria {
    constructor(query: String?) : super(query)
    private constructor(`in`: Parcel) : super(`in`)

    companion object CREATOR : Parcelable.Creator<DialogsSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): DialogsSearchCriteria {
            return DialogsSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<DialogsSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}