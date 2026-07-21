package com.flipperzero.androidkeyboard.keyboard

data class TemplateInfo(
    val id: String,
    val title: String,
    val file: String,
)

/** Where a language pack JSON is loaded from. */
sealed class LanguageSource {
    data class Asset(val path: String) : LanguageSource()
    data class UserFile(val absolutePath: String) : LanguageSource()
}

data class LanguageInfo(
    val id: String,
    val title: String,
    val locales: List<String>,
    val source: LanguageSource,
) {
    val isUserPack: Boolean
        get() = source is LanguageSource.UserFile
}

/**
 * A selectable keyboard: one template filled with one language (or template-only for Number).
 * [id] is `templateId` or `templateId:languageId`.
 */
data class LayoutInfo(
    val id: String,
    val title: String,
    val templateId: String,
    val languageId: String?,
)

data class KeyboardLayout(
    val id: String,
    val name: String,
    val rows: List<List<KeyboardKey>>,
)

data class KeyboardKey(
    val label: String,
    val hid: Byte,
    val mods: Byte = 0,
    val span: Float = 1f,
    /** If true, tapping toggles sticky mods instead of sending a key. */
    val stickyMod: Boolean = false,
    /** Special role, e.g. "space" enables swipe-to-switch layouts. */
    val role: String? = null,
    /** Secondary language glyph (dual-label mode); drawn smaller under [label]. */
    val altLabel: String? = null,
) {
    val isSpace: Boolean
        get() = role == ROLE_SPACE || hid == HID_SPACE
}

const val ROLE_SPACE = "space"
const val HID_SPACE: Byte = 0x2C

fun composedLayoutId(templateId: String, languageId: String?): String {
    return if (languageId.isNullOrBlank()) templateId else "$templateId:$languageId"
}
