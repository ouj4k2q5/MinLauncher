#!/bin/bash
#
# Create and push a release tag, which is what triggers .github/workflows/release.yml
# to build a signed APK and publish it as a GitHub Release.
#
# Every check the release workflow performs on the tag is performed here first, so a
# malformed version fails in a second locally instead of a few minutes into CI.
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
echo -e "  $0 <action> [version]"
echo -e "  $0 --dry-run <action> [version]   # Preview without creating or pushing"
echo ""
echo -e "${GREEN}Actions:${NC}"
echo -e "  ${YELLOW}major${NC}      - Increment major version (1.2.3 -> 2.0.0)"
echo -e "  ${YELLOW}minor${NC}      - Increment minor version (1.2.3 -> 1.3.0)"
echo -e "  ${YELLOW}patch${NC}      - Increment patch version (1.2.3 -> 1.2.4)"
echo -e "  ${YELLOW}custom${NC}     - Use an explicit version (X.Y.Z)"
echo ""
echo -e "${GREEN}Examples:${NC}"
echo -e "  $0 custom 1.0.0             # First release: v1.0.0 (versionCode 10000)"
echo -e "  $0 patch                    # v1.0.0 -> v1.0.1 (versionCode 10001)"
echo -e "  $0 minor                    # v1.0.1 -> v1.1.0 (versionCode 10100)"
echo -e "  $0 major                    # v1.1.0 -> v2.0.0 (versionCode 20000)"
echo -e "  $0 --dry-run patch          # Show what would happen"
echo ""
echo -e "${GREEN}Notes:${NC}"
echo -e "  - versionCode is major*10000 + minor*100 + patch, so minor and patch must"
echo -e "    stay below 100. This matches the calculation in release.yml."
echo -e "  - Expects the F-Droid changelog metadata/en-US/changelogs/<versionCode>.txt"
echo -e "    for the new version to already exist and be committed."
echo -e "  - This makes two commits: the version.properties bump, then an F-Droid"
echo -e "    recipe update (docs/f-droid/) whose Builds entry references the bump"
echo -e "    commit's full hash. The tag points at the recipe commit, and both are"
echo -e "    pushed to the current branch, so it needs write access to push to."
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

# Check arguments
if [ $# -lt 1 ]; then
show_usage
exit 1
fi

ACTION=$1
VERSION_ARG=${2:-}

if [[ ! "$ACTION" =~ ^(major|minor|patch|custom)$ ]]; then
echo -e "${RED}Error: Action must be one of: major, minor, patch, custom${NC}"
echo ""
show_usage
exit 1
fi

if [ "$ACTION" = "custom" ] && [ -z "$VERSION_ARG" ]; then
echo -e "${RED}Error: 'custom' action requires a version argument${NC}"
echo -e "${YELLOW}Use: $0 custom <version> (e.g., 1.0.0)${NC}"
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

# Draft of the fdroiddata build recipe. The script keeps it in sync with each release:
# a new Builds entry and the CurrentVersion* fields.
FDROID_RECIPE="docs/f-droid/io.github.ouj4k2q5.minlauncher"

# Working tree must be clean, or the tag would not describe what was built
if ! git diff-index --quiet HEAD --; then
echo -e "${RED}Error: Your working tree is not clean. Please commit or stash your changes first.${NC}"
git status --short | sed 's/^/  /'
exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Warn when tagging from somewhere other than the default branch
DEFAULT_BRANCH=$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's|^origin/||' || true)
if [ -n "$DEFAULT_BRANCH" ] && [ "$CURRENT_BRANCH" != "$DEFAULT_BRANCH" ]; then
echo -e "${YELLOW}Warning: tagging from '$CURRENT_BRANCH', not the default branch '$DEFAULT_BRANCH'.${NC}"
if ! confirm "Do you want to continue? (y/N)"; then
 echo -e "${RED}Aborting.${NC}"
 exit 1
fi
fi

