#!/usr/bin/env bash
# Runs the bundled lukoa-tavern.sh against a throwaway SillyTavern-shaped git repository, so the
# update / rollback contract with the App (launcher-managed files, keep|discard policy, stash
# naming, upload-limit re-apply) can be verified on a PC or in CI instead of only on a phone.
#
# Usage:
#   bash tools/simulate-tavern-script.sh [--keep] [path/to/lukoa-tavern.sh]
#
# Needs bash, git, node, npm and (optionally) curl. On Windows run it from Git Bash.
# --keep leaves the sandbox directory behind for inspection; it is always kept when a check fails.

set -u

KEEP_SANDBOX=0
SCRIPT=""
for arg in "$@"; do
  case "$arg" in
    --keep) KEEP_SANDBOX=1 ;;
    -h|--help)
      sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) SCRIPT="$arg" ;;
  esac
done

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="${SCRIPT:-$REPO_ROOT/app/src/main/assets/lukoa-tavern.sh}"
[ -f "$SCRIPT" ] || { printf "script not found: %s\n" "$SCRIPT" >&2; exit 2; }
for tool in git node npm; do
  command -v "$tool" >/dev/null 2>&1 || { printf "missing required tool: %s\n" "$tool" >&2; exit 2; }
done

SANDBOX="$(mktemp -d "${TMPDIR:-/tmp}/lukoa-sim.XXXXXX")"
OUT="$SANDBOX/out"
mkdir -p "$OUT"

# Isolate the launcher script and every git call from the developer's real environment.
export LUKOA_HOME="$SANDBOX/home"
export LUKOA_STATE_DIR="$SANDBOX/state"
export LUKOA_CONFIG_FILE="$SANDBOX/no-config.env"
export LUKOA_TAVERN_PORT="${LUKOA_SIM_PORT:-18999}"
export GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL="$SANDBOX/gitconfig"
cat > "$GIT_CONFIG_GLOBAL" <<'EOF'
[user]
	name = lukoa-sim
	email = lukoa-sim@example.invalid
[core]
	autocrlf = false
[commit]
	gpgsign = false
[init]
	defaultBranch = release
[advice]
	detachedHead = false
EOF
mkdir -p "$LUKOA_HOME"

ORIGIN="$SANDBOX/origin.git"
SEED="$SANDBOX/seed"
TAVERN="$LUKOA_HOME/SillyTavern"
UPLOAD_LIMIT_STATE="$LUKOA_STATE_DIR/profiles/main/upload-limit.tsv"

PASS=0
FAIL=0
STEP=""
FAILED_STEPS=""

pass() { PASS=$((PASS + 1)); printf "  PASS  %s\n" "$1"; }
fail() {
  FAIL=$((FAIL + 1))
  printf "  FAIL  %s\n" "$1"
  case " $FAILED_STEPS " in *" $STEP "*) ;; *) FAILED_STEPS="$FAILED_STEPS $STEP" ;; esac
}
check() { if eval "$2"; then pass "$1"; else fail "$1"; fi; }
run() {
  STEP="$1"
  shift
  printf "\n### %s\n### $ lukoa-tavern.sh %s\n" "$STEP" "$*"
  sh "$SCRIPT" "$@" > "$OUT/$STEP.txt" 2>&1
  LAST_EXIT=$?
  printf "### exit=%s\n" "$LAST_EXIT"
}
grep_out() { grep -q -- "$1" "$OUT/$STEP.txt"; }
tavern_git() { git -C "$TAVERN" "$@"; }
tavern_tag() { tavern_git describe --tags --abbrev=0; }
field_size() { sed -n 's/.*fieldSize: \([0-9]*\).*/\1/p' "$TAVERN/src/server-main.js"; }
stash_count() { tavern_git stash list | wc -l | tr -d ' '; }
dirty_files() { tavern_git status --porcelain --untracked-files=no; }
reported_stash() { sed -n 's/^localChanges\.stash=//p' "$OUT/$STEP.txt" | head -n 1; }

finish() {
  printf "\n==== %d passed, %d failed ====\n" "$PASS" "$FAIL"
  if [ "$FAIL" -ne 0 ]; then
    for step in $FAILED_STEPS; do
      printf "\n----- output of %s -----\n" "$step"
      cat "$OUT/$step.txt"
    done
  fi
  if [ "$FAIL" -ne 0 ] || [ "$KEEP_SANDBOX" -eq 1 ]; then
    printf "\nsandbox kept at: %s\n" "$SANDBOX"
  else
    rm -rf "$SANDBOX"
  fi
  [ "$FAIL" -eq 0 ]
}

