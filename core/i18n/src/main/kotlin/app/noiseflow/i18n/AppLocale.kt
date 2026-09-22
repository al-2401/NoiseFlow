package app.noiseflow.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language.
 *
 * On Android 13+ this goes through the platform LocaleManager and shows up in
 * system settings; AppCompat back-ports the same call below that. Worth the
 * few lines: people who live between two languages routinely want the system
 * in one and their apps in another, and almost no competitor offers it.
 */
object AppLocale {

    /** Languages the app ships with; mirrored in res/xml/locales_config.xml. */
    val supported = listOf(
        "en", "ru", "uk", "de", "es", "fr", "it", "pt-BR", "pl", "tr",
    )

    fun current(): String? = AppCompatDelegate.getApplicationLocales()
        .takeUnless { it.isEmpty }
        ?.get(0)
        ?.toLanguageTag()

    /** Pass null to follow the system again. */
    fun set(tag: String?) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }
}
