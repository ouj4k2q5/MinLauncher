package app.minlauncher.ui.theme

import android.content.res.TypedArray
import android.util.TypedValue

/**
 * Fail loudly (like getColorFromAttr) instead of silently painting transparent
 * when a theme attribute a Compose palette depends on is missing.
 */
internal fun TypedArray.requireColor(
    index: Int,
    name: String,
): Int {
    val value = TypedValue()
    check(getValue(index, value)) { "Failed to resolve theme attribute $name" }
    check(value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
        "Theme attribute $name is not a color (type=${value.type})"
    }
    return value.data
}

/**
 * Fail loudly when a theme attribute a Compose palette depends on is missing or
 * is not the expected value type.
 */
internal fun TypedArray.requireString(
    index: Int,
    name: String,
): String {
    val value = TypedValue()
    check(getValue(index, value)) { "Failed to resolve theme attribute $name" }
    check(value.type == TypedValue.TYPE_STRING) {
        "Theme attribute $name is not a string (type=${value.type})"
    }
    return checkNotNull(value.string) { "Theme attribute $name is empty" }.toString()
}
