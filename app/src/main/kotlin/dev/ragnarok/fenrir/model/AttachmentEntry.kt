package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AttachmentEntry : Parcelable {
    val id: Int
    val attachment: AbsModel
    var optionalId = 0
        private set
    var isCanDelete: Boolean
        private set
    var isAccompanying = false
        private set

    constructor(canDelete: Boolean, attachment: AbsModel) {
        isCanDelete = canDelete
        this.attachment = attachment
        id = ID_GEN.incrementAndGet()
    }

    private constructor(`in`: Parcel) {
        id = `in`.readInt()
        if (id > ID_GEN.toInt()) {
            ID_GEN.set(id)
        }
        optionalId = `in`.readInt()
        isCanDelete = `in`.readByte().toInt() != 0
        isAccompanying = `in`.readByte().toInt() != 0
        val wrapper: ParcelableModelWrapper =
            `in`.readParcelable(ParcelableModelWrapper::class.java.classLoader)!!
        attachment = wrapper.get()
    }

    fun setCanDelete(canDelete: Boolean): AttachmentEntry {
        isCanDelete = canDelete
        return this
    }

    fun setAccompanying(accompanying: Boolean): AttachmentEntry {
        isAccompanying = accompanying
        return this
    }

    fun setOptionalId(optionalId: Int): AttachmentEntry {
        this.optionalId = optionalId
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(optionalId)
        dest.writeByte((if (isCanDelete) 1 else 0).toByte())
        dest.writeByte((if (isAccompanying) 1 else 0).toByte())
        val wrapper = ParcelableModelWrapper.wrap(attachment)
        dest.writeParcelable(wrapper, flags)
    }

    companion object {
        private val ID_GEN = AtomicInteger(Random().nextInt(5000))

        @JvmField
        val CREATOR: Parcelable.Creator<AttachmentEntry> =
            object : Parcelable.Creator<AttachmentEntry> {
                override fun createFromParcel(parcel: Parcel): AttachmentEntry {
                    return AttachmentEntry(parcel)
                }

                override fun newArray(size: Int): Array<AttachmentEntry?> {
                    return arrayOfNulls(size)
                }
            }
    }
}