# The tagged commit has to exist on the remote, otherwise the workflow cannot check it out
git fetch origin --tags --quiet 2>/dev/null || \
echo -e "${YELLOW}Warning: could not fetch from origin; remote checks may be stale.${NC}"

if git show-ref --verify --quiet "refs/remotes/origin/$CURRENT_BRANCH"; then
UNPUSHED=$(git rev-list --count "origin/$CURRENT_BRANCH..HEAD")
if [ "$UNPUSHED" -gt 0 ]; then
 echo -e "${RED}Error: '$CURRENT_BRANCH' has $UNPUSHED unpushed commit(s).${NC}"
 echo -e "${YELLOW}The workflow builds the tagged commit from the remote, so push first:${NC}"
 echo -e "${YELLOW}  git push origin $CURRENT_BRANCH${NC}"
 exit 1
fi
else
echo -e "${RED}Error: remote branch 'origin/$CURRENT_BRANCH' does not exist.${NC}"
echo -e "${YELLOW}Push the branch before tagging: git push -u origin $CURRENT_BRANCH${NC}"
exit 1
fi

# Find the latest release tag. Every v* tag in this repository belongs to this fork.
LATEST_TAG=$(git tag --list 'v*' --sort=-v:refname \
| grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1 || true)

if [ -n "$LATEST_TAG" ]; then
echo -e "${BLUE}Latest release tag: ${YELLOW}${LATEST_TAG}${NC}"
else
echo -e "${BLUE}No release tag yet — this would be the first.${NC}"
fi

# Work out the new version
if [ -n "$LATEST_TAG" ]; then
[[ "$LATEST_TAG" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]
CUR_MAJOR="${BASH_REMATCH[1]}"
CUR_MINOR="${BASH_REMATCH[2]}"
CUR_PATCH="${BASH_REMATCH[3]}"
else
CUR_MAJOR=0; CUR_MINOR=0; CUR_PATCH=0
fi

case "$ACTION" in
major)
 MAJOR=$((CUR_MAJOR + 1)); MINOR=0; PATCH=0
 ;;
minor)
 MAJOR=$CUR_MAJOR; MINOR=$((CUR_MINOR + 1)); PATCH=0
 ;;
patch)
 MAJOR=$CUR_MAJOR; MINOR=$CUR_MINOR; PATCH=$((CUR_PATCH + 1))
 ;;
custom)
 if [[ ! "$VERSION_ARG" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
   echo -e "${RED}Error: version must be X.Y.Z (e.g., 1.0.0), got '$VERSION_ARG'${NC}"
   exit 1
 fi
 MAJOR="${BASH_REMATCH[1]}"; MINOR="${BASH_REMATCH[2]}"; PATCH="${BASH_REMATCH[3]}"
 ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_TAG="v$NEW_VERSION"

# Same rules release.yml enforces, checked here so a bad tag never reaches CI
if (( MINOR > 99 || PATCH > 99 )); then
echo -e "${RED}Error: minor and patch must stay below 100 to keep versionCode monotonic${NC}"
echo -e "${YELLOW}Got $NEW_VERSION. Bump the major version instead.${NC}"
exit 1
fi

NEW_VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))

if [ -n "$LATEST_TAG" ]; then
CUR_VERSION_CODE=$(( CUR_MAJOR * 10000 + CUR_MINOR * 100 + CUR_PATCH ))
if (( NEW_VERSION_CODE <= CUR_VERSION_CODE )); then
 echo -e "${RED}Error: versionCode would not increase: $CUR_VERSION_CODE -> $NEW_VERSION_CODE${NC}"
 echo -e "${YELLOW}Android refuses to install an update with a lower versionCode.${NC}"
 exit 1
fi
fi

# Tag must not already exist, locally or on the remote
if git rev-parse "$NEW_TAG" >/dev/null 2>&1; then
echo -e "${RED}Error: tag '$NEW_TAG' already exists locally.${NC}"
exit 1
fi
if git ls-remote --tags --exit-code origin "refs/tags/$NEW_TAG" >/dev/null 2>&1; then
echo -e "${RED}Error: tag '$NEW_TAG' already exists on origin.${NC}"
exit 1
fi

