# Development and building

## Requirements

| Component | Version                    |
|---|----------------------------|
| JDK | 17                         |
| Gradle | 9.7.1, through the wrapper |
| Android Gradle Plugin | 9.4.0                      |
| `compileSdk` / `targetSdk` | 36                         |
| `minSdk` | 30 (Android 11)            |

## Build and check

```bash
git clone https://github.com/ouj4k2q5/MinLauncher.git
cd MinLauncher
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

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
gh attestation verify MinLauncher-0.0.3.apk --repo ouj4k2q5/MinLauncher
```
