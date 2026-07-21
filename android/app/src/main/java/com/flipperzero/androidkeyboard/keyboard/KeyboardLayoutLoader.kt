package com.flipperzero.androidkeyboard.keyboard

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File

object KeyboardLayoutLoader {

    private const val TAG = "AkB.Layouts"
    private const val TEMPLATES_CATALOG = "layouts/templates/catalog.json"
    private const val LANGUAGES_CATALOG = "layouts/languages/catalog.json"
    private const val USER_LANGUAGES_SUBDIR = "layouts/languages"

    /** App-specific folder for extra language JSON files (survives reinstall only if on external files). */
    fun userLanguagesDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, USER_LANGUAGES_SUBDIR).also { it.mkdirs() }
    }

    fun loadTemplates(context: Context): List<TemplateInfo> {
        val arr = JSONObject(readAsset(context, TEMPLATES_CATALOG)).getJSONArray("templates")
        val result = mutableListOf<TemplateInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result += TemplateInfo(
                id = obj.getString("id"),
                title = obj.getString("title"),
                file = obj.getString("file"),
            )
        }
        return result
    }

    /**
     * Bundled EN/RU from assets, plus any `*.json` dropped into [userLanguagesDir].
     * User packs with the same id override bundled ones.
     */
    fun loadLanguageCatalog(context: Context): List<LanguageInfo> {
        val byId = linkedMapOf<String, LanguageInfo>()
        for (info in loadBundledLanguages(context)) {
            byId[info.id] = info
        }
        for (info in loadUserLanguages(context)) {
            byId[info.id] = info
        }
        return byId.values.toList()
    }

    fun loadSystemLanguages(context: Context): List<LanguageInfo> {
        return SystemLanguages.availablePacks(context, loadLanguageCatalog(context))
    }

    fun loadMatchedLanguages(context: Context): List<LanguageInfo> {
        return SystemLanguages.matchedPacks(context, loadLanguageCatalog(context))
    }

    fun templateUsesLanguages(context: Context, template: TemplateInfo): Boolean {
        val root = JSONObject(readAsset(context, template.file))
        val rows = root.getJSONArray("rows")
        for (r in 0 until rows.length()) {
            val row = rows.getJSONArray(r)
            for (c in 0 until row.length()) {
                if (row.getJSONObject(c).optString("fill").isNotBlank()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Build the list of layouts the user can cycle: one entry per enabled language
     * for the selected template (or a single entry if the template has no fill keys).
     */
    fun buildEnabledLayouts(
        context: Context,
        templateId: String,
        enabledLanguageIds: List<String>,
    ): List<LayoutInfo> {
        val templates = loadTemplates(context)
        val template = templates.firstOrNull { it.id == templateId }
            ?: templates.firstOrNull()
            ?: return emptyList()
        val systemLangs = loadSystemLanguages(context)
        if (!templateUsesLanguages(context, template)) {
            return listOf(
                LayoutInfo(
                    id = template.id,
                    title = template.title,
                    templateId = template.id,
                    languageId = null,
                ),
            )
        }
        val byId = systemLangs.associateBy { it.id }
        val langs = enabledLanguageIds.mapNotNull { byId[it] }
            .ifEmpty {
                SystemLanguages.defaultEnabledIds(context, systemLangs)
                    .mapNotNull { byId[it] }
                    .ifEmpty { systemLangs }
            }
        return langs.map { lang ->
            LayoutInfo(
                id = composedLayoutId(template.id, lang.id),
                title = "${template.title} · ${lang.title}",
                templateId = template.id,
                languageId = lang.id,
            )
        }
    }

    fun loadLayout(context: Context, info: LayoutInfo): KeyboardLayout {
        val template = loadTemplates(context).first { it.id == info.templateId }
        val labels = if (info.languageId != null) {
            val lang = loadLanguageCatalog(context).first { it.id == info.languageId }
            loadLanguageLabels(context, lang)
        } else {
            emptyMap()
        }
        return compose(readAsset(context, template.file), labels, info)
    }

    fun loadLayoutById(context: Context, id: String): KeyboardLayout? {
        val parts = id.split(':', limit = 2)
        val templateId = parts[0]
        val languageId = parts.getOrNull(1)
        val layoutInfo = LayoutInfo(
            id = id,
            title = id,
            templateId = templateId,
            languageId = languageId,
        )
        return runCatching { loadLayout(context, layoutInfo) }.getOrNull()
    }

    private fun loadBundledLanguages(context: Context): List<LanguageInfo> {
        val arr = JSONObject(readAsset(context, LANGUAGES_CATALOG)).getJSONArray("languages")
        val result = mutableListOf<LanguageInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result += LanguageInfo(
                id = obj.getString("id"),
                title = obj.getString("title"),
                locales = readLocales(obj.optJSONArray("locales")),
                source = LanguageSource.Asset(obj.getString("file")),
            )
        }
        return result
    }

    private fun loadUserLanguages(context: Context): List<LanguageInfo> {
        val dir = ensureUserLanguagesDir(context)
        val files = dir.listFiles { f ->
            f.isFile &&
                f.extension.equals("json", ignoreCase = true) &&
                !f.name.startsWith("_") &&
                !f.name.equals("catalog.json", ignoreCase = true)
        }.orEmpty().sortedBy { it.name.lowercase() }

        val result = mutableListOf<LanguageInfo>()
        for (file in files) {
            val parsed = runCatching { parseUserLanguageFile(file) }.getOrElse { err ->
                Log.w(TAG, "skip user language ${file.name}: ${err.message}")
                null
            } ?: continue
            result += parsed
        }
        return result
    }

    private fun ensureUserLanguagesDir(context: Context): File {
        val dir = userLanguagesDir(context)
        val readme = File(dir, "README.txt")
        if (!readme.exists()) {
            readme.writeText(
                """
                Drop language pack JSON files here (e.g. de.json).
                Files starting with _ are ignored.
                Same id as a bundled pack (en, ru) overrides it.

                Minimal schema:
                {
                  "id": "de",
                  "name": "Deutsch",
                  "locales": ["de"],
                  "labels": { "q": "q", "w": "w" }
                }

                Label keys must match template "fill" ids (see the Default template).
                Restart the app / reopen Settings after adding files.
                """.trimIndent() + "\n",
            )
        }
        return dir
    }

    private fun parseUserLanguageFile(file: File): LanguageInfo {
        val root = JSONObject(file.readText())
        val id = root.getString("id").trim()
        require(id.isNotEmpty()) { "missing id" }
        require(root.has("labels")) { "missing labels" }
        val title = root.optString("name").ifBlank { root.optString("title") }.ifBlank { id }
        return LanguageInfo(
            id = id,
            title = title,
            locales = readLocales(root.optJSONArray("locales")).ifEmpty { listOf(id) },
            source = LanguageSource.UserFile(file.absolutePath),
        )
    }

    private fun readLocales(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    }

    private fun loadLanguageLabels(context: Context, lang: LanguageInfo): Map<String, String> {
        val root = JSONObject(readLanguageJson(context, lang))
        val labelsJson = root.getJSONObject("labels")
        val map = mutableMapOf<String, String>()
        val keys = labelsJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = labelsJson.getString(key)
        }
        return map
    }

    private fun readLanguageJson(context: Context, lang: LanguageInfo): String {
        return when (val source = lang.source) {
            is LanguageSource.Asset -> readAsset(context, source.path)
            is LanguageSource.UserFile -> File(source.absolutePath).readText()
        }
    }

    private fun compose(
        templateJson: String,
        labels: Map<String, String>,
        info: LayoutInfo,
    ): KeyboardLayout {
        val root = JSONObject(templateJson)
        val name = when {
            info.languageId == null -> root.optString("name", info.templateId)
            else -> info.title
        }
        val rowsJson = root.getJSONArray("rows")
        val rows = mutableListOf<List<KeyboardKey>>()
        for (r in 0 until rowsJson.length()) {
            rows += composeRow(rowsJson.getJSONArray(r), labels)
        }
        return KeyboardLayout(id = info.id, name = name, rows = rows)
    }

    private fun composeRow(rowJson: JSONArray, labels: Map<String, String>): List<KeyboardKey> {
        val keys = mutableListOf<KeyboardKey>()
        for (i in 0 until rowJson.length()) {
            val obj = rowJson.getJSONObject(i)
            val fill = obj.optString("fill", "").takeIf { it.isNotBlank() }
            val optional = obj.optBoolean("optional", false)
            val label = when {
                fill != null && labels.containsKey(fill) -> labels.getValue(fill)
                fill != null && optional -> continue // hide optional slot without language label
                else -> obj.getString("label")
            }
            val role = obj.optString("role", "").takeIf { it.isNotBlank() }
            keys += KeyboardKey(
                label = label,
                hid = parseHexByte(obj.getString("hid")),
                mods = parseHexByte(obj.optString("mods", "0x00")),
                span = obj.optDouble("span", 1.0).toFloat().coerceAtLeast(0.5f),
                stickyMod = obj.optBoolean("sticky_mod", false),
                role = role,
            )
        }
        return keys
    }

    private fun parseHexByte(raw: String): Byte {
        val cleaned = raw.trim().removePrefix("0x").removePrefix("0X")
        return cleaned.toInt(16).toByte()
    }

    private fun readAsset(context: Context, assetName: String): String {
        return context.assets.open(assetName).bufferedReader().use(BufferedReader::readText)
    }
}