if [ ! -f "$FDROID_RECIPE" ]; then
echo -e "${RED}Error: F-Droid recipe draft not found at $FDROID_RECIPE${NC}"
exit 1
fi

# F-Droid reads the changelog for a version from the tagged commit, so it has to be
# committed before this script runs (the working tree is clean at this point).
CHANGELOG_FILE="metadata/en-US/changelogs/$NEW_VERSION_CODE.txt"
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
read -r BASE_COMMIT_SHA BASE_COMMIT_SUBJECT < <(git log -1 --format='%h %s')

# Summary
echo ""
echo -e "${GREEN}Tag:${NC}         ${YELLOW}${NEW_TAG}${NC}"
echo -e "${GREEN}versionName:${NC} $NEW_VERSION"
echo -e "${GREEN}versionCode:${NC} $NEW_VERSION_CODE"
echo -e "${GREEN}Base commit:${NC} $BASE_COMMIT_SHA ($BASE_COMMIT_SUBJECT)"
echo -e "${GREEN}Branch:${NC}      $CURRENT_BRANCH"
echo -e "${GREEN}Artifact:${NC}    MinLauncher-$NEW_VERSION.apk"
echo ""
echo -e "${BLUE}Two commits will be made on top of the base commit: first the version.properties${NC}"
echo -e "${BLUE}bump, then an F-Droid recipe update whose Builds entry references the bump${NC}"
echo -e "${BLUE}commit's full hash. The tag points at the recipe commit, and F-Droid builds the${NC}"
echo -e "${BLUE}commit recorded in the recipe's entry (the bump commit).${NC}"

if [ "$DRY_RUN" = true ]; then
echo -e "${BLUE}DRY RUN MODE - No changes will be made${NC}"
echo -e "${GREEN}Would execute:${NC}"
echo -e "  sed -i -E 's/^versionName=.*/versionName=$NEW_VERSION/; s/^versionCode=.*/versionCode=$NEW_VERSION_CODE/' version.properties"
echo -e "  git commit version.properties -m \"chore: release $NEW_TAG\""
echo -e "  insert a Builds entry (commit: <full hash of that commit>) and update CurrentVersion* in $FDROID_RECIPE"
echo -e "  git commit $FDROID_RECIPE -m \"chore: record $NEW_TAG in the F-Droid recipe\""
echo -e "  git tag -a \"$NEW_TAG\" -m \"...\""
echo -e "  git push --atomic origin \"$CURRENT_BRANCH\" \"$NEW_TAG\""
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

echo -e "${GREEN}Updating version.properties${NC}"
# Rewrite only the two value lines; the leading comment header is left untouched.
sed -i.bak -E \
-e "s/^versionName=.*/versionName=$NEW_VERSION/" \
-e "s/^versionCode=.*/versionCode=$NEW_VERSION_CODE/" \
version.properties
rm -f version.properties.bak

if ! git commit version.properties -m "chore: release $NEW_TAG"; then
echo -e "${RED}Error: failed to commit the version bump${NC}"
git checkout -- version.properties
exit 1
fi

# The F-Droid build metadata reference asks for the full commit hash in the commit
# field, not a tag name. The hash only exists once the version bump commit above
# does, which is why a release is two commits: the bump (recorded in the recipe),
# then the recipe update itself, with the tag landing on the recipe commit so the
# tagged tree still carries the new version.
RELEASE_COMMIT=$(git rev-parse HEAD)

# The new Builds entry goes on top (fdroiddata convention: newest first) and CurrentVersion*
# move to this release.
if ! awk -v name="$NEW_VERSION" -v code="$NEW_VERSION_CODE" -v sha="$RELEASE_COMMIT" '
 !inserted && $0 == "Builds:" {
   print
   print " - versionName: " name
   print "   versionCode: " code
   print "   commit: " sha
   print "   gradle:"
   print "     - yes"
   inserted = 1
   next
 }
 /^CurrentVersion:/ { print "CurrentVersion: " name; next }
 /^CurrentVersionCode:/ { print "CurrentVersionCode: " code; next }
 { print }
