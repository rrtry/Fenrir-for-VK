package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPhotosApi
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.UploadChatPhotoResponse
import dev.ragnarok.fenrir.api.model.response.UploadOwnerPhotoResponse
import dev.ragnarok.fenrir.api.model.server.*
import dev.ragnarok.fenrir.api.services.IPhotosService
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Single

internal class PhotosApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IPhotosApi {
    override fun deleteAlbum(albumId: Int, groupId: Int?): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteAlbum(albumId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun restore(ownerId: Int?, photoId: Int): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.restore(ownerId, photoId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun delete(ownerId: Int?, photoId: Int): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.delete(ownerId, photoId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun deleteComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun restoreComment(ownerId: Int?, commentId: Int): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun editComment(
        ownerId: Int?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.editComment(
                    ownerId,
                    commentId,
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) })
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun createAlbum(
        title: String?,
        groupId: Int?,
        description: String?,
        privacyView: VKApiPrivacy?,
        privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?,
        commentsDisabled: Boolean?
    ): Single<VKApiPhotoAlbum> {
        val privacyViewTxt = privacyView?.buildJsonArray()
        val privacyCommentTxt = privacyComment?.buildJsonArray()
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .createAlbum(
                        title, groupId, description, privacyViewTxt, privacyCommentTxt,
                        integerFromBoolean(uploadByAdminsOnly), integerFromBoolean(commentsDisabled)
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun editAlbum(
        albumId: Int,
        title: String?,
        description: String?,
        ownerId: Int?,
        privacyView: VKApiPrivacy?,
        privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?,
        commentsDisabled: Boolean?
    ): Single<Boolean> {
        val privacyViewTxt = privacyView?.buildJsonArray()
        val privacyCommentTxt = privacyComment?.buildJsonArray()
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .editAlbum(
                        albumId, title, description, ownerId, privacyViewTxt, privacyCommentTxt,
                        integerFromBoolean(uploadByAdminsOnly), integerFromBoolean(commentsDisabled)
                    )
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun copy(ownerId: Int, photoId: Int, accessKey: String?): Single<Int> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.copy(ownerId, photoId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun createComment(
        ownerId: Int?, photoId: Int, fromGroup: Boolean?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, accessKey: String?, generatedUniqueId: Int?
    ): Single<Int> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .createComment(
                        ownerId,
                        photoId,
                        integerFromBoolean(fromGroup),
                        message,
                        replyToComment,
                        join(
                            attachments,
                            ","
                        ) { formatAttachmentToken(it) },
                        stickerId,
                        accessKey,
                        generatedUniqueId
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getComments(
        ownerId: Int?,
        photoId: Int,
        needLikes: Boolean?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        sort: String?,
        accessKey: String?,
        extended: Boolean?,
        fields: String?
    ): Single<DefaultCommentsResponse> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getComments(
                        ownerId, photoId, integerFromBoolean(needLikes), startCommentId,
                        offset, count, sort, accessKey, integerFromBoolean(extended), fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(ids: Collection<AccessIdPair>): Single<List<VKApiPhoto>> {
        val line = join(
            ids,
            ","
        ) { pair -> pair.ownerId.toString() + "_" + pair.id + if (pair.accessKey == null) "" else "_" + pair.accessKey }
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getById(line, 1, 1)
                    .map(extractResponseWithErrorHandling())
                    .map { photos ->

                        // пересохраняем access_key, потому что не получим в ответе
                        for (photo in photos) {
                            if (photo.access_key == null) {
                                photo.access_key = findAccessKey(ids, photo.id, photo.owner_id)
                            }
                        }
                        listEmptyIfNull(photos)
                    }
            }
    }

    override fun getUploadServer(albumId: Int, groupId: Int?): Single<VKApiUploadServer> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getUploadServer(albumId, groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun saveOwnerPhoto(
        server: String?,
        hash: String?,
        photo: String?
    ): Single<UploadOwnerPhotoResponse> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .saveOwnerPhoto(server, hash, photo)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getOwnerPhotoUploadServer(ownerId: Int?): Single<VKApiOwnerPhotoUploadServer> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getOwnerPhotoUploadServer(ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChatUploadServer(chat_id: Int?): Single<VKApiChatPhotoUploadServer> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getChatUploadServer(chat_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setChatPhoto(file: String?): Single<UploadChatPhotoResponse> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .setChatPhoto(file)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun saveWallPhoto(
        userId: Int?, groupId: Int?, photo: String?,
        server: Int, hash: String?, latitude: Double?,
        longitude: Double?, caption: String?
    ): Single<List<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .saveWallPhoto(
                        userId,
                        groupId,
                        photo,
                        server,
                        hash,
                        latitude,
                        longitude,
                        caption
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getWallUploadServer(groupId: Int?): Single<VKApiWallUploadServer> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .getWallUploadServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun save(
        albumId: Int, groupId: Int?, server: Int, photosList: String?,
        hash: String?, latitude: Double?, longitude: Double?, caption: String?
    ): Single<List<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service
                    .save(albumId, groupId, server, photosList, hash, latitude, longitude, caption)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        ownerId: Int?, albumId: String?, photoIds: Collection<Int?>?,
        rev: Boolean?, offset: Int?, count: Int?
    ): Single<Items<VKApiPhoto>> {
        val photos = join(photoIds, ",")
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service[ownerId, albumId, photos, integerFromBoolean(rev), 1, 1, offset, count]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUsersPhoto(
        ownerId: Int?,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getUserPhotos(ownerId, extended, sort, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAll(
        ownerId: Int?,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getAll(ownerId, extended, photo_sizes, offset, count, 0, 1, 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override val messagesUploadServer: Single<VKApiPhotoMessageServer>
        get() = provideService(IPhotosService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.messagesUploadServer
                    .map(extractResponseWithErrorHandling())
            }

    override fun saveMessagesPhoto(
        server: Int?,
        photo: String?,
        hash: String?
    ): Single<List<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.saveMessagesPhoto(server, photo, hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbums(
        ownerId: Int?, albumIds: Collection<Int?>?,
        offset: Int?, count: Int?, needSystem: Boolean?,
        needCovers: Boolean?
    ): Single<Items<VKApiPhotoAlbum>> {
        val ids = join(albumIds, ",")
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getAlbums(
                    ownerId,
                    ids,
                    offset,
                    count,
                    integerFromBoolean(needSystem),
                    integerFromBoolean(needCovers),
                    1
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getTags(
        ownerId: Int?,
        photo_id: Int?,
        access_key: String?
    ): Single<List<VKApiPhotoTags>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getTags(ownerId, photo_id, access_key).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun getAllComments(
        ownerId: Int?,
        album_id: Int?,
        need_likes: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiComment>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.getAllComments(ownerId, album_id, need_likes, offset, count).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun search(
        q: String?,
        lat_gps: Double?,
        long_gps: Double?,
        sort: Int?,
        radius: Int?,
        startTime: Long?,
        endTime: Long?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiPhoto>> {
        return provideService(IPhotosService::class.java, TokenType.USER)
            .flatMap { service ->
                service.search(
                    q,
                    lat_gps,
                    long_gps,
                    sort,
                    radius,
                    startTime,
                    endTime,
                    offset,
                    count
                ).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    companion object {
        private fun findAccessKey(data: Collection<AccessIdPair>, id: Int, ownerId: Int): String? {
            for (pair in data) {
                if (pair.id == id && pair.ownerId == ownerId) {
                    return pair.accessKey
                }
            }
            return null
        }
    }
}