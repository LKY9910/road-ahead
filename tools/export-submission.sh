#!/usr/bin/env sh
# Copies the committed tree (minus export-ignore paths) into the submission repo,
# then fails if the author's name/email or an API key ended up in a text file.
# The APK is binary and is skipped on purpose: it carries the keys by design.
set -eu
dest=$(cd "${1:?usage: tools/export-submission.sh <submission-repo-dir>}" && pwd)
cd "$(dirname "$0")/.."
[ -d "$dest/.git" ] || { echo "not a git repo: $dest" >&2; exit 1; }
[ "$dest" != "$(pwd)" ] || { echo "refusing to export into this repo" >&2; exit 1; }

find "$dest" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
git archive HEAD | tar -x -C "$dest"

pats=$(mktemp)
trap 'rm -f "$pats"' EXIT
{ git config user.name; git config user.email; [ -f api ] && sed -E "s/^[^=]*= *'([^']*)'.*/\1/" api; } | grep . > "$pats"
if grep -rIlF -f "$pats" --exclude-dir=.git "$dest"; then
  echo "personal info or key found in the files above" >&2
  exit 1
fi
echo "exported $(git rev-parse --short HEAD) to $dest"