' "$FDROID_RECIPE" > "$FDROID_RECIPE.new" || ! grep -q "^ - versionName: $NEW_VERSION$" "$FDROID_RECIPE.new"; then
echo -e "${RED}Error: failed to add a Builds entry for $NEW_VERSION to $FDROID_RECIPE${NC}"
rm -f "$FDROID_RECIPE.new"
echo -e "${YELLOW}Undoing the version bump commit with: git reset --hard HEAD~1${NC}"
git reset --hard HEAD~1
exit 1
fi
mv "$FDROID_RECIPE.new" "$FDROID_RECIPE"

if ! git commit "$FDROID_RECIPE" -m "chore: record $NEW_TAG in the F-Droid recipe"; then
echo -e "${RED}Error: failed to commit the recipe update${NC}"
echo -e "${YELLOW}Undoing the version bump commit with: git reset --hard HEAD~1${NC}"
git reset --hard HEAD~1
exit 1
fi
echo -e "${GREEN}Successfully committed the version bump and recipe update${NC}"

read -r COMMIT_SHA COMMIT_SUBJECT < <(git log -1 --format='%h %s')
TAG_MESSAGE="Release $NEW_TAG

versionName: $NEW_VERSION
versionCode: $NEW_VERSION_CODE
commit:      $COMMIT_SHA
branch:      $CURRENT_BRANCH
tagged by:   $GIT_USER <$GIT_EMAIL> at $(date '+%Y-%m-%d %H:%M:%S %z')"

echo -e "${GREEN}Creating tag ${YELLOW}${NEW_TAG}${NC}"
if ! git tag -a "$NEW_TAG" -m "$TAG_MESSAGE"; then
echo -e "${RED}Error: failed to create tag '$NEW_TAG'${NC}"
echo -e "${YELLOW}Both release commits exist locally but no tag was created. Undo with:${NC}"
echo -e "${YELLOW}  git reset --hard HEAD~2${NC}"
exit 1
fi
echo -e "${GREEN}Successfully created tag '${YELLOW}${NEW_TAG}${GREEN}'${NC}"

# Pushed together and atomically: either the two release commits and the tag
# land on the remote, or none of them does, so there is no window with some but not all.
echo -e "${BLUE}Pushing ${CURRENT_BRANCH} and the tag to remote...${NC}"
if ! git push --atomic origin "$CURRENT_BRANCH" "$NEW_TAG"; then
echo -e "${RED}Error: failed to push '$CURRENT_BRANCH' and '$NEW_TAG'${NC}"
echo -e "${YELLOW}The release commits and the tag exist locally but were not pushed.${NC}"
echo -e "${YELLOW}Retry with:${NC}"
echo -e "${YELLOW}  git push --atomic origin $CURRENT_BRANCH $NEW_TAG${NC}"
echo -e "${YELLOW}Or undo them locally:${NC}"
echo -e "${YELLOW}  git tag -d $NEW_TAG && git reset --hard HEAD~2${NC}"
exit 1
fi
echo -e "${GREEN}Successfully pushed ${CURRENT_BRANCH} and tag '${YELLOW}${NEW_TAG}${GREEN}' to remote${NC}"

# Point at the workflow run, deriving the web URL from the origin remote
ORIGIN_URL=$(git remote get-url origin)
REPO_SLUG=$(printf '%s' "$ORIGIN_URL" | sed -E 's|^git@[^:]+:||; s|^https?://[^/]+/||; s|\.git$||')
if [ -n "$REPO_SLUG" ]; then
echo -e "${BLUE}Watch the release build:${NC}"
echo -e "  https://github.com/$REPO_SLUG/actions"
echo -e "${BLUE}Once it finishes, the APK will be at:${NC}"
echo -e "  https://github.com/$REPO_SLUG/releases/tag/$NEW_TAG"
fi
echo -e "${BLUE}Release tag creation completed successfully${NC}"
