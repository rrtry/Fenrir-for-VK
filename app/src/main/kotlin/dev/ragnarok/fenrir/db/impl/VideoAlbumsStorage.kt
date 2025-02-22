package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.MessengerContentProvider
import dev.ragnarok.fenrir.db.MessengerContentProvider.Companion.getVideoAlbumsContentUriFor
import dev.ragnarok.fenrir.db.column.VideoAlbumsColumns
import dev.ragnarok.fenrir.db.interfaces.IVideoAlbumsStorage
import dev.ragnarok.fenrir.db.model.entity.PrivacyEntity
import dev.ragnarok.fenrir.db.model.entity.VideoAlbumDboEntity
import dev.ragnarok.fenrir.model.VideoAlbumCriteria
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class VideoAlbumsStorage(base: AppStorages) : AbsStorage(base), IVideoAlbumsStorage {
    override fun findByCriteria(criteria: VideoAlbumCriteria): Single<List<VideoAlbumDboEntity>> {
        return Single.create { e: SingleEmitter<List<VideoAlbumDboEntity>> ->
            val uri = getVideoAlbumsContentUriFor(criteria.getAccountId())
            val where: String
            val args: Array<String>
            val range = criteria.getRange()
            if (range != null) {
                where = VideoAlbumsColumns.OWNER_ID + " = ? " +
                        " AND " + BaseColumns._ID + " >= ? " +
                        " AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(
                    criteria.getOwnerId().toString(),
                    range.first.toString(),
                    range.last.toString()
                )
            } else {
                where = VideoAlbumsColumns.OWNER_ID + " = ?"
                args = arrayOf(criteria.getOwnerId().toString())
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val data: MutableList<VideoAlbumDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    data.add(mapAlbum(cursor))
                }
                cursor.close()
            }
            e.onSuccess(data)
        }
    }

    override fun insertData(
        accountId: Int,
        ownerId: Int,
        data: List<VideoAlbumDboEntity>,
        invalidateBefore: Boolean
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val uri = getVideoAlbumsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (invalidateBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .withSelection(
                            VideoAlbumsColumns.OWNER_ID + " = ?",
                            arrayOf(ownerId.toString())
                        )
                        .build()
                )
            }
            for (dbo in data) {
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(getCV(dbo))
                        .build()
                )
            }
            contentResolver.applyBatch(MessengerContentProvider.AUTHORITY, operations)
            e.onComplete()
        }
    }

    private fun mapAlbum(cursor: Cursor): VideoAlbumDboEntity {
        val id = cursor.getInt(VideoAlbumsColumns.ALBUM_ID)
        val ownerId = cursor.getInt(VideoAlbumsColumns.OWNER_ID)
        var privacyEntity: PrivacyEntity? = null
        val privacyJson = cursor.getBlob(VideoAlbumsColumns.PRIVACY)
        if (privacyJson.nonNullNoEmpty()) {
            privacyEntity = MsgPack.decodeFromByteArray(PrivacyEntity.serializer(), privacyJson)
        }
        return VideoAlbumDboEntity(id, ownerId)
            .setTitle(cursor.getString(VideoAlbumsColumns.TITLE))
            .setUpdateTime(cursor.getLong(VideoAlbumsColumns.UPDATE_TIME))
            .setCount(cursor.getInt(VideoAlbumsColumns.COUNT))
            .setImage(cursor.getString(VideoAlbumsColumns.IMAGE))
            .setPrivacy(privacyEntity)
    }

    companion object {
        fun getCV(dbo: VideoAlbumDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(VideoAlbumsColumns.OWNER_ID, dbo.ownerId)
            cv.put(VideoAlbumsColumns.ALBUM_ID, dbo.id)
            cv.put(VideoAlbumsColumns.TITLE, dbo.title)
            cv.put(VideoAlbumsColumns.IMAGE, dbo.image)
            cv.put(VideoAlbumsColumns.COUNT, dbo.count)
            cv.put(VideoAlbumsColumns.UPDATE_TIME, dbo.updateTime)
            dbo.privacy.ifNonNull({
                cv.put(
                    VideoAlbumsColumns.PRIVACY,
                    MsgPack.encodeToByteArray(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(VideoAlbumsColumns.PRIVACY)
            })
            return cv
        }
    }
}