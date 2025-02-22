package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable

class LocalGallerySelectableSource : AbsSelectableSource {
    constructor() : super(Types.LOCAL_GALLERY)
    private constructor(`in`: Parcel) : super(`in`)

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocalGallerySelectableSource> {
        override fun createFromParcel(parcel: Parcel): LocalGallerySelectableSource {
            return LocalGallerySelectableSource(parcel)
        }

        override fun newArray(size: Int): Array<LocalGallerySelectableSource?> {
            return arrayOfNulls(size)
        }
    }
}