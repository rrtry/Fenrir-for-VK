package dev.ragnarok.fenrir.media.video

import android.content.Context
import android.view.SurfaceHolder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.video.VideoSize
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.media.exo.ExoUtil.pausePlayer
import dev.ragnarok.fenrir.media.exo.ExoUtil.startPlayer
import dev.ragnarok.fenrir.media.video.IVideoPlayer.IUpdatePlayListener
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.util.Utils.getExoPlayerFactory
import dev.ragnarok.fenrir.util.Utils.makeMediaItem
import java.lang.ref.WeakReference

class ExoVideoPlayer(
    context: Context,
    url: String,
    config: ProxyConfig?,
    @InternalVideoSize size: Int,
    playListener: IUpdatePlayListener
) : IVideoPlayer {
    private val player: ExoPlayer?
    private val onVideoSizeChangedListener = OnVideoSizeChangedListener(this)
    private val videoSizeChangeListeners: MutableList<IVideoPlayer.IVideoSizeChangeListener> =
        ArrayList(1)
    private var source: MediaSource
    private var supposedToBePlaying = false
    private var prepareCalled = false
    private var playbackSpeed = false
    override fun updateSource(
        context: Context,
        url: String?,
        config: ProxyConfig?,
        @InternalVideoSize size: Int
    ) {
        source = createMediaSource(
            context,
            url,
            config,
            size == InternalVideoSize.SIZE_HLS || size == InternalVideoSize.SIZE_LIVE
        )
        player?.setMediaSource(source)
        player?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        player?.prepare()
        startPlayer(player)
    }

    private fun createPlayer(context: Context): ExoPlayer {
        val ret = ExoPlayer.Builder(context, DefaultRenderersFactory(context)).build()
        ret.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).setUsage(
                C.USAGE_MEDIA
            ).build(), true
        )
        return ret
    }

    override fun play() {
        if (supposedToBePlaying) {
            return
        }
        supposedToBePlaying = true
        if (!prepareCalled) {
            player?.setMediaSource(source)
            player?.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
            player?.prepare()
            prepareCalled = true
        }
        startPlayer(player)
    }

    override fun pause() {
        if (!supposedToBePlaying) {
            return
        }
        supposedToBePlaying = false
        pausePlayer(player)
    }

    override val isPlaybackSpeed: Boolean
        get() = playbackSpeed

    override fun togglePlaybackSpeed() {
        if (player != null) {
            playbackSpeed = !playbackSpeed
            player.setPlaybackSpeed(if (playbackSpeed) 2f else 1f)
        }
    }

    override fun release() {
        if (player != null) {
            try {
                player.removeListener(onVideoSizeChangedListener)
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val duration: Long
        get() = player?.duration ?: 1

    override val currentPosition: Long
        get() = player?.currentPosition ?: 0

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    override val isPlaying: Boolean
        get() = supposedToBePlaying

    override val bufferPercentage: Int
        get() = player?.bufferedPercentage ?: 0

    override val bufferPosition: Long
        get() = player?.bufferedPosition ?: 0

    override fun setSurfaceHolder(holder: SurfaceHolder?) {
        player?.setVideoSurfaceHolder(holder)
    }

    private fun onVideoSizeChanged(w: Int, h: Int) {
        for (listener in videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, dev.ragnarok.fenrir.model.VideoSize(w, h))
        }
    }

    override fun addVideoSizeChangeListener(listener: IVideoPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.add(listener)
    }

    override fun removeVideoSizeChangeListener(listener: IVideoPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.remove(listener)
    }

    private class OnVideoSizeChangedListener(player: ExoVideoPlayer) :
        Player.Listener {
        val ref: WeakReference<ExoVideoPlayer> = WeakReference(player)
        override fun onVideoSizeChanged(size: VideoSize) {
            val player = ref.get()
            player?.onVideoSizeChanged(size.width, size.height)
        }

        override fun onRenderedFirstFrame() {}

    }

    companion object {
        private fun createMediaSource(
            context: Context,
            url: String?,
            proxyConfig: ProxyConfig?,
            isHLS: Boolean
        ): MediaSource {
            val userAgent = USER_AGENT(AccountType.BY_TYPE)
            return if (!isHLS) {
                if (url?.contains("file://") == true || url?.contains("content://") == true) {
                    ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
                        .createMediaSource(makeMediaItem(url))
                } else ProgressiveMediaSource.Factory(
                    getExoPlayerFactory(
                        userAgent,
                        proxyConfig
                    )
                ).createMediaSource(makeMediaItem(url))
            } else {
                HlsMediaSource.Factory(
                    getExoPlayerFactory(
                        userAgent,
                        proxyConfig
                    )
                ).createMediaSource(makeMediaItem(url))
            }
        }
    }

    init {
        player = createPlayer(context)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player.seekTo(0)
                    pause()
                    playListener.onPlayChanged(true)
                }
            }
        })
        player.addListener(onVideoSizeChangedListener)
        source = createMediaSource(
            context,
            url,
            config,
            size == InternalVideoSize.SIZE_HLS || size == InternalVideoSize.SIZE_LIVE
        )
    }
}