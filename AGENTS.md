# AGENTS.md

MinLauncher: a minimal, offline Android home-screen launcher (personal fork of
Olauncher). Single Gradle module `:app`, Kotlin sources under
`app/src/main/java/app/minlauncher/`, JVM tests under `app/src/test/`.

## Hard constraints

- **No network access, ever.** The app does not declare the `INTERNET` permission
  and has no analytics/telemetry. Do not add network features or dependencies that
  pull them in (this also means no WorkManager — removed by design).
- **Never regenerate `app/detekt-baseline.xml` to make detekt pass.** The baseline
  is a ratchet: fix new violations in code. CI compares the entry count against
  `config/detekt/baseline-max` and fails if the baseline grew. When you fix a
  baselined violation, delete its entry from the baseline.

## Verify before committing

CI (`.github/workflows/ci.yml`) runs exactly this — run the same locally:

```bash
./gradlew test lint detekt ktlintCheck assembleDebug assembleRelease
```

`assembleRelease` is intentional: R8 shrinking problems only surface there.
Useful subsets:

```bash
./gradlew ktlintFormat                          # auto-fix formatting (.editorconfig, 120-col)
./gradlew test --tests "app.minlauncher.data.ShortcutIdentityTest"   # single test class
```

Only JUnit 4 JVM tests exist locally; there is no emulator/instrumentation setup.

## Toolchain quirks

- JDK 17 required. AGP 9.4.0, Gradle 9.7.1 via the wrapper, Kotlin 2.2 via AGP's
  built-in Kotlin support (no `kotlin-android` plugin, no buildscript classpath).
- AGP 9 DSL differs from older examples you may know:
  `compileSdk { version = release(36) }`, `minSdk { version = release(30) }`,
  `androidResources { localeFilters += setOf("en", "ja") }` — not
  `resourceConfigurations`.
- Versions are centralized in `gradle/libs.versions.toml` (plugins DSL).
- detekt 1.23.8 prints a Gradle 10 deprecation warning (`ReportingExtension.file`)
  — harmless; fixed in detekt 2.x.

## Versioning and releases

- `version.properties` holds the committed `versionName`/`versionCode`
  (`major*10000 + minor*100 + patch`). The literal is required by F-Droid; do not
  move versioning into injected build properties. Local override:
  `./gradlew assembleDebug -PappVersionName=1.2.3 -PappVersionCode=10203`.
- Releases happen **only** via `./scripts/tag-release.sh [patch|minor|major|custom x.y.z]`
  from a clean, pushed tree. It commits `version.properties`, then a `Builds:` entry in
  the F-Droid recipe draft (`docs/f-droid/`) referencing that commit's full hash
  (two commits total), tags the second commit, and pushes both;
  `release.yml` fails if the tag and `version.properties` disagree. It expects
  `metadata/en-US/changelogs/<versionCode>.txt` to exist beforehand. Never tag manually.
- Release signing comes from env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`). Without them, `assembleRelease` still works but
  produces an unsigned, uninstallable APK — that's expected, not a build error.

## Conventions

- Debug builds use the `.debug` application-ID suffix, so they install alongside
  release builds.
- Further docs live in `docs/` — `development.md` (build), `releasing.md`
  (release process), `fork-changes.md` (what differs from upstream Olauncher and
  why code was removed), `privacy.md` (permission rationale).
