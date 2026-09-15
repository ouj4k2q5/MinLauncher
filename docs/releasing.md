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

Use [`scripts/tag-release.sh`](../scripts/tag-release.sh) from a clean, pushed
working tree. It validates the same release conditions before creating and pushing
the tag:

```bash
./scripts/tag-release.sh custom 1.0.0
./scripts/tag-release.sh patch
./scripts/tag-release.sh minor
./scripts/tag-release.sh major
./scripts/tag-release.sh --dry-run patch
```

Tags must be `vMAJOR.MINOR.PATCH`. The version code is `major * 10000 + minor *
100 + patch`; minor and patch must be below 100. Before tagging, the script
commits the new version to [`version.properties`](../version.properties) and
pushes that commit to the current branch — `release.yml` checks this matches
the tag and fails otherwise, so a tag can't be created any other way than
through this script:

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
for approval before publishing. A GitHub fork may retain upstream tags; removing
those inherited tags keeps this fork's release list clear, but is not required by
`tag-release.sh`.
