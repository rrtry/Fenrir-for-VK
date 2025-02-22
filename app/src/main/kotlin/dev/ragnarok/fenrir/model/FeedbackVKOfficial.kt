package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
class FeedbackVKOfficial {
    var footer: String? = null
    var header: String? = null
    var header_owner_avatar_url: String? = null
    var header_owner_id: Int? = null
    var text: String? = null
    var iconURL: String? = null
    var iconType: String? = null
    var hide_query: String? = null
    var time: Long = 0
    var images: ArrayList<ImageAdditional>? = null
    var attachments: ArrayList<Photo>? = null
    var action: Action? = null
    fun getImage(prefSize: Int): ImageAdditional? {
        var result: ImageAdditional? = null
        if (images.isNullOrEmpty()) return null
        for (image in images.orEmpty()) {
            if (result == null) {
                result = image
                continue
            }
            if (abs(image.calcAverageSize() - prefSize) < abs(result.calcAverageSize() - prefSize)) {
                result = image
            }
        }
        return result
    }

    @IntDef(Action_Types.MESSAGE, Action_Types.URL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Action_Types {
        companion object {
            const val MESSAGE = 0
            const val URL = 1
        }
    }

    @Serializable
    abstract class Action {
        @Action_Types
        abstract fun getType(): Int
    }

    @Serializable
    class ActionMessage(private val peerId: Int, private val messageId: Int) : Action() {
        fun getPeerId(): Int {
            return peerId
        }

        fun getMessageId(): Int {
            return messageId
        }

        override fun getType(): Int {
            return Action_Types.MESSAGE
        }
    }

    @Serializable
    class ActionURL(private val url: String?) : Action() {
        fun getUrl(): String? {
            return url
        }

        override fun getType(): Int {
            return Action_Types.URL
        }
    }

    @Serializable
    class ImageAdditional {
        @SerialName("url")
        var url: String? = null

        @SerialName("width")
        var width = 0

        @SerialName("height")
        var height = 0
        fun calcAverageSize(): Int {
            return (width + height) / 2
        }
    }

    @Serializable
    class Attachment {
        @SerialName("type")
        var type: String? = null

        @SerialName("object_id")
        var object_id: String? = null
    }
}