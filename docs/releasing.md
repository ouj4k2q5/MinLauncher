# Releasing

## Automation

| Workflow | Trigger | Result |
|---|---|---|
| [`ci.yml`](../.github/workflows/ci.yml) | Push to a branch or pull request | Runs tests, lint, debug and release builds; uploads reports and the debug APK |
| [`release.yml`](../.github/workflows/release.yml) | Push of a `v*` tag | Builds and signs the release APK, verifies it, attests provenance, publishes a GitHub Release, and retains the R8 mapping for 90 days |

Release builds run tests and lint again because a tag can point at a commit that did
not pass branch CI. GitHub Actions are pinned to commit SHAs; Dependabot updates
those pins.

## Create a release

The procedure is deliberately manual; [`scripts/tag-release.sh`](../scripts/tag-release.sh)
only validates it and pushes the tag. In order:

1. Create and switch to a release branch named after the version:
  ```bash
  git switch -c release/v0.0.3
  ```
2. Bump [`version.properties`](../version.properties): `versionName` and
   `versionCode` (`major * 10000 + minor * 100 + patch`; minor and patch stay
   below 100).
3. Write the F-Droid changelog
   [`metadata/en-US/changelogs/<versionCode>.txt`](../metadata/en-US/changelogs/).
4. Commit and push the branch.
5. Run the script from the branch:
  ```bash
  ./scripts/tag-release.sh --dry-run   # validate only
  ./scripts/tag-release.sh            # create and push the tag
  ```

The script checks: the tree is clean, the branch is `release/vX.Y.Z` and pushed
(not just locally, and not behind the remote), `version.properties` actually
changed relative to `origin/main`, the branch name, `version.properties` and the
future tag all carry the same version, the version code is valid and larger than
the latest tag's, the tag does not exist yet, and the changelog is committed.
Then it pushes the annotated tag, which triggers `release.yml`.

6. Merge the release branch into `main` via a pull request:
  ```bash
  gh pr create --base main --head release/v0.0.3
  ```

A tag pushed any other way still fails in `release.yml`, which asserts the
tagged `version.properties` matches the tag:

```bash
git tag -a v1.0.0 -m "Release v1.0.0"  # fails in CI: version.properties still says the old version
git push origin v1.0.0
```

## Signing setup

Create and safely back up a release keystore; losing it prevents updates to an
installed copy of the app.

```bash
keytool -genkeypair -v \
-keystore release.jks -storetype PKCS12 \
-alias minlauncher -keyalg RSA -keysize 4096 -validity 10950
```

Keep the keystore outside the repository. Add these repository secrets under
**Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Output of `base64 -i release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | `minlauncher` |
| `KEY_PASSWORD` | Key password |

Adding required reviewers to the `release` GitHub environment makes a tag push wait
for approval before publishing.

Each release also carries an F-Droid changelog:
[`metadata/en-US/changelogs/<versionCode>.txt`](../metadata/en-US/changelogs/).
F-Droid reads it from the tagged commit, so create and commit it *before* running
`tag-release.sh`; the script warns and asks for confirmation when it is missing.
