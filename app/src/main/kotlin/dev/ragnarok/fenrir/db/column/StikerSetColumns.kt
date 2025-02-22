package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object StikerSetColumns : BaseColumns {
    const val TABLENAME = "sticker_set"
    const val POSITION = "position"
    const val TITLE = "title"
    const val ICON = "icon"
    const val PURCHASED = "purchased"
    const val PROMOTED = "promoted"
    const val ACTIVE = "active"
    const val STICKERS = "stickers"
}