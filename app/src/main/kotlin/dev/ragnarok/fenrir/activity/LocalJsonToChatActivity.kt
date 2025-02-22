package dev.ragnarok.fenrir.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.PhotoPagerActivity.Companion.newInstance
import dev.ragnarok.fenrir.fragment.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.AudioPlayerFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.LocalJsonToChatFragment
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils

class LocalJsonToChatActivity : NoMainActivity(), PlaceProvider, AppStyleable {
    private val mOnBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { keyboardHide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
            supportFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val accountId = Settings.get().accounts().current
        if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            finish()
            return
        }
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            attachInitialFragment(LocalJsonToChatFragment.newInstance(accountId))
        }
    }

    fun keyboardHide() {
        try {
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            inputManager?.hideSoftInputFromWindow(
                window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } catch (ignored: Exception) {
        }
    }

    private fun attachInitialFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(getMainContainerViewId(), fragment)
            .addToBackStack("primary_local_chat")
            .commitAllowingStateLoss()
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.VK_PHOTO_ALBUM_GALLERY, Place.FAVE_PHOTOS_GALLERY, Place.SIMPLE_PHOTO_GALLERY, Place.VK_PHOTO_TMP_SOURCE, Place.VK_PHOTO_ALBUM_GALLERY_SAVED, Place.VK_PHOTO_ALBUM_GALLERY_NATIVE -> newInstance(
                this,
                place.type,
                args
            )?.let {
                place.launchActivityForResult(
                    this,
                    it
                )
            }
            Place.SINGLE_PHOTO, Place.GIF_PAGER -> {
                val ph = Intent(this, PhotoFullScreenActivity::class.java)
                ph.action = PhotoFullScreenActivity.ACTION_OPEN_PLACE
                ph.putExtra(Extra.PLACE, place)
                startActivity(ph)
            }
            Place.DOC_PREVIEW -> {
                val document: Document? = args.getParcelable(Extra.DOC)
                if (document != null && document.hasValidGifVideoLink()) {
                    val aid = args.getInt(Extra.ACCOUNT_ID)
                    val documents = ArrayList(listOf(document))
                    val gf = Intent(this, PhotoFullScreenActivity::class.java)
                    gf.action = PhotoFullScreenActivity.ACTION_OPEN_PLACE
                    gf.putExtra(Extra.PLACE, PlaceFactory.getGifPagerPlace(aid, documents, 0))
                    startActivity(gf)
                } else {
                    Utils.openPlaceWithSwipebleActivity(this, place)
                }
            }
            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                newInstance(args).show(supportFragmentManager, "audio_player")
            }
            else -> Utils.openPlaceWithSwipebleActivity(this, place)
        }
    }

    public override fun onPause() {
        ViewUtils.keyboardHide(this)
        super.onPause()
    }

    public override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)
        ViewUtils.keyboardHide(this)
        super.onDestroy()
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
}