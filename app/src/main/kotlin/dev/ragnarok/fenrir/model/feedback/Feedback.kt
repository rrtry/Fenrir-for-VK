package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.CallSuper
import dev.ragnarok.fenrir.model.Comment

abstract class Feedback : Parcelable {
    @FeedbackType
    val type: Int
    var date: Long = 0
        private set
    var reply: Comment? = null
        private set

    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    protected constructor(`in`: Parcel) {
        type = `in`.readInt()
        date = `in`.readLong()
        reply = `in`.readParcelable(Comment::class.java.classLoader)
    }

    fun setDate(date: Long): Feedback {
        this.date = date
        return this
    }

    fun setReply(reply: Comment?): Feedback {
        this.reply = reply
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    @CallSuper
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeLong(date)
        dest.writeParcelable(reply, flags)
    }
}