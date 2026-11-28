package app.minlauncher.ui.home

import java.time.Clock

/**
 * Test seam for the home screen's clock and date line, which would otherwise
 * make screenshot baselines time-dependent. Tests point this at a fixed
 * instant; production behavior is unchanged.
 *
 * Named [HomeClockSource] (not HomeClock) to avoid colliding with the
 * existing composables in HomeClock.kt. [app.minlauncher.ui.HomeFragment.clock]
 * is a delegation alias so existing tests keep working unmodified.
 */
internal object HomeClockSource {
    var clock: Clock = Clock.systemDefaultZone()
}
