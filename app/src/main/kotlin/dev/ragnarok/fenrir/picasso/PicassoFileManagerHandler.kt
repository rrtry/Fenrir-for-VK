package dev.ragnarok.fenrir.picasso

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.squareup.picasso3.BitmapUtils
import com.squareup.picasso3.Picasso
import com.squareup.picasso3.Request
import com.squareup.picasso3.RequestHandler
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.StringHash
import dev.ragnarok.fenrir.module.animation.AnimatedFileFrame
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.CoverSafeResize
import dev.ragnarok.fenrir.util.Utils
import okio.Source
import okio.source
import java.io.*
import java.util.*

class PicassoFileManagerHandler(val context: Context) : RequestHandler() {
    companion object {
        fun toSha1(str: String): String {
            return StringHash.calculateSha1(str)
        }
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri!!.path != null && data.uri!!.lastPathSegment != null && "thumb_file" == data.uri?.scheme
    }

    private fun getMetadataAudioThumbnail(uri: Uri): Bitmap? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        return try {
            mediaMetadataRetriever.setDataSource(Includes.provideApplicationContext(), uri)
            val cover = mediaMetadataRetriever.embeddedPicture ?: return null
            BitmapFactory.decodeStream(ByteArrayInputStream(cover))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Throws(FileNotFoundException::class)
    fun getSource(uri: Uri): Source {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("can't open input stream, uri: $uri")
        return inputStream.source()
    }

    private val filter: FilenameFilter = FilenameFilter { dir: File, filename: String ->
        val sel = File(dir, filename)
        var ret = !sel.isHidden && sel.canRead() && !sel.isDirectory
        // Filters based on whether the file is hidden or not
        if (ret) {
            ret = false
            for (i in Settings.get().other().photoExt()) {
                if (sel.extension.contains(i, true)) {
                    ret = true
                    break
                }
            }
            if (!ret) {
                for (i in Settings.get().other().audioExt()) {
                    if (sel.extension.contains(i, true)) {
                        ret = true
                        break
                    }
                }
            }
            if (!ret) {
                for (i in Settings.get().other().videoExt()) {
                    if (sel.extension.contains(i, true)) {
                        ret = true
                        break
                    }
                }
            }
        }
        ret
    }

    private class ItemModificationComparator : Comparator<File> {
        override fun compare(lhs: File, rhs: File): Int {
            return rhs.lastModified().compareTo(lhs.lastModified())
        }
    }

    private fun getExifRotation(orientation: Int) =
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }

    private fun isExtension(str: String, ext: Set<String>): Boolean {
        var ret = false
        for (i in ext) {
            if (str.endsWith(i, true)) {
                ret = true
                break
            }
        }
        return ret
    }

