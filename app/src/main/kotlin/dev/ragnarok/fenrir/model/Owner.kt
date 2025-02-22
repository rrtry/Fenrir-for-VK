package dev.ragnarok.fenrir.model

import android.os.Parcel
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import kotlinx.serialization.Serializable

@Serializable
sealed class Owner : AbsModel, ParcelNative.ParcelableNative {
    @OwnerType
    val ownerType: Int

    constructor(ownerType: Int) {
        this.ownerType = ownerType
    }

    constructor(`in`: Parcel) : super(`in`) {
        ownerType = `in`.readInt()
    }

    constructor(`in`: ParcelNative) {
        ownerType = `in`.readInt()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(ownerType)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(ownerType)
    }

    open val ownerId: Int
        get() {
            throw UnsupportedOperationException()
        }
    open val domain: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val maxSquareAvatar: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val originalAvatar: String?
        get() {
            throw UnsupportedOperationException()
        }

    open fun get100photoOrSmaller(): String? {
        throw UnsupportedOperationException()
    }

    open val fullName: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val isVerified: Boolean
        get() {
            throw UnsupportedOperationException()
        }
    open val isDonated: Boolean
        get() {
            throw UnsupportedOperationException()
        }
}