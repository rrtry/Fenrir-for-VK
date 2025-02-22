package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.AudioPlaylistDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = AudioPlaylistDtoAdapter::class)
class VKApiAudioPlaylist : VKApiAttachment {
    var id = 0
    var owner_id = 0
    var count = 0
    var update_time: Long = 0
    var Year = 0
    var artist_name: String? = null
    var genre: String? = null
    var title: String? = null
    var description: String? = null
    var thumb_image: String? = null
    var access_key: String? = null
    var original_access_key: String? = null
    var original_id = 0
    var original_owner_id = 0
    override fun getType(): String {
        return VKApiAttachment.TYPE_AUDIO_PLAYLIST
    }
}