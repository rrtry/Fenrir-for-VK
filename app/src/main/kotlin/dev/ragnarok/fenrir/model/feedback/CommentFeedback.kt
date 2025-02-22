package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel

class CommentFeedback : Feedback {
    var commentOf: AbsModel? = null
        private set
    var comment: Comment? = null
        private set

    constructor(@FeedbackType type: Int) : super(type)
    private constructor(`in`: Parcel) : super(`in`) {
        commentOf = readModel(`in`)
        comment = `in`.readParcelable(Comment::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeModel(dest, flags, commentOf)
        dest.writeParcelable(comment, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setCommentOf(commentOf: AbsModel?): CommentFeedback {
        this.commentOf = commentOf
        return this
    }

    fun setComment(comment: Comment?): CommentFeedback {
        this.comment = comment
        return this
    }

    companion object CREATOR : Parcelable.Creator<CommentFeedback> {
        override fun createFromParcel(parcel: Parcel): CommentFeedback {
            return CommentFeedback(parcel)
        }

        override fun newArray(size: Int): Array<CommentFeedback?> {
            return arrayOfNulls(size)
        }
    }
}