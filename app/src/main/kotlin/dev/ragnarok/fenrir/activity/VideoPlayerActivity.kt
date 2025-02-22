package dev.ragnarok.fenrir.activity

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SwipebleActivity.Companion.applyIntent
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.media.video.ExoVideoPlayer
import dev.ragnarok.fenrir.media.video.IVideoPlayer
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoSize
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.ExpandableSurfaceView
import dev.ragnarok.fenrir.view.VideoControllerView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class VideoPlayerActivity : AppCompatActivity(), SurfaceHolder.Callback,
    VideoControllerView.MediaPlayerControl, IVideoPlayer.IVideoSizeChangeListener, AppStyleable {
    private val mCompositeDisposable = CompositeDisposable()
    private var mDecorView: View? = null
    private var mSpeed: ImageView? = null
    private var mControllerView: VideoControllerView? = null
    private var mSurfaceView: ExpandableSurfaceView? = null
    private var mPlayer: IVideoPlayer? = null
    private var video: Video? = null
    private var onStopCalled = false
    private var seekSave: Long = -1

    @InternalVideoSize
    private var size = 0
    private var doNotPause = false
    private val requestSwipeble = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { doNotPause = false }
    private var isLocal = false
    private var isLandscape = false
    private fun onOpen() {
        val intent = Intent(this, SwipebleActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_WALL
        intent.putExtra(Extra.OWNER_ID, (video ?: return).ownerId)
        doNotPause = true
        applyIntent(intent)
        requestSwipeble.launch(intent)
    }

    override fun onStop() {
        onStopCalled = true
        super.onStop()
    }

    override fun finish() {
        finishAndRemoveTask()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d(VideoPlayerActivity::class.java.name, "onNewIntent, intent: $intent")
        handleIntent(intent, true)
    }

    private fun handleIntent(intent: Intent?, update: Boolean) {
        if (intent == null) {
            return
        }
        video = intent.getParcelableExtra(EXTRA_VIDEO)
        size = intent.getIntExtra(EXTRA_SIZE, InternalVideoSize.SIZE_240)
        isLocal = intent.getBooleanExtra(EXTRA_LOCAL, false)
        val actionBar = supportActionBar
        actionBar?.title = video?.title
        actionBar?.subtitle = video?.description
        if (!isLocal && video != null) {
            mCompositeDisposable.add(OwnerInfo.getRx(
                this,
                Settings.get().accounts().current,
                (video ?: return).ownerId
            )
                .fromIOToMain()
                .subscribe({ userInfo ->
                    val av =
                        findViewById<ImageView>(R.id.toolbar_avatar)
                    av.setImageBitmap(userInfo.avatar)
                    av.setOnClickListener { onOpen() }
                    if (video?.description.isNullOrEmpty() && actionBar != null) {
                        actionBar.subtitle = userInfo.owner.fullName
                    }
                }) { })
        } else {
            findViewById<View>(R.id.toolbar_avatar).visibility = View.GONE
        }
        if (update) {
            val settings = proxySettings
            val config = settings.activeProxy
            val url = fileUrl
            mPlayer?.updateSource(this, url, config, size)
            mPlayer?.play()
            mControllerView?.updateComment(!isLocal && video?.isCanComment == true)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("size", size)
        outState.putParcelable("video", video)
        outState.putBoolean("isLocal", isLocal)
        outState.putLong("seek", mPlayer?.currentPosition ?: -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        if (Settings.get().other().isVideo_swipes) {
            attach(
                this,
                SlidrConfig.Builder().scrimColor(CurrentTheme.getColorBackground(this))
                    .fromUnColoredToColoredStatusBar(true).build()
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mDecorView = window.decorView
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.arrow_left)
            toolbar.setNavigationOnClickListener { finish() }
        }
        if (savedInstanceState == null) {
            handleIntent(intent, false)
        } else {
            size = savedInstanceState.getInt("size")
            video = savedInstanceState.getParcelable("video")
            isLocal = savedInstanceState.getBoolean("isLocal")
            seekSave = savedInstanceState.getLong("seek")
            val actionBar = supportActionBar
            actionBar?.title = video?.title
            actionBar?.subtitle = video?.description
            if (!isLocal && video != null) {
                mCompositeDisposable.add(OwnerInfo.getRx(
                    this,
                    Settings.get().accounts().current,
                    (video ?: return).ownerId
                )
                    .fromIOToMain()
                    .subscribe({ userInfo ->
                        val av =
                            findViewById<ImageView>(R.id.toolbar_avatar)
                        av.setImageBitmap(userInfo.avatar)
                        av.setOnClickListener { onOpen() }
                        if (video?.description.isNullOrEmpty() && actionBar != null) {
                            actionBar.subtitle = userInfo.owner.fullName
                        }
                    }) { })
            } else {
                findViewById<View>(R.id.toolbar_avatar).visibility = View.GONE
            }
            mControllerView?.updateComment(!isLocal && video?.isCanComment == true)
        }
        mControllerView = VideoControllerView(this)
        val surfaceContainer = findViewById<ViewGroup>(R.id.videoSurfaceContainer)
        mSurfaceView = findViewById(R.id.videoSurface)
        surfaceContainer.setOnClickListener { resolveControlsVisibility() }
        val videoHolder = mSurfaceView?.holder
        videoHolder?.addCallback(this)
        resolveControlsVisibility()
        mPlayer = createPlayer()
        mPlayer?.addVideoSizeChangeListener(this)
        mPlayer?.play()
        if (seekSave > 0) {
            mPlayer?.seekTo(seekSave)
        }
        mSpeed = findViewById(R.id.toolbar_play_speed)
        Utils.setTint(
            mSpeed,
            if (mPlayer?.isPlaybackSpeed == true) CurrentTheme.getColorPrimary(this) else Color.parseColor(
                "#ffffff"
            )
        )
        mSpeed?.setOnClickListener {
            mPlayer?.togglePlaybackSpeed()
            Utils.setTint(
                mSpeed,
                if (mPlayer?.isPlaybackSpeed == true) CurrentTheme.getColorPrimary(this) else Color.parseColor(
                    "#ffffff"
                )
            )
        }
        mControllerView?.setMediaPlayer(this)
        if (Settings.get().other().isVideo_controller_to_decor) {
            mControllerView?.setAnchorView(mDecorView as ViewGroup?, true)
        } else {
            mControllerView?.setAnchorView(findViewById(R.id.panel), false)
        }
        mControllerView?.updateComment(!isLocal && video != null && video?.isCanComment == true)
        mControllerView?.updatePip(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            ) && hasPipPermission()
        )
    }

    private fun createPlayer(): IVideoPlayer {
        val settings = proxySettings
        val config = settings.activeProxy
        val url = fileUrl
        return ExoVideoPlayer(
            this,
            url,
            config,
            size, object : IVideoPlayer.IUpdatePlayListener {
                override fun onPlayChanged(isPause: Boolean) {
                    mControllerView?.updatePausePlay()
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveControlsVisibility() {
        val actionBar = supportActionBar ?: return
        if (actionBar.isShowing) {
            actionBar.hide()
            mControllerView?.hide()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
                WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
            mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        } else {
            actionBar.show()
            mControllerView?.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
                WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT)
            mDecorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onDestroy() {
        mPlayer?.release()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(colored = false, invertIcons = false)
            .build()
            .apply(this)
        onStopCalled = false
        val actionBar = supportActionBar
        if (actionBar != null && actionBar.isShowing) {
            actionBar.hide()
            mControllerView?.hide()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
            WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
        mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        val actionBar = supportActionBar ?: return
        if (isInPictureInPictureMode) {
            actionBar.hide()
            mControllerView?.hide()
        } else {
            if (onStopCalled) {
                finish()
            } else {
                actionBar.show()
                mControllerView?.show()
            }
        }
    }

    private fun canVideoPause(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !isInPictureInPictureMode
        } else true
    }

    override fun onPause() {
        if (canVideoPause()) {
            if (!doNotPause) {
                mPlayer?.pause()
            }
            mControllerView?.updatePausePlay()
        }
        super.onPause()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceCreated(holder: SurfaceHolder) {
        mPlayer?.setSurfaceHolder(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override val bufferPercentage: Int
        get() = mPlayer?.bufferPercentage ?: 0
    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val bufferPosition: Long
        get() = mPlayer?.bufferPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun pause() {
        mPlayer?.pause()
    }

    override fun seekTo(pos: Long) {
        mPlayer?.seekTo(pos)
    }

    override fun start() {
        mPlayer?.play()
    }

    override val isFullScreen: Boolean
        get() = false

    override fun commentClick() {
        val intent = Intent(this, SwipebleActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_PLACE
        val commented = Commented.from(video ?: return)
        intent.putExtra(
            Extra.PLACE,
            PlaceFactory.getCommentsPlace(Settings.get().accounts().current, commented, null)
        )
        doNotPause = true
        applyIntent(intent)
        requestSwipeble.launch(intent)
    }

    override fun toggleFullScreen() {
        try {
            requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            CustomToast.createCustomToast(this).showToastError(R.string.not_supported)
        }
    }

    @Suppress("DEPRECATION")
    private fun hasPipPermission(): Boolean {
        val appsOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager?
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                appsOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                appsOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            else -> {
                false
            }
        }
    }

    override fun toPIPScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && hasPipPermission()
            ) if (!isInPictureInPictureMode) {
                val aspectRatio = Rational(mSurfaceView?.width ?: 0, mSurfaceView?.height ?: 0)
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                )
            }
        }
    }

    private val fileUrl: String
        get() = when (size) {
            InternalVideoSize.SIZE_240 -> video?.mp4link240 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_360 -> video?.mp4link360 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_480 -> video?.mp4link480 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_720 -> video?.mp4link720 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_1080 -> video?.mp4link1080 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_1440 -> video?.mp4link1440 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_2160 -> video?.mp4link2160 ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_HLS -> video?.hls ?: run { finish(); return "null" }
            InternalVideoSize.SIZE_LIVE -> video?.live ?: run { finish(); return "null" }
            else -> {
                finish()
                "null"
            }
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false
        }
    }

    override fun onVideoSizeChanged(player: IVideoPlayer, size: VideoSize) {
        mSurfaceView?.setAspectRatio(size.width, size.height)
    }

    override fun hideMenu(hide: Boolean) {}
    override fun openMenu(open: Boolean) {}

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        if (Utils.hasMarshmallow()) {
            var flags = window.decorView.systemUiVisibility
            flags = if (invertIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Utils.hasOreo()) {
            var flags = window.decorView.systemUiVisibility
            if (invertIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                w.decorView.systemUiVisibility = flags
                w.navigationBarColor = Color.WHITE
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
    }

    companion object {
        const val EXTRA_VIDEO = "video"
        const val EXTRA_SIZE = "size"
        const val EXTRA_LOCAL = "local"
    }
}