#!/bin/bash
#
# Create and push the release tag for the current branch, which is what triggers
# .github/workflows/release.yml to build a signed APK and publish it as a
# GitHub Release.
#
# Everything around the tag is manual (see docs/releasing.md): create a
# release/vX.Y.Z branch, bump version.properties, write the changelog, commit
# and push. This script only validates that procedure and then pushes the tag,
# so a mistake fails in a second locally instead of a few minutes into CI.
#
set -euo pipefail

## Define colors for output
GREEN='\033[1;32m'
BLUE='\033[1;34m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
NC='\033[0m' # No Color

# Ask a y/N question; succeeds only on y/Y, so callers abort on anything else.
confirm() {
echo -e "${YELLOW}$1${NC}"
read -n 1 -r answer_raw
echo
answer=$(printf "%s" "$answer_raw" | tr '[:upper:]' '[:lower:]')
[[ $answer =~ ^[Yy]$ ]]
}

# Display usage information
show_usage() {
echo -e "${BLUE}Release Tag Script Usage:${NC}"
echo ""
echo -e "${GREEN}Basic Usage:${NC}"
echo -e "  $0               # Validate the release branch and push its tag"
echo -e "  $0 --dry-run     # Validate only; show what would be pushed"
echo ""
echo -e "${GREEN}Release procedure, in order:${NC}"
echo -e "  1. git switch -c release/v<version>   # e.g. release/v0.0.3"
echo -e "  2. bump versionName/versionCode in version.properties"
echo -e "  3. write metadata/en-US/changelogs/<versionCode>.txt"
echo -e "  4. commit and push the branch"
echo -e "  5. $0"
echo -e "  6. merge the release branch into main via PR"
echo ""
echo -e "${GREEN}Notes:${NC}"
echo -e "  - versionCode is major*10000 + minor*100 + patch, so minor and patch must"
echo -e "    stay below 100. This matches the calculation in release.yml."
echo -e "  - The branch name, version.properties and the tag must all carry the same"
echo -e "    version; the script refuses to tag otherwise."
echo -e "  - Only the tag is pushed here; the branch itself must already be pushed."
echo ""
echo -e "=================================================================================="
echo ""
}

# Check for dry-run mode
DRY_RUN=false
if [ "${1:-}" = "--dry-run" ]; then
DRY_RUN=true
shift
fi

