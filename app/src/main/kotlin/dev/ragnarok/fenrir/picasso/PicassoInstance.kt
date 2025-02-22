package dev.ragnarok.fenrir.picasso

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.MediaStore
import com.squareup.picasso3.BitmapSafeResize
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.ProxyUtil
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.CompressDefaultInterceptor
import dev.ragnarok.fenrir.util.CoverSafeResize
import dev.ragnarok.fenrir.util.Logger
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

class PicassoInstance @SuppressLint("CheckResult") private constructor(
    private val app: Context,
    private val proxySettings: IProxySettings
) {
    private var cache_data: Cache? = null

    @Volatile
    private var singleton: Picasso? = null
    private fun onProxyChanged() {
        synchronized(this) {
            if (singleton != null) {
                singleton?.shutdown()
                singleton = null
                cache_data?.flush()
            }
            Logger.d(TAG, "Picasso singleton shutdown")
        }
    }

    private fun getSingleton(): Picasso {
        if (singleton == null) {
            synchronized(this) {
                if (singleton == null) {
                    singleton = create()
                }
            }
        }
        return singleton!!
    }

    private fun getCache_data() {
        if (cache_data == null) {
            val cache = File(app.cacheDir, "picasso-cache")
            if (!cache.exists()) {
                cache.mkdirs()
            }
            cache_data = Cache(cache, calculateDiskCacheSize(cache))
        }
    }

    private fun create(): Picasso {
        Logger.d(TAG, "Picasso singleton creation")
        getCache_data()
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .cache(cache_data)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-VK-Android-Client", "new")
                    .addHeader("User-Agent", Constants.USER_AGENT(AccountType.BY_TYPE)).build()
                chain.proceed(request)
            }).addInterceptor(CompressDefaultInterceptor)
        if (Settings.get().other().isLimit_cache) {
            builder.addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "max-age=86400").build()
            })
        }
        ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
        BitmapSafeResize.setMaxResolution(Settings.get().other().maxBitmapResolution)
        BitmapSafeResize.setHardwareRendering(Settings.get().other().rendering_mode)
        CoverSafeResize.setMaxResolution(Settings.get().other().getMaxThumbResolution())
        return Picasso.Builder(app)
            .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
            .client(builder.build())
            .addRequestHandler(PicassoLocalRequestHandler())
            .addRequestHandler(PicassoMediaMetadataHandler())
            .addRequestHandler(PicassoFileManagerHandler(app))
            .addRequestHandler(PicassoFullLocalRequestHandler(app))
            .build()
    }

    companion object {
        private val TAG = PicassoInstance::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        private var instance: PicassoInstance? = null


        fun buildUriForPicasso(@Content_Local type: Int, id: Long): Uri {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return buildUriForPicassoNew(type, id)
            }
            when (type) {
                Content_Local.PHOTO -> return ContentUris.withAppendedId(
                    Uri.parse("content://media/external/images/media/"),
                    id
                )
                Content_Local.VIDEO -> return ContentUris.withAppendedId(
                    Uri.parse("content://media/external/videos/media/"),
                    id
                )
                Content_Local.AUDIO -> return ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audios/media/"),
                    id
                )
            }
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/images/media/"),
                id
            )
        }


        fun buildUriForPicassoNew(@Content_Local type: Int, id: Long): Uri {
            when (type) {
                Content_Local.PHOTO -> return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                Content_Local.VIDEO -> return ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                Content_Local.AUDIO -> return ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            }
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }

        fun init(context: Context, proxySettings: IProxySettings) {
            instance = PicassoInstance(context.applicationContext, proxySettings)
        }


        fun with(): Picasso {
            return instance!!.getSingleton()
        }

        fun getCoversPath(context: Context): File {
            val cache = File(context.cacheDir, "covers-cache")
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return cache
        }

        @Throws(IOException::class)
        fun clear_cache() {
            instance?.getCache_data()
            instance?.cache_data?.evictAll()
        }

        // from picasso sources
        private fun calculateDiskCacheSize(dir: File): Long {
            var size = 5242880L
            try {
                val statFs = StatFs(dir.absolutePath)
                val blockCount = statFs.blockCountLong
                val blockSize = statFs.blockSizeLong
                val available = blockCount * blockSize
                size = available / 50L
            } catch (ignored: IllegalArgumentException) {
            }
            return size.coerceAtMost(52428800L).coerceAtLeast(5242880L)
        }
    }

    init {
        proxySettings.observeActive()
            .subscribe { onProxyChanged() }
    }
}
