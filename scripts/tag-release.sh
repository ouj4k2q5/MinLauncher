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
 echo -e "${YELLOW}Do you want to continue? (y/N)${NC}"
 read -n 1 -r branch_answer_raw
 echo
 branch_answer=$(printf "%s" "$branch_answer_raw" | tr '[:upper:]' '[:lower:]')
 if [[ ! $branch_answer =~ ^[Yy]$ ]]; then
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

# Find the latest release tag
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

GIT_USER=$(git config user.name || echo "unknown")
GIT_EMAIL=$(git config user.email || echo "unknown")
read -r COMMIT_SHA COMMIT_SUBJECT < <(git log -1 --format='%h %s')
TAG_MESSAGE="Release $NEW_TAG

versionName: $NEW_VERSION
versionCode: $NEW_VERSION_CODE
commit:      $COMMIT_SHA
branch:      $CURRENT_BRANCH
tagged by:   $GIT_USER <$GIT_EMAIL> at $(date '+%Y-%m-%d %H:%M:%S %z')"

# Summary
echo ""
echo -e "${GREEN}Tag:${NC}         ${YELLOW}${NEW_TAG}${NC}"
echo -e "${GREEN}versionName:${NC} $NEW_VERSION"
echo -e "${GREEN}versionCode:${NC} $NEW_VERSION_CODE"
echo -e "${GREEN}Commit:${NC}      $COMMIT_SHA ($COMMIT_SUBJECT)"
echo -e "${GREEN}Branch:${NC}      $CURRENT_BRANCH"
echo -e "${GREEN}Artifact:${NC}    MinLauncher-$NEW_VERSION.apk"
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
echo -e "${RED}Proceed? (y/N)${NC}"
read -n 1 -r answer_raw
echo
answer=$(printf "%s" "$answer_raw" | tr '[:upper:]' '[:lower:]')
if [[ ! $answer =~ ^[Yy]$ ]]; then
 echo -e "${RED}Aborting.${NC}"
 exit 1
fi

echo -e "${GREEN}Creating tag ${YELLOW}${NEW_TAG}${NC}"
if ! git tag -a "$NEW_TAG" -m "$TAG_MESSAGE"; then
 echo -e "${RED}Error: failed to create tag '$NEW_TAG'${NC}"
 exit 1
fi
echo -e "${GREEN}Successfully created tag '${YELLOW}${NEW_TAG}${GREEN}'${NC}"

echo -e "${BLUE}Pushing tag to remote...${NC}"
if ! git push origin "$NEW_TAG"; then
 echo -e "${RED}Error: failed to push tag '$NEW_TAG'${NC}"
 echo -e "${YELLOW}The tag exists locally but was not pushed. Retry with:${NC}"
 echo -e "${YELLOW}  git push origin $NEW_TAG${NC}"
 echo -e "${YELLOW}Or remove it locally:${NC}"
 echo -e "${YELLOW}  git tag -d $NEW_TAG${NC}"
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
echo -e "${BLUE}Release tag creation completed successfully${NC}"