if [ $# -gt 0 ]; then
echo -e "${RED}Error: unexpected argument(s): $*${NC}"
echo ""
show_usage
exit 1
fi

# Work from the repository root regardless of where this was invoked from
REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

# A tag only does something if the workflow that reacts to it is present
if [ ! -f .github/workflows/release.yml ]; then
echo -e "${RED}Error: .github/workflows/release.yml not found${NC}"
echo -e "${YELLOW}Pushing a tag would not build or publish anything.${NC}"
exit 1
fi

# The tag must describe what was built, so the working tree has to be clean
if ! git diff-index --quiet HEAD --; then
echo -e "${RED}Error: Your working tree is not clean. Please commit or stash your changes first.${NC}"
git status --short | sed 's/^/  /'
exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# The release branch name carries the version and must match version.properties
if [[ ! "$CURRENT_BRANCH" =~ ^release/v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
echo -e "${RED}Error: not on a release branch (release/vX.Y.Z), but on '$CURRENT_BRANCH'.${NC}"
echo -e "${YELLOW}Create one with: git switch -c release/v<new version>${NC}"
exit 1
fi
BRANCH_VERSION="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"

# The tagged commit has to exist on the remote, otherwise the workflow cannot check it out
git fetch origin --tags --quiet 2>/dev/null || \
echo -e "${YELLOW}Warning: could not fetch from origin; remote checks may be stale.${NC}"

if ! git rev-parse --verify --quiet refs/remotes/origin/main; then
echo -e "${RED}Error: origin/main not found; cannot verify the version bump against it.${NC}"
exit 1
fi

if git show-ref --verify --quiet "refs/remotes/origin/$CURRENT_BRANCH"; then
UNPUSHED=$(git rev-list --count "origin/$CURRENT_BRANCH..HEAD")
if [ "$UNPUSHED" -gt 0 ]; then
 echo -e "${RED}Error: '$CURRENT_BRANCH' has $UNPUSHED unpushed commit(s).${NC}"
 echo -e "${YELLOW}The workflow builds the tagged commit from the remote, so push first:${NC}"
 echo -e "${YELLOW}  git push origin $CURRENT_BRANCH${NC}"
 exit 1
fi
if ! git merge-base --is-ancestor "origin/$CURRENT_BRANCH" HEAD; then
 echo -e "${RED}Error: '$CURRENT_BRANCH' is behind 'origin/$CURRENT_BRANCH'.${NC}"
 echo -e "${YELLOW}Pull the remote changes first: git pull --rebase origin $CURRENT_BRANCH${NC}"
 exit 1
fi
else
echo -e "${RED}Error: remote branch 'origin/$CURRENT_BRANCH' does not exist.${NC}"
echo -e "${YELLOW}Push the branch before tagging: git push -u origin $CURRENT_BRANCH${NC}"
exit 1
fi

# Print "versionName versionCode" from a version.properties file on stdin
read_version_values() {
awk -F= '
$1 == "versionName" { n = $2 }
$1 == "versionCode" { c = $2 }
END { print n, c }
'
}

# Read the version to release from the (already committed) version.properties
read -r NEW_NAME NEW_CODE < <(read_version_values < version.properties)

# The version bump itself is manual; make sure it actually happened on this
# branch, by comparing values with origin/main rather than the file diff (a
# comment-only edit should not count as a bump).
read -r MAIN_NAME MAIN_CODE < <(git show "origin/main:version.properties" 2>/dev/null | read_version_values)
if [ "$NEW_NAME" = "$MAIN_NAME" ] && [ "$NEW_CODE" = "$MAIN_CODE" ]; then
echo -e "${RED}Error: version.properties still says what origin/main says (${MAIN_NAME:-?}/${MAIN_CODE:-?}).${NC}"
echo -e "${YELLOW}Bump versionName/versionCode, commit, and push before tagging.${NC}"
exit 1
fi

if [[ ! "$NEW_NAME" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
echo -e "${RED}Error: versionName in version.properties must be X.Y.Z (e.g., 1.0.0), got '${NEW_NAME:-<empty>}'${NC}"
exit 1
fi
MAJOR="${BASH_REMATCH[1]}"; MINOR="${BASH_REMATCH[2]}"; PATCH="${BASH_REMATCH[3]}"

# Same rules release.yml enforces, checked here so a bad version never reaches CI
if (( MINOR > 99 || PATCH > 99 )); then
echo -e "${RED}Error: minor and patch must stay below 100 to keep versionCode monotonic${NC}"
echo -e "${YELLOW}Got $NEW_NAME. Bump the major version instead.${NC}"
exit 1
fi

EXPECTED_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))
if [ "$NEW_CODE" != "$EXPECTED_CODE" ]; then
echo -e "${RED}Error: versionCode in version.properties is '${NEW_CODE:-<empty>}', but $NEW_NAME requires $EXPECTED_CODE (major*10000 + minor*100 + patch).${NC}"
exit 1
fi

NEW_TAG="v$NEW_NAME"

if [ "$NEW_NAME" != "$BRANCH_VERSION" ]; then
echo -e "${RED}Error: branch '$CURRENT_BRANCH' does not match version.properties ($NEW_NAME).${NC}"
echo -e "${YELLOW}The branch name, version.properties and the tag must all carry the same version.${NC}"
exit 1
fi

# Find the latest release tag. Every v* tag in this repository belongs to this fork.
LATEST_TAG=$(git tag --list 'v*' --sort=-v:refname \
| grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1 || true)

if [ -n "$LATEST_TAG" ]; then
echo -e "${BLUE}Latest release tag: ${YELLOW}${LATEST_TAG}${NC}"
[[ "$LATEST_TAG" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]
CUR_VERSION_CODE=$(( BASH_REMATCH[1] * 10000 + BASH_REMATCH[2] * 100 + BASH_REMATCH[3] ))
if (( NEW_CODE <= CUR_VERSION_CODE )); then
 echo -e "${RED}Error: versionCode would not increase: $CUR_VERSION_CODE -> $NEW_CODE${NC}"
 echo -e "${YELLOW}Android refuses to install an update with a lower versionCode.${NC}"
 exit 1
fi
else
echo -e "${BLUE}No release tag yet — this would be the first.${NC}"
fi

# Tag must not already exist, locally or on the remote
if git rev-parse "$NEW_TAG" >/dev/null 2>&1; then
echo -e "${RED}Error: tag '$NEW_TAG' already exists locally.${NC}"
echo -e "${YELLOW}Retry its push with: git push origin $NEW_TAG${NC}"
echo -e "${YELLOW}Or drop it with: git tag -d $NEW_TAG${NC}"
exit 1
fi
if git ls-remote --tags --exit-code origin "refs/tags/$NEW_TAG" >/dev/null 2>&1; then
echo -e "${RED}Error: tag '$NEW_TAG' already exists on origin.${NC}"
exit 1
fi

# F-Droid reads the changelog for a version from the tagged commit, so it has to be
# committed before this script runs (the working tree is clean at this point).
CHANGELOG_FILE="metadata/en-US/changelogs/$NEW_CODE.txt"
if [ ! -f "$CHANGELOG_FILE" ]; then
echo -e "${YELLOW}Warning: F-Droid changelog '$CHANGELOG_FILE' does not exist.${NC}"
echo -e "${YELLOW}F-Droid will show no per-version changelog for $NEW_TAG.${NC}"
echo -e "${YELLOW}To write one: abort, create the file, commit it, and re-run this script.${NC}"
if ! confirm "Release without a changelog? (y/N)"; then
 echo -e "${RED}Aborting.${NC}"
 exit 1
fi
fi

GIT_USER=$(git config user.name || echo "unknown")
GIT_EMAIL=$(git config user.email || echo "unknown")
read -r COMMIT_SHA COMMIT_SUBJECT < <(git log -1 --format='%h %s')

# Summary
echo ""
echo -e "${GREEN}Tag:${NC}         ${YELLOW}${NEW_TAG}${NC}"
echo -e "${GREEN}versionName:${NC} $NEW_NAME"
echo -e "${GREEN}versionCode:${NC} $NEW_CODE"
echo -e "${GREEN}Commit:${NC}     $COMMIT_SHA ($COMMIT_SUBJECT)"
echo -e "${GREEN}Branch:${NC}      $CURRENT_BRANCH"
echo -e "${GREEN}Artifact:${NC}    MinLauncher-$NEW_NAME.apk"
echo ""

if [ "$DRY_RUN" = true ]; then
echo -e "${BLUE}DRY RUN MODE - No changes will be made${NC}"
echo -e "${GREEN}Would execute:${NC}"
echo -e "  git tag -a \"$NEW_TAG\" -m \"...\""
echo -e "  git push origin \"$NEW_TAG\""
echo -e "${BLUE}Dry run completed successfully${NC}"
exit 0
fi

echo -e "${RED}This pushes a tag, which starts a release build and publishes a GitHub Release.${NC}"
echo -e "${YELLOW}It needs the KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD${NC}"
echo -e "${YELLOW}secrets to be configured, or the workflow will fail at the signing step.${NC}"
if ! confirm "Proceed? (y/N)"; then
echo -e "${RED}Aborting.${NC}"
exit 1
fi

TAG_MESSAGE="Release $NEW_TAG

versionName: $NEW_NAME
versionCode: $NEW_CODE
commit:      $COMMIT_SHA
branch:      $CURRENT_BRANCH
tagged by:   $GIT_USER <$GIT_EMAIL> at $(date '+%Y-%m-%d %H:%M:%S %z')"

echo -e "${GREEN}Creating tag ${YELLOW}${NEW_TAG}${NC}"
if ! git tag -a "$NEW_TAG" -m "$TAG_MESSAGE"; then
echo -e "${RED}Error: failed to create tag '$NEW_TAG'.${NC}"
echo -e "${YELLOW}Nothing was pushed; drop the tag with: git tag -d $NEW_TAG${NC}"
exit 1
fi
echo -e "${GREEN}Successfully created tag '${YELLOW}${NEW_TAG}${GREEN}'${NC}"

# The branch is already on the remote; only the tag needs pushing. If the push
# fails, the tag can simply be dropped and recreated — nothing else was changed.
echo -e "${BLUE}Pushing the tag to remote...${NC}"
if ! git push origin "$NEW_TAG"; then
echo -e "${RED}Error: failed to push '$NEW_TAG'.${NC}"
echo -e "${YELLOW}The tag exists locally but not on the remote. Retry with:${NC}"
echo -e "${YELLOW}  git push origin $NEW_TAG${NC}"
echo -e "${YELLOW}Or drop it locally: git tag -d $NEW_TAG${NC}"
exit 1
fi
echo -e "${GREEN}Successfully pushed tag '${YELLOW}${NEW_TAG}${GREEN}' to remote${NC}"

# Point at the workflow run, deriving the web URL from the origin remote
ORIGIN_URL=$(git remote get-url origin)
REPO_SLUG=$(printf '%s' "$ORIGIN_URL" | sed -E 's|^git@[^:]+:||; s|^https?://[^/]+/||; s|\.git$||')
if [ -n "$REPO_SLUG" ]; then
echo -e "${BLUE}Watch the release build:${NC}"
echo -e "  https://github.com/$REPO_SLUG/actions"
echo -e "${BLUE}Once it finishes, the APK will be at:${NC}"
echo -e "  https://github.com/$REPO_SLUG/releases/tag/$NEW_TAG"
fi
echo -e "${BLUE}When the build is green, merge the release branch into main:${NC}"
echo -e "  gh pr create --base main --head $CURRENT_BRANCH"
echo -e "${BLUE}Release tag creation completed successfully${NC}"
