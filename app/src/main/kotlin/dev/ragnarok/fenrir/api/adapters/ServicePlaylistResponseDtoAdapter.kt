package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import dev.ragnarok.fenrir.api.model.response.ServicePlaylistResponse
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class ServicePlaylistResponseDtoAdapter :
    AbsAdapter<ServicePlaylistResponse>("ServicePlaylistResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ServicePlaylistResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.asJsonObject
        val dto = ServicePlaylistResponse()
        dto.playlists = ArrayList()
        if (checkArray(root["response"])) {
            val response = root.getAsJsonArray("response")
            for (i in response.orEmpty()) {
                if (checkObject(i)) {
                    dto.playlists?.add(
                        kJson.decodeFromJsonElement(VKApiAudioPlaylist.serializer(), i)
                    )
                }
            }
        } else if (checkObject(root["response"])) {
            val response = root["response"]
            dto.playlists?.add(
                kJson.decodeFromJsonElement(
                    VKApiAudioPlaylist.serializer(),
                    response ?: return dto
                )
            )
        }
        return dto
    }

    companion object {
        private val TAG = ServicePlaylistResponseDtoAdapter::class.java.simpleName
    }
}