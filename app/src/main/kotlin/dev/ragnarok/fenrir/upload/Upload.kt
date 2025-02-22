package dev.ragnarok.fenrir.upload

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectLong
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectLong
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

open class Upload : AbsModel, Identificable {
    val accountId: Int

    /* Идентификатор обьекта загрузки, генерируется базой данных при вставке */
    private var id: Int

    /* Локальный путь к файлу */
    var fileUri: Uri? = null
        private set

    /* Идентификатор обьекта, к которому прикрепляется файл
       (локальный код сообщения, поста, комментария) */
    lateinit var destination: UploadDestination
        private set

    /* Размер изображения (только для изображений)*/
    var size = 0
        private set

    /* Текущий статус загрузки (QUEUE,UPLOADING,ERROR,CANCELLING)*/
    var status = 0

    /* Текущий прогресс загрузки */
    var progress = 0

    /* Текст ошибки, если она произошла */
    var errorText: String? = null

    /**
     * Дополнительные данные
     */
    var fileId: Long? = null
        private set
    var isAutoCommit = false
        private set

    constructor(accountId: Int) {
        this.accountId = accountId
        id = incrementedUploadId
    }

    private constructor(`in`: Parcel) : super(`in`) {
        accountId = `in`.readInt()
        id = `in`.readInt()
        fileUri = `in`.readParcelable(Uri::class.java.classLoader)
        destination = `in`.readParcelable(UploadDestination::class.java.classLoader)!!
        size = `in`.readInt()
        status = `in`.readInt()
        progress = `in`.readInt()
        errorText = `in`.readString()
        fileId = readObjectLong(`in`)
    }

    fun setAutoCommit(autoCommit: Boolean): Upload {
        isAutoCommit = autoCommit
        return this
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): Upload {
        this.id = id
        return this
    }

    fun hasThumbnail(): Boolean {
        return fileId != null
    }

    fun buildThumnailUri(): Uri {
        return buildUriForPicasso(Content_Local.PHOTO, fileId!!)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Upload
        return id == that.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        super.writeToParcel(parcel, i)
        parcel.writeInt(accountId)
        parcel.writeInt(id)
        parcel.writeParcelable(fileUri, i)
        parcel.writeParcelable(destination, i)
        parcel.writeInt(size)
        parcel.writeInt(status)
        parcel.writeInt(progress)
        parcel.writeString(errorText)
        writeObjectLong(parcel, fileId)
    }

    fun setFileUri(fileUri: Uri?): Upload {
        this.fileUri = fileUri
        return this
    }

    fun setDestination(destination: UploadDestination): Upload {
        this.destination = destination
        return this
    }

    fun setSize(size: Int): Upload {
        this.size = size
        return this
    }

    fun setStatus(status: Int): Upload {
        this.status = status
        return this
    }

    fun setProgress(progress: Int): Upload {
        this.progress = progress
        return this
    }

    fun setErrorText(errorText: String?): Upload {
        this.errorText = errorText
        return this
    }

    fun setFileId(fileId: Long?): Upload {
        this.fileId = fileId
        return this
    }

    private val incrementedUploadId: Int
        get() = IDGEN.incrementAndGet()

    companion object {
        const val IMAGE_SIZE_800 = 800
        const val IMAGE_SIZE_1200 = 1200
        const val IMAGE_SIZE_FULL = -1
        const val IMAGE_SIZE_CROPPING = -2
        const val STATUS_QUEUE = 1
        const val STATUS_UPLOADING = 2
        const val STATUS_ERROR = 3
        const val STATUS_CANCELLING = 4

        @JvmField
        val CREATOR: Parcelable.Creator<Upload> = object : Parcelable.Creator<Upload> {
            override fun createFromParcel(`in`: Parcel): Upload {
                return Upload(`in`)
            }

            override fun newArray(size: Int): Array<Upload?> {
                return arrayOfNulls(size)
            }
        }
        val IDGEN = AtomicInteger(Random().nextInt(5000))
    }
}