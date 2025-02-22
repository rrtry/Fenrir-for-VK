package dev.ragnarok.fenrir.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.api.Auth
import dev.ragnarok.fenrir.api.IValidateProvider
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemesController.currentStyle
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ValidateActivity : AppCompatActivity() {
    private var validateProvider: IValidateProvider? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var urlVal: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        urlVal = (intent.getStringExtra(EXTRA_VALIDATE) ?: return)

        validateProvider = Includes.validationProvider

        validateProvider?.let {
            mCompositeDisposable.add(
                it.observeWaiting()
                    .filter { ob -> ob == urlVal }
                    .observeOn(Includes.provideMainThreadScheduler())
                    .subscribe({ onWaitingRequestReceived() }, RxUtils.ignore())
            )
            mCompositeDisposable.add(
                it.observeCanceling()
                    .filter { ob -> ob == urlVal }
                    .observeOn(Includes.provideMainThreadScheduler())
                    .subscribe({ onRequestCancelled() }, RxUtils.ignore())
            )
        }

        attach(
            this,
            SlidrConfig.Builder().listener(object : SlidrListener {
                override fun onSlideStateChanged(state: Int) {

                }

                override fun onSlideChange(percent: Float) {

                }

                override fun onSlideOpened() {

                }

                override fun onSlideClosed(): Boolean {
                    cancel()
                    return true
                }
            }).scrimColor(CurrentTheme.getColorBackground(this)).build()
        )
        val webview = findViewById<WebView>(R.id.vkontakteview)
        webview.settings.javaScriptEnabled = true
        webview.clearCache(true)
        webview.settings.userAgentString = Constants.USER_AGENT_ACCOUNT

        //Чтобы получать уведомления об окончании загрузки страницы
        webview.webViewClient = VkontakteWebViewClient()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { aBoolean: Boolean ->
            Log.d(
                TAG,
                "Cookie removed: $aBoolean"
            )
        }
        webview.loadUrl(urlVal ?: "")
    }

    private fun cancel() {
        urlVal?.let { validateProvider?.cancel(it) }
        finish()
    }

    private fun onRequestCancelled() {
        finish()
    }

    private fun onWaitingRequestReceived() {
        urlVal?.let { validateProvider?.notifyThatValidateEntryActive(it) }
    }

    override fun onBackPressed() {

    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    private fun parseUrl(url: String?) {
        try {
            if (url == null) {
                return
            }
            Logger.d(TAG, "url=$url")
            if (url.startsWith(Auth.redirect_url)) {
                if (!url.contains("error=")) {
                    val intent = Intent()
                    try {
                        val accessToken = tryExtractAccessToken(url)
                        val userId = tryExtractUserId(url)
                        if (accessToken.nonNullNoEmpty() || userId.nonNullNoEmpty()) {
                            userId?.toInt()
                                ?.let {
                                    Settings.get().accounts().storeAccessToken(it, accessToken)
                                }
                        }
                    } catch (ignored: Exception) {
                    }
                    setResult(RESULT_OK, intent)
                }
                urlVal?.let { validateProvider?.enterState(it, true) }
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class VkontakteWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            parseUrl(url)
        }
    }

    companion object {
        private val TAG = ValidateActivity::class.java.simpleName
        private const val EXTRA_VALIDATE = "validate"


        fun createIntent(context: Context, validate_url: String?): Intent {
            return Intent(context, ValidateActivity::class.java)
                .putExtra(EXTRA_VALIDATE, validate_url)
        }

        private fun tryExtractAccessToken(url: String): String? {
            return VKStringUtils.extractPattern(url, "access_token=(.*?)&")
        }

        private fun tryExtractUserId(url: String): String? {
            return VKStringUtils.extractPattern(url, "user_id=(\\d*)")
        }
    }
}