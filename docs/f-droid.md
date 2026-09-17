# F-Droid

MinLauncher has no `INTERNET` permission and no non-free dependencies, so it
is a reasonable fit for [F-Droid](https://f-droid.org). Distribution there
works differently from [`release.yml`](../.github/workflows/release.yml):

- F-Droid's own build servers clone this repository and build the APK from
  source. They do not use the APK produced by `release.yml`, and they sign
  the result with F-Droid's own key, so the `KEYSTORE_*` secrets are
  unrelated to F-Droid distribution.
- The recipe that tells F-Droid how to build the app lives in a separate
  repository, [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata), not here.
  A draft of that recipe is kept at
  [`docs/f-droid/io.github.ouj4k2q5.minlauncher`](f-droid/io.github.ouj4k2q5.minlauncher)
  to copy into a merge request there.
- What *does* live in this repository is the metadata F-Droid reads directly
  from source: [`metadata/en-US/`](../metadata/en-US/) (title, descriptions,
  changelogs, and screenshots — the same
  [fastlane-compatible layout](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
  used by other stores, without needing the `fastlane` tool itself), and
  [`version.properties`](../version.properties) (see below).

## Why `version.properties` exists

F-Droid's build server runs a plain `gradle assembleRelease` — it does not pass
the `-PappVersionName`/`-PappVersionCode` properties that `release.yml` uses.
And `UpdateCheckMode: Tags`, F-Droid's mechanism for noticing a new release,
works by regex-scanning specific files in each tagged revision for a literal
version — it does not run Gradle and cannot resolve a property reference.

So the version needs to exist as a literal, checked-in value, not only as
something computed from the tag at build time. [`version.properties`](../version.properties)
is that literal: [`scripts/tag-release.sh`](../scripts/tag-release.sh) commits
the new `versionName`/`versionCode` to it *before* creating the tag, so the
tagged commit already carries the correct version in source. `app/build.gradle.kts`
reads it as the default (an explicit `-P` override still wins, for local
testing — see [Development](development.md)), and `release.yml` asserts it
matches the tag before building, so a tag pushed without going through
`tag-release.sh` fails loudly instead of shipping a mislabeled build.
The draft recipe's `UpdateCheckData` field points F-Droid's checker at this
same file.

## One-time submission

1. Verify the draft recipe still matches
   [F-Droid's build metadata reference](https://f-droid.org/docs/Build_Metadata_Reference/).
2. Test it with F-Droid's build tooling (`fdroid build` / `fdroid checkupdates`)
   before submitting. This has not been tested against real F-Droid build
   tooling.
3. Point the recipe's first `Builds:` entry at a tag created *after*
   `version.properties` started being committed by `tag-release.sh` — earlier
   tags (including `v0.0.1`) predate that file and won't build correctly
   under F-Droid's plain `gradle` step.
4. Open a merge request against `fdroiddata` with the recipe.

## Ongoing maintenance

Once accepted, `UpdateCheckMode: Tags` plus the `UpdateCheckData` pointer at
`version.properties` means F-Droid's tooling can detect new `vMAJOR.MINOR.PATCH`
tags (the same tags `scripts/tag-release.sh` already creates) directly from
that literal file and add a new build entry — no MR needed here for routine
releases, *if* this detection is confirmed working per step 2 above. The
recipe still needs a follow-up MR if the build process itself changes in a
way F-Droid's build environment can't handle (for example, a new dependency
source, or a Gradle/AGP upgrade).

## Screenshots

Screenshots for the F-Droid listing live in
[`metadata/en-US/images/phoneScreenshots/`](../metadata/en-US/images/phoneScreenshots/README.md).
The same image files can be referenced from the main
[README](../README.md) with a relative path, so there is no need to maintain
two copies.