# ------------------------------------------------------------------ fixture
# origin: 1.13.0 -> 1.14.0 on branch "release", both tagged. tavern: clone detached at 1.13.0.
git init -q "$SEED"
mkdir -p "$SEED/src" "$SEED/public" "$SEED/default"
printf '{ "name": "sillytavern", "version": "1.13.0", "private": true }\n' > "$SEED/package.json"
cat > "$SEED/package-lock.json" <<'EOF'
{
  "name": "sillytavern",
  "version": "1.13.0",
  "lockfileVersion": 3,
  "requires": true,
  "packages": {
    "": { "name": "sillytavern", "version": "1.13.0" }
  }
}
EOF
cat > "$SEED/src/server-main.js" <<'EOF'
// fake SillyTavern upload middleware; the launcher's upload-limit tool patches fieldSize here
app.use(multer({ dest: UPLOADS_DIRECTORY, limits: { fieldSize: 200 * 1024 * 1024 } }).single('avatar'));
EOF
printf '<html>original 1.13</html>\n' > "$SEED/public/index.html"
printf '#!/bin/sh\nnode server.js\n' > "$SEED/start.sh"
printf 'console.log("fake");\n' > "$SEED/server.js"
printf 'dataRoot: ./data\n' > "$SEED/default/config.yaml"
git -C "$SEED" add -A
git -C "$SEED" commit -q -m "1.13.0"
git -C "$SEED" tag 1.13.0
sed -i 's/1\.13\.0/1.14.0/g' "$SEED/package.json" "$SEED/package-lock.json"
printf '<html>upstream 1.14</html>\n' > "$SEED/public/index.html"
git -C "$SEED" commit -q -am "1.14.0"
git -C "$SEED" tag 1.14.0
git clone -q --bare "$SEED" "$ORIGIN"
git clone -q "$ORIGIN" "$TAVERN"
tavern_git checkout -q 1.13.0
printf "script:  %s\nsandbox: %s\ntavern:  %s @ %s\n" "$SCRIPT" "$SANDBOX" "$TAVERN" "$(tavern_tag)"

# ------------------------------------------------------------------ 0. launcher-managed upload limit
run 00-upload-limit-set upload-limit-set 1024
check "upload limit patch applied (fieldSize=1024)" '[ "$LAST_EXIT" -eq 0 ] && [ "$(field_size)" = "1024" ]'

run 01-version version
check "version reports git.localChanges=1 listing only the managed file" \
  'grep_out "git.localChanges=1" && grep_out " M src/server-main.js" && ! grep_out "index.html"'

# ------------------------------------------------------------------ C. managed-only changes + keep policy -> must not block
sed -i 's/"lockfileVersion": 3/"lockfileVersion": 2/' "$TAVERN/package-lock.json"
check "fixture: upload patch and package-lock drift are both dirty" '[ "$(dirty_files | wc -l | tr -d " ")" = "2" ]'
run 10-update-managed-only-keep update 1.14.0 "$ORIGIN" keep
check "C: exit 0 although only launcher-managed files were dirty" '[ "$LAST_EXIT" -eq 0 ]'
check "C: HEAD moved to 1.14.0" '[ "$(tavern_tag)" = "1.14.0" ]'
check "C: upload limit re-applied after update" 'grep_out "uploadLimit.updateAction=reapplied" && [ "$(field_size)" = "1024" ]'
check "C: no stash created" '! grep_out "localChanges.stash=" && [ "$(stash_count)" = "0" ]'
check "C: only the managed patch remains dirty" '[ "$(dirty_files)" = " M src/server-main.js" ]'

run 11-rollback-managed-only-keep rollback 1.13.0 "$ORIGIN" keep
check "C: rollback with keep succeeded" '[ "$LAST_EXIT" -eq 0 ] && [ "$(tavern_tag)" = "1.13.0" ]'
check "C: upload limit re-applied after rollback" 'grep_out "uploadLimit.rollbackAction=reapplied" && [ "$(field_size)" = "1024" ]'

# ------------------------------------------------------------------ A. user edit + keep policy -> block, lose nothing
printf '<html>my own hack</html>\n' > "$TAVERN/public/index.html"
run 20-update-user-edit-keep update 1.14.0 "$ORIGIN" keep
check "A: blocked with exit 78" '[ "$LAST_EXIT" -eq 78 ]'
check "A: message says blocked until discarded and lists the user file" \
  'grep_out "blocked until they are discarded" && grep_out " M public/index.html"'