    private fun work(requestUri: Uri, dir: File, request: Request, callback: Callback) {
        if (dir.exists()) {
            if (dir.length() <= 0) {
                callback.onError(Throwable("Cache file empty"))
                return
            }
            try {
                callback.onSuccess(
                    Result.Bitmap(
                        BitmapUtils.decodeStream(
                            getSource(Uri.fromFile(dir)),
                            request
                        ), Picasso.LoadedFrom.DISK
                    )
                )
            } catch (e: Exception) {
                callback.onError(e)
            }
            return
        }
        when {
            isExtension(requestUri.toString(), Settings.get().other().audioExt()) -> {
                var target = getMetadataAudioThumbnail(requestUri)
                if (target == null) {
                    dir.createNewFile()
                    callback.onError(Throwable("Thumb work error"))
                    return
                } else {
                    target = CoverSafeResize.checkBitmap(target)
                    val fOutputStream = FileOutputStream(dir)
                    target.compress(
                        if (Utils.hasR()) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG,
                        95,
                        fOutputStream
                    )

                    fOutputStream.flush()
                    fOutputStream.close()
                }
                callback.onSuccess(Result.Bitmap(target, Picasso.LoadedFrom.DISK))
                return
            }
            isExtension(
                requestUri.toString(),
                Settings.get().other().videoExt()
            ) || requestUri.toString().endsWith("gif", true) -> {
                var target =
                    if (FenrirNative.isNativeLoaded) AnimatedFileFrame.getThumbnail(requestUri.toFile().absoluteFile) else null
                if (target == null) {
                    dir.createNewFile()
                    callback.onError(Throwable("Thumb work error"))
                    return
                } else {
                    target = CoverSafeResize.checkBitmap(target)
                    val fOutputStream = FileOutputStream(dir)
                    target.compress(
                        if (Utils.hasR()) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG,
                        95,
                        fOutputStream
                    )

                    fOutputStream.flush()
                    fOutputStream.close()
                }
                callback.onSuccess(Result.Bitmap(target, Picasso.LoadedFrom.DISK))
                return
            }
            isExtension(requestUri.toString(), Settings.get().other().photoExt()) -> {
                var target: Bitmap
                try {
                    target = BitmapUtils.decodeStream(getSource(requestUri), request)
                } catch (e: Exception) {
                    dir.createNewFile()
                    callback.onError(Throwable("Thumb work error"))
                    return
                }
                var exifOrientation = 0
                try {
                    exifOrientation = getExifRotation(getExifOrientation(requestUri))
                } catch (e: Exception) {
                    if (Constants.IS_DEBUG) {
                        e.printStackTrace()
                    }
                }
                target = CoverSafeResize.checkBitmap(target)
                if (exifOrientation > 0) {
                    val matrix = Matrix()
                    matrix.postRotate(exifOrientation.toFloat())
                    target = Bitmap.createBitmap(
                        target, 0, 0,
                        target.width, target.height, matrix, true
                    )
                }
                val fOutputStream = FileOutputStream(dir)
                target.compress(
                    if (Utils.hasR()) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG,
                    95,
                    fOutputStream
                )

                fOutputStream.flush()
                fOutputStream.close()
                callback.onSuccess(Result.Bitmap(target, Picasso.LoadedFrom.DISK))
                return
            }
            else -> callback.onError(Throwable("Thumb not handle"))
        }
    }

    private fun prepareDirectory(requestUri: Uri, request: Request, callback: Callback): Boolean {
        val direct: File
        try {
            direct = requestUri.toFile()
            if (!direct.isDirectory) {
                return false
            }
        } catch (e: Exception) {
            return false
        }

        val dir = File(
            PicassoInstance.getCoversPath(context),
            "thumb_" + toSha1(direct.absolutePath + direct.lastModified()) + ".jpg"
        )
        if (dir.exists()) {
            if (dir.length() <= 0) {
                callback.onError(Throwable("Cache file empty"))
                return true
            }
            try {
                callback.onSuccess(
                    Result.Bitmap(
                        BitmapUtils.decodeStream(
                            getSource(Uri.fromFile(dir)),
                            request
                        ), Picasso.LoadedFrom.DISK
                    )
                )
            } catch (e: Exception) {
                callback.onError(e)
            }
            return true
        }

        val fList = direct.listFiles(filter)
        val dst = if (fList != null && fList.isNotEmpty()) {
            val tmp = fList.toList()
            Collections.sort(tmp, ItemModificationComparator())
            tmp[0]
        } else {
            null
        }
        if (dst == null) {
            callback.onError(Throwable("Thumb not handle"))
        } else {
            work(Uri.parse("file://" + dst.absolutePath), dir, request, callback)
        }
        return true

    }

    private fun getExifOrientation(uri: Uri): Int {
        val path = uri.path ?: throw FileNotFoundException("path == null, uri: $uri")
        return ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }

    override fun load(picasso: Picasso, request: Request, callback: Callback) {
        val requestUri = Uri.parse(
            checkNotNull(request.uri) { "request.uri == null" }.toString()
                .replace("thumb_", "")
        )
        if (prepareDirectory(requestUri, request, callback)) {
            return
        }
        val pt = requestUri.toFile()

        val dir = File(
            PicassoInstance.getCoversPath(context),
            "thumb_" + toSha1(pt.absolutePath + pt.lastModified()) + ".jpg"
        )
        work(requestUri, dir, request, callback)
    }
}
