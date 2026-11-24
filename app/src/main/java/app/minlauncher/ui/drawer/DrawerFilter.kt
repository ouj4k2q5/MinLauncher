package app.minlauncher.ui.drawer

import app.minlauncher.data.AppModel
import java.text.Normalizer

private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
private val separatorsRegex = Regex("[-_+,.`'\\s\\p{Z}]")

/**
 * The drawer's search filter, extracted verbatim from the old
 * AppDrawerAdapter: a blank query shows the whole list, headers never match,
 * and labels match by plain case-insensitive contains first, then by NFD
 * normalization with diacritics and separators stripped.
 */
fun filterApps(
    source: List<AppModel>,
    query: CharSequence,
): List<AppModel> =
    if (query.isBlank()) {
        source
    } else {
        source.filter { it !is AppModel.PrivateSpaceHeader && appLabelMatches(it.appLabel, query) }
    }

/** A "!"-prefixed query is a web-search escape hatch, never an app filter. */
fun isBangSearch(query: CharSequence): Boolean = query.startsWith("!")

/**
 * Auto-launch is a search-time affordance: it must never fire for a blank
 * query, or a device whose drawer holds exactly one app (fresh emulator,
 * kiosk) would launch it the moment the drawer opens. A leading space stays
 * an explicit opt-out.
 */
fun shouldAutoLaunch(query: CharSequence): Boolean = query.isNotBlank() && !query.startsWith(" ")

private fun appLabelMatches(
    appLabel: String,
    charSearch: CharSequence,
): Boolean {
    fun normalize(value: CharSequence): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
            .replace(separatorsRegex, "")

    if (appLabel.contains(charSearch.trim(), true)) return true
    val query = normalize(charSearch)
    return query.isNotEmpty() && normalize(appLabel).contains(query, true)
}
