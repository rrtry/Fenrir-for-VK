package dev.ragnarok.fenrir.model.drawer

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class IconMenuItem : SectionMenuItem {
    @DrawableRes
    var icon: Int

    constructor(
        section: Int,
        @DrawableRes icon: Int,
        @StringRes title: Int
    ) : super(TYPE_ICON, section, title) {
        this.icon = icon
    }

    private constructor(`in`: Parcel) : super(`in`) {
        icon = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(icon)
    }

    companion object CREATOR : Parcelable.Creator<IconMenuItem> {
        override fun createFromParcel(parcel: Parcel): IconMenuItem {
            return IconMenuItem(parcel)
        }

        override fun newArray(size: Int): Array<IconMenuItem?> {
            return arrayOfNulls(size)
        }
    }
}