check "A: managed file is not reported as a local change" '! grep_out " M src/server-main.js"'
check "A: HEAD untouched" '[ "$(tavern_tag)" = "1.13.0" ]'
check "A: user edit still in place" 'grep -q "my own hack" "$TAVERN/public/index.html"'
check "A: upload limit patch restored after the block" '[ "$(field_size)" = "1024" ]'
check "A: no stash created" '[ "$(stash_count)" = "0" ]'

# ------------------------------------------------------------------ B. user edit + discard policy -> stash, update, re-apply
run 30-update-user-edit-discard update 1.14.0 "$ORIGIN" discard
STASH1="$(reported_stash)"
check "B: exit 0" '[ "$LAST_EXIT" -eq 0 ]'
check "B: result block reports localChanges.stash=lukoa-before-update-* (got: ${STASH1:-none})" \
  'case "$STASH1" in lukoa-before-update-*) true ;; *) false ;; esac'
check "B: exactly one stash exists with that name" '[ "$(stash_count)" = "1" ] && tavern_git stash list | grep -q -- "$STASH1"'
check "B: stash holds only the user file, not the managed patch" \
  '[ "$(tavern_git stash show --name-only "stash@{0}" | tr -d "\r")" = "public/index.html" ]'
check "B: stash keeps the user content" 'tavern_git stash show -p "stash@{0}" | grep -q "my own hack"'
check "B: HEAD is 1.14.0 with upstream index.html" '[ "$(tavern_tag)" = "1.14.0" ] && grep -q "upstream 1.14" "$TAVERN/public/index.html"'
check "B: result block is complete for the App parser" 'grep_out "exitCode=0" && grep_out "npmExitCode=0" && grep_out "==== end SillyTavern update ===="'
check "B: upload limit re-applied on the new version" 'grep_out "uploadLimit.updateAction=reapplied" && [ "$(field_size)" = "1024" ]'
check "B: only the managed patch remains dirty" '[ "$(dirty_files)" = " M src/server-main.js" ]'
check "B: upload limit state records the 1.14.0 commit" 'grep -q "$(tavern_git rev-parse --short "1.14.0^{commit}")" "$UPLOAD_LIMIT_STATE"'

# ------------------------------------------------------------------ D. rollback + discard -> second stash
printf '#!/bin/sh\nnode --inspect server.js\n' > "$TAVERN/start.sh"
run 40-rollback-user-edit-discard rollback 1.13.0 "$ORIGIN" discard
STASH2="$(reported_stash)"
check "D: exit 0" '[ "$LAST_EXIT" -eq 0 ]'
check "D: stash named lukoa-before-rollback-* (got: ${STASH2:-none})" 'case "$STASH2" in lukoa-before-rollback-*) true ;; *) false ;; esac'
check "D: two stashes, newest first" '[ "$(stash_count)" = "2" ] && tavern_git stash list | head -n 1 | grep -q -- "$STASH2"'
check "D: HEAD back at 1.13.0" '[ "$(tavern_tag)" = "1.13.0" ]'
check "D: upload limit re-applied after rollback" 'grep_out "uploadLimit.rollbackAction=reapplied" && [ "$(field_size)" = "1024" ]'

# ------------------------------------------------------------------ E. recovery path the App tells the user about
STEP=50-recover-with-stash-pop
tavern_git stash pop -q > "$OUT/$STEP.txt" 2>&1
check "E: git stash pop restores the start.sh edit" 'grep -q -- "--inspect" "$TAVERN/start.sh"'
tavern_git stash pop -q >> "$OUT/$STEP.txt" 2>&1
check "E: second pop restores index.html on 1.13.0 without conflict" \
  'grep -q "my own hack" "$TAVERN/public/index.html" && [ "$(stash_count)" = "0" ]'
tavern_git checkout -q -- start.sh public/index.html

# ------------------------------------------------------------------ F. user edited the managed file itself -> safety refusal
printf '// user note\n' >> "$TAVERN/src/server-main.js"
run 60-update-managed-file-edited-discard update 1.14.0 "$ORIGIN" discard
check "F: refused with exit 73 because the patch can no longer be lifted safely" '[ "$LAST_EXIT" -eq 73 ]'
check "F: explains the upload limit could not be removed" 'grep_out "Managed upload limit could not be safely removed before update"'
check "F: HEAD untouched and nothing stashed" '[ "$(tavern_tag)" = "1.13.0" ] && [ "$(stash_count)" = "0" ]'
check "F: user note still present" 'grep -q "user note" "$TAVERN/src/server-main.js"'

finish
