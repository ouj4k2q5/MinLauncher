# Development and building

## Requirements

| Component | Version                    |
|---|----------------------------|
| JDK | 21                         |
| Gradle | 9.9.2, through the wrapper |
| Android Gradle Plugin | 9.5.3                      |
| `compileSdk` | 37                         |
| `targetSdk` | 36                         |
| `minSdk` | 30 (Android 11)            |

## Build and check

```bash
git clone https://github.com/ouj4k2q5/MinLauncher.git
cd MinLauncher
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

## On-device smoke tests

Besides the JVM/Robolectric suite, a few Espresso smoke tests run on a Gradle
Managed Device — a Pixel 2 emulator with the headless, test-optimized
[aosp-atd](https://developer.android.com/studio/test/gradle-managed-devices#atd-images)
API 30 image (matching `minSdk`):

```bash
./gradlew pixel2api30DebugAndroidTest
```

The first run downloads a ~1.2 GB system image. Because of that cost these tests
are not part of the per-push CI command; `.github/workflows/instrumented.yml`
runs them nightly and on demand (workflow_dispatch), where the runner's KVM makes
the emulator fast enough.

## Screenshot tests

The Robolectric suite includes Roborazzi screenshot tests
([ScreenShotTest.kt](../app/src/test/java/app/minlauncher/ui/ScreenShotTest.kt))
that capture the home screen (dark and light), the app drawer, a filtered drawer,
and the settings screen. The baselines are the PNGs committed in
[`app/roborazzi/`](../app/roborazzi/), and CI's `verifyRoborazziDebug` step fails
when a screenshot no longer matches its baseline (plain `test` silently skips
the capture).

When a UI change intentionally alters a screen, re-record the baselines:

1. `./gradlew recordRoborazziDebug` — re-capture the screenshots
2. `./gradlew verifyRoborazziDebug` — confirm they now match
3. Commit the baselines that intentionally changed (`app/roborazzi/*.png`) and
   describe the visual diff in the commit message; an unintentionally changed
   screenshot means the UI changed more than you expected

When verification fails locally or in CI, the `*_actual.png` / `*_compare.png`
diff images land in `app/build/intermediates/roborazzi/` (CI uploads them in
the `reports` artifact).

## Test coverage

JVM test coverage is measured with AGP's built-in JaCoCo integration
(`enableUnitTestCoverage` on the debug build type; report generation only —
there is deliberately no minimum-coverage gate):

```bash
./gradlew createDebugUnitTestCoverageReport
```

The HTML report and the JaCoCo XML land in
`app/build/reports/coverage/test/debug/`. CI generates the report on every push
and includes it in the `reports` artifact. The JaCoCo version is pinned in
[libs.versions.toml](../gradle/libs.versions.toml).

CI also posts the coverage table as a pull request comment (updated on each
push while the PR is open) and to the Actions run's summary page, via the
pinned [madrapps/jacoco-report](https://github.com/Madrapps/jacoco-report)
action. This is report-only: there is no coverage threshold that fails the
build, and fork PRs skip the comment because their `GITHUB_TOKEN` is
read-only.

## Code style and static analysis

Kotlin code is checked by [ktlint](https://pinterest.github.io/ktlint/) (formatting,
configured through [.editorconfig](../.editorconfig)) and
[detekt](https://detekt.dev/) (static analysis, configured through
[config/detekt/detekt.yml](../config/detekt/detekt.yml)). Existing detekt findings
that need code changes are recorded in `app/detekt-baseline.xml`; only new
violations fail the build.

The baseline works as a ratchet — it may shrink, but never grow:

- If detekt fails on new code, fix the code. Do not regenerate the baseline
  with `detektBaseline` on top of new violations: it silently absorbs them.
- When you fix a baselined violation, delete the matching entries from
  `app/detekt-baseline.xml`. If detekt still passes afterwards, nothing was
  missed.
- CI enforces this: the number of baseline entries must not exceed the count
  in `config/detekt/baseline-max`. Deliberately exempting new violations
  requires raising that number in the same commit, so the decision is
  reviewed. Lowering the count after shrinking the baseline is encouraged but
  optional.

```bash
./gradlew ktlintCheck    # check formatting
./gradlew ktlintFormat   # apply formatting automatically
./gradlew detekt         # run static analysis
```

The debug build uses the `.debug` application-ID suffix, so it can be installed
alongside a release build or upstream Olauncher:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

`assembleRelease` creates an unsigned APK unless signing credentials are present
in the environment. An unsigned APK cannot be installed; signing is documented in
[Releasing](releasing.md).

## Version overrides

Local builds default to whatever [`version.properties`](../version.properties)
has committed — normally the last released version. Override it when testing
a different version:

```bash
./gradlew assembleDebug -PappVersionName=1.2.3 -PappVersionCode=10203
```

Published release APKs include a SLSA build provenance attestation. Verify a
download before installing it with:

```bash
gh attestation verify MinLauncher-0.0.4.apk --repo ouj4k2q5/MinLauncher
```
