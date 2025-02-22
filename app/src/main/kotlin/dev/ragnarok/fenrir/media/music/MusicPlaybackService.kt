package dev.ragnarok.fenrir.media.music

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.audiofx.AudioEffect
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultRenderersFactory.*
import com.google.android.exoplayer2.Player.PlayWhenReadyChangeReason
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso.LoadedFrom
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.media.exo.ExoUtil
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.DownloadWorkUtils.GetLocalTrackLink
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.makeMediaItem
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.lang.ref.WeakReference

class MusicPlaybackService : Service() {
    private val SHUTDOWN = "dev.ragnarok.fenrir.media.music.shutdown"
    private val mBinder: IBinder = ServiceStub(this)
    private var mPlayer: MultiPlayer? = null
    private var mAlarmManager: AlarmManager? = null
    private var mShutdownIntent: PendingIntent? = null
    private var mShutdownScheduled = false
    var isPlaying = false
        private set

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private var errorsCount = 0
    private var onceCloseMiniPlayer = false
    private var mAnyActivityInForeground = false
    private var mMediaSession: MediaSessionCompat? = null
    private var mTransportController: MediaControllerCompat.TransportControls? = null
    private var mPlayPos = -1
    private var coverAudio: String? = null
    private var coverBitmap: Bitmap? = null
    private var albumTitle: String? = null
    private var mShuffleMode = SHUFFLE_NONE
    private var mRepeatMode = REPEAT_NONE
    private var mPlayList: ArrayList<Audio>? = null
    private var mPlayListOrig: ArrayList<Audio>? = null
    private var mNotificationHelper: NotificationHelper? = null
    private var mMediaMetadataCompat: MediaMetadataCompat? = null
    private var inForeground: Boolean = false
    override fun onBind(intent: Intent): IBinder {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Service bound, intent = $intent")
        cancelShutdown()
        return mBinder
    }

    fun goForeground(id: Int, notification: Notification?, mManager: NotificationManager?) {
        if (notification == null || mManager == null) {
            return
        }
        try {
            if (inForeground) {
                mManager.notify(id, notification)
                return
            }
            startForeground(id, notification)
            inForeground = true
        } catch (ignored: Exception) {
        }
    }

    fun outForeground(removeNotification: Boolean) {
        inForeground = false
        stopForeground(removeNotification)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Service unbound")
        if (isPlaying || mAnyActivityInForeground) {
            Logger.d(
                TAG,
                "onUnbind, mIsSupposedToBePlaying || mPausedByTransientLossOfFocus || isPreparing()"
            )
            return true
        }
        stopSelf()
        Logger.d(TAG, "onUnbind, stopSelf(mServiceStartId)")
        return true
    }

    override fun onRebind(intent: Intent) {
        cancelShutdown()
    }

    override fun onCreate() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Creating service")
        super.onCreate()
        mNotificationHelper = NotificationHelper(this)
        setUpRemoteControlClient()

        IDLE_DELAY = Settings.get().other().musicLifecycle

        mPlayer = MultiPlayer(this)
        val filter = IntentFilter()
        filter.addAction(SERVICECMD)
        filter.addAction(TOGGLEPAUSE_ACTION)
        filter.addAction(SWIPE_DISMISS_ACTION)
        filter.addAction(PAUSE_ACTION)
        filter.addAction(STOP_ACTION)
        filter.addAction(NEXT_ACTION)
        filter.addAction(PREVIOUS_ACTION)
        filter.addAction(REPEAT_ACTION)
        filter.addAction(SHUFFLE_ACTION)
        registerReceiver(mIntentReceiver, filter)

        // Initialize the delayed shutdown intent
        val shutdownIntent = Intent(this, MusicPlaybackService::class.java)
        shutdownIntent.action = SHUTDOWN
        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        mShutdownIntent =
            PendingIntent.getService(this, 0, shutdownIntent, Utils.makeMutablePendingIntent(0))

