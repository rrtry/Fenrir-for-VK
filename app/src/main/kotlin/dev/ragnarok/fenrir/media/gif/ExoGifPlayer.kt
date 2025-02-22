package dev.ragnarok.fenrir.media.gif

import android.view.SurfaceHolder
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.media.gif.IGifPlayer.IStatus
import dev.ragnarok.fenrir.media.gif.IGifPlayer.IStatusChangeListener
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.model.VideoSize
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Utils.getExoPlayerFactory
import dev.ragnarok.fenrir.util.Utils.makeMediaItem

class ExoGifPlayer(
    private val url: String,
    private val proxyConfig: ProxyConfig?,
    private val isRepeat: Boolean
) : IGifPlayer {
    private val videoSizeChangeListeners: MutableList<IGifPlayer.IVideoSizeChangeListener> =
        ArrayList(1)
    private val statusChangeListeners: MutableList<IStatusChangeListener> = ArrayList(1)
    override var videoSize: VideoSize? = null
        private set
    private val videoListener: Player.Listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: com.google.android.exoplayer2.video.VideoSize) {
            this@ExoGifPlayer.videoSize = VideoSize(videoSize.width, videoSize.height)
            this@ExoGifPlayer.onVideoSizeChanged()
        }

        override fun onRenderedFirstFrame() {}
        override fun onPlaybackStateChanged(state: @Player.State Int) {
            d("FenrirExo", "onPlaybackStateChanged, state: $state")
            onInternalPlayerStateChanged(state)
        }
    }
    override var playerStatus: Int = IStatus.INIT
    private var internalPlayer: ExoPlayer? = null
    private var supposedToBePlaying = false
    override fun play() {
        if (supposedToBePlaying) return
        supposedToBePlaying = true
        when (playerStatus) {
            IStatus.PREPARED -> startPlayer(internalPlayer)
            IStatus.INIT -> preparePlayer()
            IStatus.PREPARING -> {}
        }
    }

    private fun preparePlayer() {
        setStatus(IStatus.PREPARING)
        internalPlayer = ExoPlayer.Builder(instance).build()
        val userAgent = USER_AGENT(AccountType.BY_TYPE)

        // This is the MediaSource representing the media to be played:
        // FOR SD CARD SOURCE:
        // MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
        // FOR LIVESTREAM LINK:
        val mediaSource: MediaSource =
            ProgressiveMediaSource.Factory(getExoPlayerFactory(userAgent, proxyConfig))
                .createMediaSource(
                    makeMediaItem(
                        url
                    )
                )
        internalPlayer?.repeatMode =
            if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        internalPlayer?.addListener(videoListener)
        internalPlayer?.playWhenReady = true
        internalPlayer?.setMediaSource(mediaSource)
        internalPlayer?.prepare()
    }

    private fun onInternalPlayerStateChanged(state: @Player.State Int) {
        if (state == Player.STATE_READY) {
            setStatus(IStatus.PREPARED)
        } else if (state == Player.STATE_ENDED && !isRepeat) {
            setStatus(IStatus.ENDED)
        }
    }

    private fun onVideoSizeChanged() {
        for (listener in videoSizeChangeListeners) {
            listener.onVideoSizeChanged(this, videoSize ?: VideoSize(1, 1))
        }
    }

    override fun pause() {
        if (!supposedToBePlaying) return
        supposedToBePlaying = false
        if (internalPlayer != null) {
            try {
                internalPlayer?.let { pausePlayer(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        internalPlayer?.setVideoSurfaceHolder(holder)
    }

    override fun release() {
        if (internalPlayer != null) {
            try {
                internalPlayer?.removeListener(videoListener)
                internalPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setStatus(newStatus: Int) {
        val oldStatus = playerStatus
        if (playerStatus == newStatus) {
            return
        }
        playerStatus = newStatus
        for (listener in statusChangeListeners) {
            listener.onPlayerStatusChange(this, oldStatus, newStatus)
        }
    }

    override fun addVideoSizeChangeListener(listener: IGifPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.add(listener)
    }

    override fun addStatusChangeListener(listener: IStatusChangeListener) {
        statusChangeListeners.add(listener)
    }

    override fun removeVideoSizeChangeListener(listener: IGifPlayer.IVideoSizeChangeListener) {
        videoSizeChangeListeners.remove(listener)
    }

    override fun removeStatusChangeListener(listener: IStatusChangeListener) {
        statusChangeListeners.remove(listener)
    }

    companion object {
        private fun pausePlayer(internalPlayer: ExoPlayer) {
            internalPlayer.playWhenReady = false
            internalPlayer.playbackState
        }

        private fun startPlayer(internalPlayer: ExoPlayer?) {
            internalPlayer?.playWhenReady = true
            internalPlayer?.playbackState
        }
    }
}