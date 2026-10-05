package app.minlauncher.helper

/**
 * Pure decision of whether an enumerated app belongs in the app list.
 * Kept free of Android types so it is unit-testable on the JVM.
 * [userKey] is the profile's UserHandle string; hidden-app keys are
 * formatted as "package|userHandle".
 *
 * Six named parameters read better than an untyped data holder at the
 * single call site, hence the LongParameterList suppression.
 */
@Suppress("LongParameterList")
internal fun shouldIncludeApp(
    selfPackage: String,
    appPackage: String,
    userKey: String,
    hiddenApps: Set<String>,
    includeRegularApps: Boolean,
    includeHiddenApps: Boolean,
): Boolean =
    appPackage != selfPackage &&
        if (hiddenApps.contains("$appPackage|$userKey")) {
            includeHiddenApps
        } else {
            includeRegularApps
        }