        // Listen for the idle state
        scheduleDelayedShutdown()
        notifyChange(META_CHANGED)
    }

    @Suppress("DEPRECATION")
    private fun setUpRemoteControlClient() {
        mMediaSession =
            MediaSessionCompat(application, resources.getString(R.string.app_name), null, null)
        val playbackStateCompat = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                position(),
                1.0f
            )
            .build()
        mMediaSession?.setPlaybackState(playbackStateCompat)
        mMediaSession?.setCallback(mMediaSessionCallback)
        mMediaSession?.isActive = true
        updateRemoteControlClient(META_CHANGED)
        mTransportController = mMediaSession?.controller?.transportControls
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                play()
            }

            override fun onPause() {
                super.onPause()
                pause()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                gotoNext(true)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                prev()
            }

            override fun onStop() {
                super.onStop()
                pause()
                Logger.d(javaClass.simpleName, "Stopping services. onStop()")
                stopSelf()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                seek(pos)
            }
        }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Destroying service")
        super.onDestroy()
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
        mAlarmManager?.cancel(mShutdownIntent)
        mPlayer?.release()
        mMediaSession?.release()
        mNotificationHelper?.killNotification()
        unregisterReceiver(mIntentReceiver)
    }

    /**
     * {@inheritDoc}
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Got new intent $intent, startId = $startId")
        if (intent != null) {
            val action = intent.action
            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mAnyActivityInForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false)
                updateNotification()
            }
            if (SHUTDOWN == action) {
                mShutdownScheduled = false
                releaseServiceUiAndStop()
                return START_NOT_STICKY
            }
            handleCommandIntent(intent)
            MediaButtonReceiver.handleIntent(mMediaSession, intent)
        }
        scheduleDelayedShutdown()
        return START_STICKY
    }

    @Suppress("DEPRECATION")
    private fun releaseServiceUiAndStop() {
        if (isPlaying) {
            return
        }
        if (Constants.IS_DEBUG) Logger.d(TAG, "Nothing is playing anymore, releasing notification")
        mNotificationHelper?.killNotification()
        if (!mAnyActivityInForeground) {
            stopSelf()
        }
    }

    private fun handleCommandIntent(intent: Intent) {
        val action = intent.action
        val command = if (SERVICECMD == action) intent.getStringExtra(CMDNAME) else null
        if (Constants.IS_DEBUG) Logger.d(
            TAG,
            "handleCommandIntent: action = $action, command = $command"
        )
        if (SWIPE_DISMISS_ACTION == action) {
            stopSelf()
        }
        if (CMDNEXT == command || NEXT_ACTION == action) {
            mTransportController?.skipToNext()
        }
        if (CMDPREVIOUS == command || PREVIOUS_ACTION == action) {
            mTransportController?.skipToPrevious()
        }
        if (CMDTOGGLEPAUSE == command || TOGGLEPAUSE_ACTION == action) {
            if (isPlaying) {
                mTransportController?.pause()
            } else {
                mTransportController?.play()
            }
        }
        if (CMDPAUSE == command || PAUSE_ACTION == action) {
            mTransportController?.pause()
        }
        if (CMDPLAY == command) {
            play()
        }
        if (CMDSTOP == command || STOP_ACTION == action) {
            mTransportController?.pause()
            seek(0)
            releaseServiceUiAndStop()
        }
        if (REPEAT_ACTION == action) {
            cycleRepeat()
        }
        if (SHUFFLE_ACTION == action) {
            cycleShuffle()
        }
        if (CMDPLAYLIST == action) {
            val apiAudios: ArrayList<Audio>? = intent.getParcelableArrayListExtra(Extra.AUDIOS)
            val position = intent.getIntExtra(Extra.POSITION, 0)
            val forceShuffle = intent.getIntExtra(Extra.SHUFFLE_MODE, SHUFFLE_NONE)
            shuffleMode = forceShuffle
            if (apiAudios != null)
                open(apiAudios, position)
        }
    }

    /**
     * Updates the notification, considering the current play and activity state
     */
    private fun updateNotification() {
        mNotificationHelper?.buildNotification(
            this,
            artistName,
            trackName,
            isPlaying,
            Utils.firstNonNull(
                coverBitmap,
                BitmapFactory.decodeResource(resources, R.drawable.generic_audio_nowplaying_service)
            ),
            mMediaSession?.sessionToken
        )
    }

    private fun scheduleDelayedShutdown() {
        if (Constants.IS_DEBUG) Log.v(TAG, "Scheduling shutdown in $IDLE_DELAY ms")
        mAlarmManager?.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY,
            mShutdownIntent
        )
        mShutdownScheduled = true
    }

    private fun cancelShutdown() {
        if (Constants.IS_DEBUG) Logger.d(
            TAG,
            "Cancelling delayed shutdown, scheduled = $mShutdownScheduled"
        )
        if (mShutdownScheduled) {
            mAlarmManager?.cancel(mShutdownIntent)
            mShutdownScheduled = false
        }
    }

    /**
     * Stops playback
     *
     * @param goToIdle True to go to the idle state, false otherwise
     */
    private fun stop(goToIdle: Boolean) {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Stopping playback, goToIdle = $goToIdle")
        if (mPlayer?.isInitialized == true) {
            mPlayer?.stop()
        }
        if (goToIdle) {
            scheduleDelayedShutdown()
            isPlaying = false
        } else {
            outForeground(false) //надо подумать
        }
    }

    private val isInitialized: Boolean
        get() = mPlayer?.isInitialized == true

    private val isPreparing: Boolean
        get() = mPlayer?.isPreparing == true

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    private fun playCurrentTrack(UpdateMeta: Boolean) {
        synchronized(this) {
            Logger.d(TAG, "playCurrentTrack, mPlayListLen: " + Utils.safeCountOf(mPlayList))
            if (mPlayList.isNullOrEmpty()) {
                return
            }
            stop(false)
            mPlayList?.let {
                if (it.size - 1 < mPlayPos) {
                    mPlayPos = 0
                }
            }
            val current = mPlayList?.get(mPlayPos)
            openFile(current, UpdateMeta)
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     * otherwise.
     * @return The next position to play.
     */
    private fun getNextPosition(force: Boolean): Int {
        if (!force && mRepeatMode == REPEAT_CURRENT) {
            return mPlayPos.coerceAtLeast(0)
        }
        return if (mPlayPos >= Utils.safeCountOf(mPlayList) - 1) {
            if (mRepeatMode == REPEAT_NONE && !force) {
                return -1
            }
            if (mRepeatMode == REPEAT_ALL || force) {
                0
            } else -1
        } else {
            mPlayPos + 1
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    private fun notifyChange(what: String) {
        if (Constants.IS_DEBUG) Logger.d(TAG, "notifyChange: what = $what")
        updateRemoteControlClient(what)
        if (what == POSITION_CHANGED) {
            return
        }
        sendBroadcast(Intent(what))
        if (what == PLAYSTATE_CHANGED) {
            mNotificationHelper?.updatePlayState(isPlaying)
        }
    }

    /**
     * Updates the lockscreen controls.
     *
     * @param what The broadcast
     */
    private fun updateRemoteControlClient(what: String) {
        when (what) {
            PLAYSTATE_CHANGED, POSITION_CHANGED -> {
                val playState =
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                val pmc = PlaybackStateCompat.Builder()
                    .setState(playState, position(), 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_STOP
                    )
                    .build()
                mMediaSession?.setPlaybackState(pmc)
            }
            META_CHANGED -> fetchCoverAndUpdateMetadata()
        }
    }

    private fun fetchCoverAndUpdateMetadata() {
        updateMetadata()
        if (coverBitmap != null || albumCover.isNullOrEmpty()) {
            return
        }
        PicassoInstance.with()
            .load(albumCover)
            .into(object : BitmapTarget {
                override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                    coverBitmap = bitmap
                    updateMetadata()
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }

                override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                }

            })
    }

    private fun updateMetadata() {
        updateNotification()
        mMediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, trackName)
            //.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, Utils.firstNonNull(CoverBitmap, BitmapFactory.decodeResource(getResources(), R.drawable.generic_audio_nowplaying_service)))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration())
            .build()
        mMediaSession?.setMetadata(mMediaMetadataCompat)
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param audio The path of the file to open
     */
    fun openFile(audio: Audio?, UpdateMeta: Boolean) {
        synchronized(this) {
            if (audio == null) {
                stop(true)
                return
            }

            if (Settings.get().other().isForce_cache && TrackIsDownloaded(audio) == 1)
                audio.setUrl(GetLocalTrackLink(audio))
            if (UpdateMeta) {
                errorsCount = 0
                coverAudio = null
                albumTitle = null
                coverBitmap = null
                onceCloseMiniPlayer = false
            }
            mPlayer?.setDataSource(audio)
            if (audio.thumb_image_big != null && UpdateMeta) {
                coverAudio = audio.thumb_image_big
                albumTitle = audio.album_title
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            } else {
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            }
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val audioSessionId: Int
        get() {
            synchronized(this) { return mPlayer?.audioSessionId ?: -1 }
        }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val bufferPercent: Int
        get() {
            synchronized(this) { return mPlayer?.bufferPercent ?: 0 }
        }

    val bufferPos: Long
        get() {
            synchronized(this) { return mPlayer?.bufferPos ?: 0 }
        }

    var shuffleMode: Int
        get() = mShuffleMode
        set(shufflemode) {
            synchronized(this) {
                if (mShuffleMode == shufflemode && Utils.safeCountOf(mPlayList) > 0) {
                    return
                }
                mShuffleMode = shufflemode
                notifyChange(SHUFFLEMODE_CHANGED)
                if (mShuffleMode == SHUFFLE) {
                    mPlayList?.shuffle()
                    skip(0, true)
                } else {
                    val ps = mPlayListOrig?.indexOf(mPlayList?.get(mPlayPos))
                    ps?.let {
                        mPlayPos = if (it < 0) {
                            mPlayList?.get(mPlayPos)?.let { it1 -> mPlayListOrig?.add(0, it1) }
                            0
                        } else {
                            it
                        }
                    }
                    mPlayList?.clear()
                    mPlayListOrig?.let { mPlayList?.addAll(it) }
                    notifyChange(META_CHANGED)
                }
            }
        }

    var repeatMode: Int
        get() = mRepeatMode
        set(repeatmode) {
            synchronized(this) {
                mRepeatMode = repeatmode
                notifyChange(REPEATMODE_CHANGED)
            }
        }

    val queuePosition: Int
        get() {
            synchronized(this) { return mPlayPos }
        }

    val path: String?
        get() {
            synchronized(this) {
                val apiAudio = currentTrack ?: return null
                return apiAudio.url
            }
        }

    val albumName: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else albumTitle
            }
        }

    /**
     * Returns the album cover
     *
     * @return url
     */

    private val albumCover: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else coverAudio
            }
        }

    val trackName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.title
            }
        }

    val artistName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.artist
            }
        }

    val currentTrack: Audio?
        get() {
            synchronized(this) {
                mPlayList?.let {
                    if (mPlayPos >= 0 && it.size > mPlayPos) {
                        return it[mPlayPos]
                    }
                }
            }
            return null
        }

    val currentTrackPos: Int
        get() {
            synchronized(this) {
                mPlayList?.let {
                    if (mPlayPos >= 0 && it.size > mPlayPos) {
                        return mPlayPos
                    }
                }
            }
            return -1
        }

    fun seek(position: Long): Long {
        var positiontemp = position
        mPlayer?.let {
            if (it.isInitialized) {
                if (positiontemp < 0) {
                    positiontemp = 0
                } else if (positiontemp > it.duration()) {
                    positiontemp = it.duration()
                }
                val result = it.seek(positiontemp)
                notifyChange(POSITION_CHANGED)
                return result
            }
        }
        return -1
    }

    fun position(): Long {
        return if (mPlayer?.isInitialized == true) {
            mPlayer?.position() ?: -1
        } else -1
    }

    fun duration(): Long {
        return if (mPlayer?.isInitialized == true) {
            mPlayer?.duration() ?: -1
        } else -1
    }

    val queue: List<Audio>
        get() {
            synchronized(this) {
                val len = Utils.safeCountOf(mPlayList)
                val list: MutableList<Audio> = ArrayList(len)
                for (i in 0 until len) {
                    mPlayList?.let {
                        list.add(i, it[i])
                    }
                }
                return list
            }
        }

    private val currentTrackNotSyncPos: Int
        get() {
            mPlayList?.let {
                if (mPlayPos >= 0 && it.size > mPlayPos) {
                    return mPlayPos
                }
            }
            return -1
        }

    fun canPlayAfterCurrent(audio: Audio): Boolean {
        synchronized(this) {
            val current = currentTrackNotSyncPos
            if (mPlayList.isNullOrEmpty() || current == -1 || mPlayList?.get(current) == audio) {
                return false
            }
            return true
        }
    }

    fun playAfterCurrent(audio: Audio) {
        synchronized(this) {
            val current = currentTrackNotSyncPos
            if (mPlayList.isNullOrEmpty() || current == -1 || mPlayList?.get(current) == audio) {
                return
            }
            mPlayList?.insertAfter(current, audio)
            notifyChange(QUEUE_CHANGED)
        }
    }

    /**
     * Opens a list for playback
     *
     * @param list     The list of tracks to open
     * @param position The position to start playback at
     */
    fun open(list: List<Audio>, position: Int) {
        synchronized(this) {
            val oldAudio = currentTrack
            mPlayList = ArrayList(list)
            if (mShuffleMode == SHUFFLE)
                mPlayList?.shuffle()
            mPlayListOrig = ArrayList(list)
            notifyChange(QUEUE_CHANGED)
            mPlayPos = if (position >= 0) {
                position
            } else {
                0
            }
            playCurrentTrack(true)
            if (oldAudio !== currentTrack) {
                notifyChange(META_CHANGED)
            }
        }
    }

    fun play() {
        mPlayer?.let {
            if (it.isInitialized) {
                val duration = it.duration()
                if (mRepeatMode != REPEAT_CURRENT && duration > 2000 && it.position() >= duration - 2000) {
                    gotoNext(false)
                }
                it.start()
                if (!isPlaying) {
                    isPlaying = true
                    notifyChange(PLAYSTATE_CHANGED)
                }
                cancelShutdown()
                fetchCoverAndUpdateMetadata()
            }
        }
    }

    /**
     * Temporarily pauses playback.
     */
    fun pause() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Pausing playback")
        synchronized(this) {
            if (isPlaying) {
                mPlayer?.pause()
                scheduleDelayedShutdown()
                isPlaying = false
                notifyChange(PLAYSTATE_CHANGED)
            }
        }
    }

    private fun pauseNonSync() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Pausing playback")
        if (isPlaying) {
            mPlayer?.pause()
            scheduleDelayedShutdown()
            isPlaying = false
            notifyChange(PLAYSTATE_CHANGED)
        }
    }

    /**
     * Changes from the current track to the next track
     */
    fun gotoNext(force: Boolean): Boolean {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to next track")
        synchronized(this) {
            if (Utils.safeCountOf(mPlayList) <= 0) {
                if (Constants.IS_DEBUG) Logger.d(TAG, "No play queue")
                scheduleDelayedShutdown()
                return true
            }
            val pos = getNextPosition(force)
            if (Constants.IS_DEBUG) Logger.d(TAG, pos.toString())
            if (pos < 0) {
                seek(0)
                pauseNonSync()
                return false
            }
            mPlayPos = pos
            stop(false)
            mPlayPos = pos
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
        return true
    }

    fun skip(pos: Int, force: Boolean) {
        if (!force && pos == currentTrackPos)
            return
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to next track")
        synchronized(this) {
            if (Utils.safeCountOf(mPlayList) <= 0) {
                if (Constants.IS_DEBUG) Logger.d(TAG, "No play queue")
                scheduleDelayedShutdown()
                return
            }
            if (Constants.IS_DEBUG) Logger.d(TAG, pos.toString())
            if (pos < 0) {
                seek(0)
                pauseNonSync()
                return
            }
            mPlayPos = pos
            stop(false)
            mPlayPos = pos
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    /**
     * Changes from the current track to the previous played track
     */
    fun prev() {
        if (Constants.IS_DEBUG) Logger.d(TAG, "Going to previous track")
        synchronized(this) {
            if (mPlayPos > 0) {
                mPlayPos--
            } else {
                mPlayPos = Utils.safeCountOf(mPlayList) - 1
            }
            stop(false)
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    private fun cycleRepeat() {
        when (mRepeatMode) {
            REPEAT_NONE -> repeatMode = REPEAT_ALL
            REPEAT_ALL -> {
                repeatMode = REPEAT_CURRENT
                if (mShuffleMode != SHUFFLE_NONE) {
                    shuffleMode = SHUFFLE_NONE
                }
            }
            else -> repeatMode = REPEAT_NONE
        }
    }

    private fun cycleShuffle() {
        when (mShuffleMode) {
            SHUFFLE -> shuffleMode = SHUFFLE_NONE
            SHUFFLE_NONE -> {
                shuffleMode = SHUFFLE
                if (mRepeatMode == REPEAT_CURRENT) {
                    repeatMode = REPEAT_ALL
                }
            }
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    fun refresh() {
        notifyChange(REFRESH)
    }

    private val mIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleCommandIntent(intent)
        }
    }

    private class MultiPlayer(service: MusicPlaybackService) {
        val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        var mCurrentMediaPlayer: ExoPlayer = ExoPlayer.Builder(
            service, DefaultRenderersFactory(service)
                .setExtensionRendererMode(
                    when (Settings.get().other().fFmpegPlugin) {
                        0 -> EXTENSION_RENDERER_MODE_OFF
                        1 -> EXTENSION_RENDERER_MODE_ON
                        2 -> EXTENSION_RENDERER_MODE_PREFER
                        else -> EXTENSION_RENDERER_MODE_OFF
                    }
                )
        ).build()
        var isInitialized = false
        var isPreparing = false
        val factory = Utils.getExoPlayerFactory(
            Constants.USER_AGENT(AccountType.BY_TYPE),
            Includes.proxySettings.activeProxy
        )
        val factoryLocal =
            DefaultDataSource.Factory(service)
        val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
        val compositeDisposable = CompositeDisposable()

        /**
         * @param remoteUrl The path of the file, or the http/rtsp URL of the stream
         * you want to play
         * return True if the `player` has been prepared and is
         * ready to play, false otherwise
         */
        fun setDataSource(remoteUrl: String?) {
            isPreparing = true
            var res: String? = remoteUrl
            if ("https://vk.com/mp3/audio_api_unavailable.mp3" == res) {
                res = null
            }
            val url = Utils.firstNonEmptyString(
                res,
                //RawResourceDataSource.buildRawResourceUri(R.raw.audio_error).toString()
                "file:///android_asset/audio_error.ogg"
            )
            val mediaSource: MediaSource =
                if (url?.contains("file://") == true || url?.contains("content://") == true || url?.contains(
                        RawResourceDataSource.RAW_RESOURCE_SCHEME
                    ) == true
                ) {
                    ProgressiveMediaSource.Factory(factoryLocal)
                        .createMediaSource(makeMediaItem(url))
                } else {
                    if (url?.contains("index.m3u8") == true) HlsMediaSource.Factory(factory)
                        .createMediaSource(makeMediaItem(url)) else ProgressiveMediaSource.Factory(
                        factory
                    ).createMediaSource(makeMediaItem(url))
                }
            mCurrentMediaPlayer.setMediaSource(mediaSource)
            mCurrentMediaPlayer.prepare()
            mCurrentMediaPlayer.setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), true
            )
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mService.get()?.packageName)
            mService.get()?.sendBroadcast(intent)
            mService.get()?.notifyChange(PLAYSTATE_CHANGED)
        }

        fun setDataSource(audio: Audio) {
            val accountId = Settings.get().accounts().current
            if (!Utils.isHiddenAccount(accountId) || !audio.isLocalServer || !audio.isLocal) {
                compositeDisposable.add(
                    audioInteractor.trackEvents(
                        accountId,
                        audio
                    )
                        .fromIOToMain()
                        .subscribe(RxUtils.dummy(), RxUtils.ignore())
                )
            }
            if (audio.url.isNullOrEmpty() || "https://vk.com/mp3/audio_api_unavailable.mp3" == audio.url) {
                compositeDisposable.add(audioInteractor.getById(
                    accountId,
                    listOf(audio)
                )
                    .fromIOToMain()
                    .subscribe({ remoteUrl -> setDataSource(remoteUrl[0].url) }) {
                        setDataSource(
                            audio.url
                        )
                    })
            } else {
                setDataSource(audio.url)
            }
        }

        fun start() {
            ExoUtil.startPlayer(mCurrentMediaPlayer)
        }

        fun stop() {
            this.isInitialized = false
            isPreparing = false
            mCurrentMediaPlayer.stop()
            mCurrentMediaPlayer.clearMediaItems()
        }

        fun release() {
            stop()
            mCurrentMediaPlayer.release()
            compositeDisposable.dispose()
        }

        fun pause() {
            ExoUtil.pausePlayer(mCurrentMediaPlayer)
        }

        fun duration(): Long {
            return mCurrentMediaPlayer.duration
        }

        fun position(): Long {
            return mCurrentMediaPlayer.currentPosition
        }

        fun seek(whereto: Long): Long {
            mCurrentMediaPlayer.seekTo(whereto)
            return whereto
        }

        val audioSessionId: Int
            get() = mCurrentMediaPlayer.audioSessionId

        val bufferPercent: Int
            get() = mCurrentMediaPlayer.bufferedPercentage

        val bufferPos: Long
            get() = mCurrentMediaPlayer.bufferedPosition

        /**
         * Constructor of `MultiPlayer`
         */
        init {
            mCurrentMediaPlayer.setHandleAudioBecomingNoisy(true)
            mCurrentMediaPlayer.setWakeMode(C.WAKE_MODE_NETWORK)

            mCurrentMediaPlayer.repeatMode = Player.REPEAT_MODE_OFF

            mCurrentMediaPlayer.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(@Player.State state: Int) {
                    when (state) {
                        Player.STATE_READY -> if (isPreparing) {
                            isPreparing = false
                            isInitialized = true
                            mService.get()?.notifyChange(PREPARED)
                            mService.get()?.play()
                        }
                        Player.STATE_ENDED -> if (!isPreparing && isInitialized) {
                            isInitialized = mService.get()?.gotoNext(false) == false
                        }
                        else -> {
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    @PlayWhenReadyChangeReason reason: Int
                ) {
                    if (mService.get()?.isPlaying != playWhenReady) {
                        mService.get()?.isPlaying = playWhenReady
                        mService.get()?.notifyChange(PLAYSTATE_CHANGED)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mService.get()?.let {
                        it.errorsCount++
                        if (it.errorsCount > 10) {
                            it.errorsCount = 0
                            it.stopSelf()
                        } else {
                            val playbackPos = mCurrentMediaPlayer.currentPosition
                            it.playCurrentTrack(false)
                            mCurrentMediaPlayer.seekTo(playbackPos)
                            it.notifyChange(META_CHANGED)
                        }
                    }
                }
            })
        }
    }

    private class ServiceStub(service: MusicPlaybackService) : IAudioPlayerService.Stub() {
        private val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        override fun openFile(audio: Audio) {
            mService.get()?.openFile(audio, true)
        }

        override fun open(list: List<Audio>, position: Int) {
            mService.get()?.open(list, position)
        }

        override fun stop() {
            mService.get()?.pause()
            mService.get()?.releaseServiceUiAndStop()
        }

        override fun pause() {
            mService.get()?.pause()
        }

        override fun play() {
            mService.get()?.play()
        }

        override fun prev() {
            mService.get()?.prev()
        }

        override fun next() {
            mService.get()?.gotoNext(true)
        }

        override fun setShuffleMode(shufflemode: Int) {
            mService.get()?.shuffleMode = shufflemode
        }

        override fun setRepeatMode(repeatmode: Int) {
            mService.get()?.repeatMode = repeatmode
        }

        override fun closeMiniPlayer() {
            mService.get()?.onceCloseMiniPlayer = true
        }

        override fun refresh() {
            mService.get()?.refresh()
        }

        override fun isPlaying(): Boolean {
            return mService.get()?.isPlaying == true
        }

        override fun isPreparing(): Boolean {
            return mService.get()?.isPreparing == true
        }

        override fun isInitialized(): Boolean {
            return mService.get()?.isInitialized == true
        }

        override fun canPlayAfterCurrent(audio: Audio): Boolean {
            return mService.get()?.canPlayAfterCurrent(audio) == true
        }

        override fun playAfterCurrent(audio: Audio) {
            mService.get()?.playAfterCurrent(audio)
        }

        override fun getQueue(): List<Audio>? {
            return mService.get()?.queue
        }

        override fun duration(): Long {
            return mService.get()?.duration() ?: -1
        }

        override fun position(): Long {
            return mService.get()?.position() ?: -1
        }

        override fun getMiniplayerVisibility(): Boolean {
            return mService.get()?.onceCloseMiniPlayer != true && mService.get()?.currentTrack != null
        }

        override fun seek(position: Long): Long {
            return mService.get()?.seek(position) ?: -1
        }

        override fun skip(position: Int) {
            mService.get()?.skip(position, false)
        }

        override fun getCurrentAudio(): Audio? {
            return mService.get()?.currentTrack
        }

        override fun getCurrentAudioPos(): Int {
            return mService.get()?.currentTrackPos ?: -1
        }

        override fun getArtistName(): String? {
            return mService.get()?.artistName
        }

        override fun getTrackName(): String? {
            return mService.get()?.trackName
        }

        override fun getAlbumName(): String? {
            return mService.get()?.albumName
        }

        override fun getAlbumCover(): String? {
            return mService.get()?.albumCover
        }

        override fun getPath(): String? {
            return mService.get()?.path
        }

        override fun getQueuePosition(): Int {
            return mService.get()?.queuePosition ?: -1
        }

        override fun getShuffleMode(): Int {
            return mService.get()?.shuffleMode ?: SHUFFLE_NONE
        }

        override fun getRepeatMode(): Int {
            return mService.get()?.repeatMode ?: REPEAT_NONE
        }

        override fun getAudioSessionId(): Int {
            return mService.get()?.audioSessionId ?: -1
        }

        override fun getBufferPercent(): Int {
            return mService.get()?.bufferPercent ?: 0
        }

        override fun getBufferPosition(): Long {
            return mService.get()?.bufferPos ?: 0
        }

    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        const val PLAYSTATE_CHANGED = "dev.ragnarok.fenrir.media.music.playstatechanged"
        const val POSITION_CHANGED = "dev.ragnarok.fenrir.media.music.positionchanged"
        const val META_CHANGED = "dev.ragnarok.fenrir.media.music.metachanged"
        const val PREPARED = "dev.ragnarok.fenrir.media.music.prepared"
        const val REPEATMODE_CHANGED = "dev.ragnarok.fenrir.media.music.repeatmodechanged"
        const val SHUFFLEMODE_CHANGED = "dev.ragnarok.fenrir.media.music.shufflemodechanged"
        const val QUEUE_CHANGED = "dev.ragnarok.fenrir.media.music.queuechanged"
        const val SERVICECMD = "dev.ragnarok.fenrir.media.music.musicservicecommand"
        const val TOGGLEPAUSE_ACTION = "dev.ragnarok.fenrir.media.music.togglepause"
        const val PAUSE_ACTION = "dev.ragnarok.fenrir.media.music.pause"
        const val STOP_ACTION = "dev.ragnarok.fenrir.media.music.stop"
        const val SWIPE_DISMISS_ACTION = "dev.ragnarok.fenrir.media.music.swipe_dismiss"
        const val PREVIOUS_ACTION = "dev.ragnarok.fenrir.media.music.previous"
        const val NEXT_ACTION = "dev.ragnarok.fenrir.media.music.next"
        const val REPEAT_ACTION = "dev.ragnarok.fenrir.media.music.repeat"
        const val SHUFFLE_ACTION = "dev.ragnarok.fenrir.media.music.shuffle"

        /**
         * Called to update the service about the foreground state of Apollo's activities
         */
        const val FOREGROUND_STATE_CHANGED = "dev.ragnarok.fenrir.media.music.fgstatechanged"
        const val NOW_IN_FOREGROUND = "nowinforeground"
        const val REFRESH = "dev.ragnarok.fenrir.media.music.refresh"

        /**
         * Called to update the remote control client
         */
        const val CMDNAME = "command"
        const val CMDTOGGLEPAUSE = "togglepause"
        const val CMDSTOP = "stop"
        const val CMDPAUSE = "pause"
        const val CMDPLAY = "play"
        const val CMDPREVIOUS = "previous"
        const val CMDNEXT = "next"
        const val CMDPLAYLIST = "playlist"
        const val SHUFFLE_NONE = 0
        const val SHUFFLE = 1
        const val REPEAT_NONE = 0
        const val REPEAT_CURRENT = 1
        const val REPEAT_ALL = 2
        private var IDLE_DELAY = Constants.AUDIO_PLAYER_SERVICE_IDLE
        private const val MAX_QUEUE_SIZE = 200

        fun startForPlayList(
            context: Context,
            audios: ArrayList<Audio>,
            position: Int,
            forceShuffle: Boolean
        ) {
            if (audios.isEmpty()) {
                return
            }
            Logger.d(TAG, "startForPlayList, count: " + audios.size + ", position: " + position)
            val target: ArrayList<Audio>
            var targetPosition: Int
            if (audios.size <= MAX_QUEUE_SIZE) {
                target = audios
                targetPosition = position
            } else {
                target = ArrayList(MAX_QUEUE_SIZE)
                val half = MAX_QUEUE_SIZE / 2
                var startAt = position - half
                if (startAt < 0) {
                    startAt = 0
                }
                targetPosition = position - startAt
                var i = startAt
                while (target.size < MAX_QUEUE_SIZE) {
                    if (i > audios.size - 1) {
                        break
                    }
                    target.add(audios[i])
                    i++
                }
                if (target.size < MAX_QUEUE_SIZE) {
                    var it = startAt - 1
                    while (target.size < MAX_QUEUE_SIZE) {
                        target.add(0, audios[it])
                        targetPosition++
                        it--
                    }
                }
            }
            val intent = Intent(context, MusicPlaybackService::class.java)
            intent.action = CMDPLAYLIST
            intent.putParcelableArrayListExtra(Extra.AUDIOS, target)
            intent.putExtra(Extra.POSITION, targetPosition)
            intent.putExtra(Extra.SHUFFLE_MODE, if (forceShuffle) SHUFFLE else SHUFFLE_NONE)
            context.startService(intent)
        }
    }
}
