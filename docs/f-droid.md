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
  [`docs/f-droid/io.github.ouj4k2q5.minlauncher.yml`](f-droid/io.github.ouj4k2q5.minlauncher.yml)
  to copy into a merge request there.
- What *does* live in this repository is the metadata F-Droid reads directly
  from source: [`metadata/en-US/`](../metadata/en-US/) (title, descriptions,
  changelogs, and screenshots — the same
  [fastlane-compatible layout](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)
  used by other stores, without needing the `fastlane` tool itself).

## One-time submission

1. Verify the draft recipe still matches
   [F-Droid's build metadata reference](https://f-droid.org/docs/Build_Metadata_Reference/).
2. Test it with F-Droid's build tooling (`fdroid build` / `fdroid checkupdates`)
   before submitting — in particular the `prebuild:` step that injects
   `appVersionName`/`appVersionCode`, since this project normally gets those
   from Gradle properties passed by `release.yml`, and F-Droid's plain
   `gradle` build step does not pass them on its own. This has not been
   tested against real F-Droid build tooling.
3. Open a merge request against `fdroiddata` with the recipe.

## Ongoing maintenance

Once accepted, `UpdateCheckMode: Tags` means F-Droid's tooling automatically
detects new `vMAJOR.MINOR.PATCH` tags (the same tags
[`scripts/tag-release.sh`](../scripts/tag-release.sh) already creates) and
proposes a new build entry — no action needed here for
routine releases. The recipe only needs a follow-up MR if the build process
itself changes in a way F-Droid's build environment can't handle (for
example, a new dependency source, or a Gradle/AGP upgrade that breaks the
`prebuild:` workaround above).

## Screenshots

Screenshots for the F-Droid listing live in
[`metadata/en-US/images/phoneScreenshots/`](../metadata/en-US/images/phoneScreenshots/README.md).
The same image files can be referenced from the main
[README](../README.md) with a relative path, so there is no need to maintain
two copies.
