package dev.ragnarok.fenrir

import android.content.res.Resources
import android.os.Build
import dev.ragnarok.fenrir.db.column.GroupColumns
import dev.ragnarok.fenrir.db.column.UserColumns
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import java.util.*

object Constants {
    const val API_VERSION = "5.131"
    const val DATABASE_FENRIR_VERSION = 16
    const val DATABASE_TEMPORARY_VERSION = 4
    const val EXPORT_SETTINGS_FORMAT = 1

    @AccountType
    const val DEFAULT_ACCOUNT_TYPE: Int = BuildConfig.DEFAULT_ACCOUNT_TYPE
    const val FCM_SESSION_ID_GEN_URL: String = BuildConfig.FCM_SESSION_ID_GEN_URL

    val AUTH_VERSION = if (DEFAULT_ACCOUNT_TYPE == AccountType.KATE) API_VERSION else "5.122"
    const val FILE_PROVIDER_AUTHORITY: String = BuildConfig.APPLICATION_ID + ".file_provider"
    const val VK_ANDROID_APP_VERSION_NAME = "7.29"
    const val VK_ANDROID_APP_VERSION_CODE = "12659"
    const val KATE_APP_VERSION_NAME = "89 lite"
    const val KATE_APP_VERSION_CODE = "518"
    const val API_ID: Int = BuildConfig.VK_API_APP_ID
    const val SECRET: String = BuildConfig.VK_CLIENT_SECRET
    const val MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS
    const val PHOTOS_PATH = "DCIM/Fenrir"
    const val AUDIO_PLAYER_SERVICE_IDLE = 300000
    const val PIN_DIGITS_COUNT = 4
    const val MAX_RECENT_CHAT_COUNT = 4
    const val FRAGMENT_CHAT_APP_BAR_VIEW_COUNT = 1
    const val FRAGMENT_CHAT_DOWN_MENU_VIEW_COUNT = 0
    const val PICASSO_TAG = "picasso_tag"

    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    var DEVICE_COUNTRY_CODE = "ru"

    val KATE_USER_AGENT
        get() = String.format(
            Locale.US,
            "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
            KATE_APP_VERSION_NAME,
            KATE_APP_VERSION_CODE,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            Build.SUPPORTED_ABIS[0],
            Utils.deviceName,
            DEVICE_COUNTRY_CODE,
            SCREEN_RESOLUTION
        )

    private val KATE_USER_AGENT_FAKE
        get() = String.format(
            Locale.US,
            "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
            KATE_APP_VERSION_NAME,
            KATE_APP_VERSION_CODE,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            BuildConfig.FAKE_ABI,
            BuildConfig.FAKE_DEVICE,
            DEVICE_COUNTRY_CODE,
            SCREEN_RESOLUTION
        )
    private val VK_ANDROID_USER_AGENT
        get() = String.format(
            Locale.US,
            "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
            VK_ANDROID_APP_VERSION_NAME,
            VK_ANDROID_APP_VERSION_CODE,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            Build.SUPPORTED_ABIS[0],
            Utils.deviceName,
            DEVICE_COUNTRY_CODE,
            SCREEN_RESOLUTION
        )
    private val VK_ANDROID_USER_AGENT_FAKE
        get() = String.format(
            Locale.US,
            "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
            VK_ANDROID_APP_VERSION_NAME,
            VK_ANDROID_APP_VERSION_CODE,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            BuildConfig.FAKE_ABI,
            BuildConfig.FAKE_DEVICE,
            DEVICE_COUNTRY_CODE,
            SCREEN_RESOLUTION
        )

    private val SCREEN_RESOLUTION
        get() = Resources.getSystem().displayMetrics?.let {
            it.heightPixels.toString() + "x" + it.widthPixels
        } ?: "1920x1080"

    private fun getTypedUserAgent(@AccountType type: Int): String {
        if (type == AccountType.VK_ANDROID_HIDDEN || type == AccountType.KATE_HIDDEN) {
            val device = Settings.get().accounts().getDevice(Settings.get().accounts().current)
            if (device.nonNullNoEmpty()) {
                return if (type == AccountType.KATE_HIDDEN) String.format(
                    Locale.US,
                    "KateMobileAndroid/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
                    KATE_APP_VERSION_NAME,
                    KATE_APP_VERSION_CODE,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                    BuildConfig.FAKE_ABI,
                    device,
                    DEVICE_COUNTRY_CODE,
                    SCREEN_RESOLUTION
                ) else String.format(
                    Locale.US,
                    "VKAndroidApp/%s-%s (Android %s; SDK %d; %s; %s; %s; %s)",
                    VK_ANDROID_APP_VERSION_NAME,
                    VK_ANDROID_APP_VERSION_CODE,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT,
                    BuildConfig.FAKE_ABI,
                    device,
                    DEVICE_COUNTRY_CODE,
                    SCREEN_RESOLUTION
                )
            }
        }
        when (type) {
            AccountType.BY_TYPE, AccountType.VK_ANDROID -> return VK_ANDROID_USER_AGENT
            AccountType.VK_ANDROID_HIDDEN -> return VK_ANDROID_USER_AGENT_FAKE
            AccountType.KATE -> return KATE_USER_AGENT
            AccountType.KATE_HIDDEN -> return KATE_USER_AGENT_FAKE
        }
        return Utils.BY_DEFAULT_ACCOUNT_TYPE(VK_ANDROID_USER_AGENT, KATE_USER_AGENT)
    }

    val USER_AGENT_ACCOUNT
        get() = Settings.get().accounts().current.let {
            if (it == ISettings.IAccountsSettings.INVALID_ID) {
                Utils.BY_DEFAULT_ACCOUNT_TYPE(
                    VK_ANDROID_USER_AGENT,
                    KATE_USER_AGENT
                )
            } else getTypedUserAgent(
                Settings.get().accounts().getType(it)
            )
        }


    fun USER_AGENT(@AccountType type: Int): String {
        if (type != AccountType.BY_TYPE) {
            return getTypedUserAgent(type)
        }
        val accountId = Settings.get().accounts().current
        return if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            Utils.BY_DEFAULT_ACCOUNT_TYPE(
                VK_ANDROID_USER_AGENT,
                KATE_USER_AGENT
            )
        } else getTypedUserAgent(
            Settings.get().accounts().getType(accountId)
        )
    }
}