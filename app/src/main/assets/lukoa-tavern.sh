#!/data/data/com.termux/files/usr/bin/sh

set -u

APP_NAME="lukoa-launcher"
HOME_DIR="${LUKOA_HOME:-${HOME:-/data/data/com.termux/files/home}}"
STATE_DIR="${LUKOA_STATE_DIR:-$HOME_DIR/.local/state/$APP_NAME}"
CONFIG_FILE="${LUKOA_CONFIG_FILE:-$HOME_DIR/.config/$APP_NAME/config.env}"
DEFAULT_TAVERN_DIR="$HOME_DIR/SillyTavern"
TAVERN_PORT="${TAVERN_PORT:-8000}"
PID_FILE="$STATE_DIR/pid"
STATUS_FILE="$STATE_DIR/status.json"
COMMAND_FILE="$STATE_DIR/last-command.json"
PROOF_FILE="$STATE_DIR/launcher-proof.txt"
LOG_FILE="$STATE_DIR/tavern.log"
LOG_SYNC_CURSOR_FILE="$STATE_DIR/app-log.cursor"
ROLLBACK_FILE="$STATE_DIR/last-tavern-update-commit"
BACKUP_LIBRARY_RELATIVE_DIR="${LUKOA_BACKUP_LIBRARY_RELATIVE_DIR:-lukoa/backups}"
MANUAL_BACKUP_LIBRARY_RELATIVE_DIR="${LUKOA_MANUAL_BACKUP_LIBRARY_RELATIVE_DIR:-$BACKUP_LIBRARY_RELATIVE_DIR/sd}"
AUTO_BACKUP_LIBRARY_RELATIVE_DIR="${LUKOA_AUTO_BACKUP_LIBRARY_RELATIVE_DIR:-$BACKUP_LIBRARY_RELATIVE_DIR/zd}"

mkdir -p "$STATE_DIR"

if [ -f "$CONFIG_FILE" ]; then
  # shellcheck disable=SC1090
  . "$CONFIG_FILE"
fi

if [ -n "${LUKOA_TAVERN_DIR:-}" ]; then
  TAVERN_DIR="$LUKOA_TAVERN_DIR"
fi

OFFICIAL_REPO="${LUKOA_OFFICIAL_REPO:-https://github.com/SillyTavern/SillyTavern.git}"
NPM_REGISTRY="${LUKOA_NPM_REGISTRY:-${NPM_CONFIG_REGISTRY:-}}"
if [ -n "$NPM_REGISTRY" ]; then
  export npm_config_registry="$NPM_REGISTRY"
  export NPM_CONFIG_REGISTRY="$NPM_REGISTRY"
fi

case "$TAVERN_PORT" in
  ''|*[!0-9]*)
    TAVERN_PORT=8000
    ;;
esac

if [ "$TAVERN_PORT" -lt 1 ] || [ "$TAVERN_PORT" -gt 65535 ]; then
  TAVERN_PORT=8000
fi

expand_launcher_path() {
  path="$1"
  if [ -z "$path" ]; then
    printf "%s" "$path"
    return 0
  fi
  if [ "$path" = "~" ] || [ "$path" = "\$HOME" ]; then
    printf "%s" "$HOME_DIR"
    return 0
  fi
  if [ "${path#~/}" != "$path" ]; then
    printf "%s/%s" "$HOME_DIR" "${path#~/}"
    return 0
  fi
  if [ "${path#\$HOME/}" != "$path" ]; then
    printf "%s/%s" "$HOME_DIR" "${path#\$HOME/}"
    return 0
  fi
  printf "%s" "$path"
}

if [ -z "${TAVERN_DIR:-}" ]; then
  TAVERN_DIR="$DEFAULT_TAVERN_DIR"
fi
TAVERN_DIR="$(expand_launcher_path "$TAVERN_DIR")"

looks_like_tavern_dir() {
  dir="$1"
  [ -n "$dir" ] || return 1
  [ -d "$dir" ] || return 1
  [ -f "$dir/package.json" ] || return 1
  if [ -f "$dir/start.sh" ] || [ -f "$dir/server.js" ]; then
    return 0
  fi
  return 1
}

append_tavern_candidate() {
  candidate="$1"
  [ -n "$candidate" ] || return 0
  case "$candidate" in
    "$TAVERN_DIR"|"$DEFAULT_TAVERN_DIR") ;;
  esac
  if [ -z "${TAVERN_CANDIDATE_LIST:-}" ]; then
    TAVERN_CANDIDATE_LIST="$candidate"
  else
    TAVERN_CANDIDATE_LIST="$TAVERN_CANDIDATE_LIST
$candidate"
  fi
  TAVERN_CANDIDATE_COUNT=$((TAVERN_CANDIDATE_COUNT + 1))
  if [ -z "${TAVERN_CANDIDATE_FIRST:-}" ]; then
    TAVERN_CANDIDATE_FIRST="$candidate"
  fi
}

collect_tavern_dir_candidates() {
  TAVERN_CANDIDATE_LIST=""
  TAVERN_CANDIDATE_COUNT=0
  TAVERN_CANDIDATE_FIRST=""
  DISCOVERED_TAVERN_DIR=""

  if looks_like_tavern_dir "$TAVERN_DIR"; then
    append_tavern_candidate "$TAVERN_DIR"
  fi
  if [ "$DEFAULT_TAVERN_DIR" != "$TAVERN_DIR" ] && looks_like_tavern_dir "$DEFAULT_TAVERN_DIR"; then
    append_tavern_candidate "$DEFAULT_TAVERN_DIR"
  fi
  for candidate in "$HOME_DIR"/*; do
    [ -d "$candidate" ] || continue
    [ "$candidate" != "$TAVERN_DIR" ] || continue
    [ "$candidate" != "$DEFAULT_TAVERN_DIR" ] || continue
    looks_like_tavern_dir "$candidate" || continue
    append_tavern_candidate "$candidate"
  done
}

discover_tavern_dir() {
  collect_tavern_dir_candidates
  if [ "${TAVERN_CANDIDATE_COUNT:-0}" -eq 1 ] && [ -n "${TAVERN_CANDIDATE_FIRST:-}" ]; then
    DISCOVERED_TAVERN_DIR="$TAVERN_CANDIDATE_FIRST"
    return 0
  fi
  return 1
}

adopt_detected_tavern_dir() {
  discover_tavern_dir || return 1
  [ -n "${DISCOVERED_TAVERN_DIR:-}" ] || return 1
  TAVERN_DIR="$DISCOVERED_TAVERN_DIR"
  return 0
}

emit_tavern_dir_candidates() {
  [ "${TAVERN_CANDIDATE_COUNT:-0}" -gt 1 ] || return 0
  printf "\n==== SillyTavern directory candidates ====\n"
  printf "%s\n" "$TAVERN_CANDIDATE_LIST" | awk '{ printf "candidate.%d=%s\n", NR, $0 }'
  printf "==== end SillyTavern directory candidates ====\n"
}

missing_tavern_dir_exit_code() {
  if [ "${TAVERN_CANDIDATE_COUNT:-0}" -gt 1 ]; then
    printf "67"
  else
    printf "66"
  fi
}

write_tavern_dir_error() {
  code="$(missing_tavern_dir_exit_code)"
  if [ "${TAVERN_CANDIDATE_COUNT:-0}" -gt 1 ]; then
    write_status "error" "Multiple SillyTavern directories found; choose one in the launcher" false "$code"
  else
    write_status "error" "SillyTavern directory not found: $TAVERN_DIR" false "$code"
  fi
}

timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

json_escape() {
  printf "%s" "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

write_status() {
  status="$1"
  message="$2"
  running="$3"
  code="${4:-0}"
  now="$(timestamp)"
  safe_message="$(json_escape "$message")"
  safe_dir="$(json_escape "$TAVERN_DIR")"
  cat > "$STATUS_FILE" <<EOF
{
  "app": "$APP_NAME",
  "time": "$now",
  "status": "$status",
  "running": $running,
  "exitCode": $code,
  "message": "$safe_message",
  "tavernDir": "$safe_dir",
  "port": $TAVERN_PORT,
  "pidFile": "$PID_FILE",
  "logFile": "$LOG_FILE"
}
EOF
}

write_command() {
  command="$1"
  nonce="${2:-}"
  now="$(timestamp)"
  safe_command="$(json_escape "$command")"
  safe_nonce="$(json_escape "$nonce")"
  cat > "$COMMAND_FILE" <<EOF
{
  "time": "$now",
  "command": "$safe_command",
  "nonce": "$safe_nonce"
}
EOF
}

emit_status() {
  if [ "${LUKOA_QUIET:-0}" != "1" ]; then
    cat "$STATUS_FILE"
    emit_log_tail
  fi
}

emit_log_tail() {
  if [ ! -f "$LOG_FILE" ]; then
    return 0
  fi

  printf "\n==== SillyTavern recent log: %s ====\n" "$LOG_FILE"
  if command -v tail >/dev/null 2>&1; then
    tail -n "${LUKOA_LOG_LINES:-160}" "$LOG_FILE" 2>/dev/null || true
  else
    cat "$LOG_FILE" 2>/dev/null || true
  fi
  printf "\n==== end SillyTavern recent log ====\n"
}

file_size_bytes() {
  wc -c < "$1" 2>/dev/null | tr -d '[:space:]'
}

emit_live_log_delta() {
  if [ ! -f "$LOG_FILE" ]; then
    rm -f "$LOG_SYNC_CURSOR_FILE" 2>/dev/null || true
    printf "\nNo SillyTavern log file yet: %s\n" "$LOG_FILE"
    return 0
  fi

  size="$(file_size_bytes "$LOG_FILE")"
  case "$size" in ''|*[!0-9]*) size=0 ;; esac
  cursor="$(cat "$LOG_SYNC_CURSOR_FILE" 2>/dev/null || printf 0)"
  case "$cursor" in ''|*[!0-9]*) cursor=0 ;; esac
  if [ "$cursor" -gt "$size" ]; then
    cursor=0
  fi

  max_bytes="${LUKOA_LIVE_LOG_MAX_BYTES:-65536}"
  case "$max_bytes" in ''|*[!0-9]*) max_bytes=65536 ;; esac
  new_bytes="$((size - cursor))"
  start="$((cursor + 1))"
  omitted=0
  if [ "$new_bytes" -gt "$max_bytes" ]; then
    omitted="$((new_bytes - max_bytes))"
    start="$((size - max_bytes + 1))"
    new_bytes="$max_bytes"
  fi

  printf "liveLog.cursor.before=%s\n" "$cursor"
  printf "liveLog.cursor.after=%s\n" "$size"
  printf "liveLog.bytes=%s\n" "$new_bytes"
  printf "\n==== SillyTavern live log: %s ====\n" "$LOG_FILE"
  if [ "$omitted" -gt 0 ]; then
    printf "[lukoa] 前面 %s 字节日志太多，已从最新部分继续同步。\n" "$omitted"
  fi
  if [ "$new_bytes" -gt 0 ]; then
    tail -c +"$start" "$LOG_FILE" 2>/dev/null || true
  fi
  printf "\n==== end SillyTavern live log ====\n"
  printf "%s\n" "$size" > "$LOG_SYNC_CURSOR_FILE.$$"
  mv "$LOG_SYNC_CURSOR_FILE.$$" "$LOG_SYNC_CURSOR_FILE"
}

is_running() {
  if [ ! -f "$PID_FILE" ]; then
    return 1
  fi

  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  case "$pid" in
    ''|*[!0-9]*) return 1 ;;
  esac

  kill -0 "$pid" 2>/dev/null
}

process_cwd_matches() {
  pid="$1"
  cwd="$(readlink "/proc/$pid/cwd" 2>/dev/null || true)"
  [ "$cwd" = "$TAVERN_DIR" ] && return 0
  case "$cwd" in
    "$TAVERN_DIR"/*) return 0 ;;
  esac
  return 1
}

process_matches_tavern() {
  pid="$1"
  args=""
  if [ -r "/proc/$pid/cmdline" ]; then
    args="$(tr '\000' ' ' 2>/dev/null < "/proc/$pid/cmdline" || true)"
  fi
  case "$args" in
    *"$TAVERN_DIR"*) return 0 ;;
  esac
  if process_cwd_matches "$pid"; then
    case "$args" in
      *"server.js"*|*"start.sh"*) return 0 ;;
    esac
  fi
  return 1
}

candidate_pids() {
  current_pid="$$"
  parent_pid="${PPID:-}"
  if command -v pgrep >/dev/null 2>&1; then
    {
      pgrep -f "$TAVERN_DIR" 2>/dev/null || true
      pgrep -f "server\\.js" 2>/dev/null || true
      pgrep -f "start\\.sh" 2>/dev/null || true
    } | sort -u | while IFS= read -r pid; do
      [ -n "$pid" ] || continue
      [ "$pid" = "$current_pid" ] && continue
      [ -n "$parent_pid" ] && [ "$pid" = "$parent_pid" ] && continue
      process_matches_tavern "$pid" && printf "%s\n" "$pid"
    done
    return
  fi

  ps -A -o pid,args 2>/dev/null |
    grep -E "SillyTavern|server\\.js|start\\.sh" |
    grep -v grep |
    while IFS= read -r line; do
      pid="$(printf "%s\n" "$line" | awk '{print $1}')"
      [ -n "$pid" ] || continue
      [ "$pid" = "$current_pid" ] && continue
      [ -n "$parent_pid" ] && [ "$pid" = "$parent_pid" ] && continue
      process_matches_tavern "$pid" && printf "%s\n" "$pid"
    done || true
}

kill_candidate_pids() {
  pids="$(candidate_pids | tr '\n' ' ')"
  if [ -z "$pids" ]; then
    return 1
  fi

  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true
  sleep 1

  for pid in $pids; do
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
  return 0
}

http_probe_available() {
  command -v curl >/dev/null 2>&1
}

http_ok() {
  if ! http_probe_available; then
    return 2
  fi
  curl -fsS --max-time 3 "http://127.0.0.1:$TAVERN_PORT/" >/dev/null 2>&1
}

tavern_running_value() {
  if http_ok || is_running || [ -n "$(candidate_pids | head -n 1)" ]; then
    printf "true"
  else
    printf "false"
  fi
}

start_script_runner() {
  if command -v bash >/dev/null 2>&1; then
    printf "bash"
  else
    printf "sh"
  fi
}

tracked_changes() {
  git status --porcelain --untracked-files=no 2>/dev/null || true
}

only_package_lock_tracked_changes() {
  changes="$(tracked_changes)"
  [ -n "$changes" ] || return 1
  printf "%s\n" "$changes" | grep -Ev '^[ MADRCU?!]{2} package-lock\.json$' >/dev/null 2>&1 && return 1
  return 0
}

cleanup_install_generated_changes() {
  if [ ! -d .git ]; then
    return 0
  fi
  if only_package_lock_tracked_changes; then
    printf "[%s] cleanup generated package-lock.json change\n" "$(timestamp)" >> "$LOG_FILE"
    git checkout -- package-lock.json >> "$LOG_FILE" 2>&1 || true
  fi
  return 0
}

has_tracked_changes() {
  [ -n "$(tracked_changes)" ]
}

remote_default_branch() {
  branch="$(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
  if [ -z "$branch" ]; then
    branch="$(git remote show origin 2>/dev/null | sed -n 's/.*HEAD branch: //p' | head -n 1)"
  fi
  if [ -z "$branch" ]; then
    for candidate in release main master staging; do
      if git show-ref --verify --quiet "refs/remotes/origin/$candidate"; then
        branch="$candidate"
        break
      fi
    done
  fi
  printf "%s" "$branch"
}

valid_version_target() {
  target="$1"
  case "$target" in
    ''|-*|*::*|*..*|*//*|*/|*[!A-Za-z0-9._/@+-]*)
      return 1
      ;;
  esac
  return 0
}

checkout_requested_target() {
  target="$1"
  if [ -z "$target" ]; then
    return 64
  fi
  if ! valid_version_target "$target"; then
    return 64
  fi
  if git show-ref --verify --quiet "refs/remotes/origin/$target"; then
    if git show-ref --verify --quiet "refs/heads/$target"; then
      git checkout "$target" >> "$LOG_FILE" 2>&1 &&
        git merge --ff-only "origin/$target" >> "$LOG_FILE" 2>&1
    else
      git checkout -B "$target" "origin/$target" >> "$LOG_FILE" 2>&1
    fi
    return "$?"
  fi
  if git show-ref --verify --quiet "refs/tags/$target"; then
    git checkout "tags/$target" >> "$LOG_FILE" 2>&1
    return "$?"
  fi
  git checkout "$target" >> "$LOG_FILE" 2>&1
}

checkout_update_target() {
  requested="${1:-}"
  if [ -n "$requested" ]; then
    checkout_requested_target "$requested"
    return "$?"
  fi
  current_branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf HEAD)"
  upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
  if [ "$current_branch" != "HEAD" ] && [ -n "$upstream" ]; then
    git pull --ff-only >> "$LOG_FILE" 2>&1
    return "$?"
  fi

  target_branch="$current_branch"
  if [ "$target_branch" = "HEAD" ] || ! git show-ref --verify --quiet "refs/remotes/origin/$target_branch"; then
    target_branch="$(remote_default_branch)"
  fi
  if [ -z "$target_branch" ]; then
    return 80
  fi

  if git show-ref --verify --quiet "refs/heads/$target_branch"; then
    git checkout "$target_branch" >> "$LOG_FILE" 2>&1 &&
      git merge --ff-only "origin/$target_branch" >> "$LOG_FILE" 2>&1
  else
    git checkout -B "$target_branch" "origin/$target_branch" >> "$LOG_FILE" 2>&1
  fi
}

emit_official_versions() {
  if ! command -v git >/dev/null 2>&1; then
    return 69
  fi
  stable_file="$STATE_DIR/official-stable-tags.txt"
  heads_file="$STATE_DIR/official-heads.txt"
  git ls-remote --tags --refs "$OFFICIAL_REPO" 2>"$STATE_DIR/official-version-error.log" |
    awk '{ name=$2; sub(/^refs\/tags\//, "", name); if (name ~ /^v?[0-9]+(\.[0-9]+){1,3}$/) print name " " substr($1, 1, 12) }' |
    sort -Vr |
    head -n 5 > "$stable_file"
  stable_code="$?"
  git ls-remote --heads "$OFFICIAL_REPO" 2>>"$STATE_DIR/official-version-error.log" > "$heads_file"
  heads_code="$?"
  if [ "$stable_code" -ne 0 ] || [ "$heads_code" -ne 0 ]; then
    return 70
  fi

  printf "official.repo=%s\n" "$OFFICIAL_REPO"
  i=1
  while IFS=' ' read -r tag commit; do
    [ -n "$tag" ] || continue
    printf "stable.%s.name=%s\n" "$i" "$tag"
    printf "stable.%s.target=%s\n" "$i" "$tag"
    printf "stable.%s.commit=%s\n" "$i" "$commit"
    i=$((i + 1))
  done < "$stable_file"

  i=1
  for branch in staging stats-2.0 release main dev develop testing test preview next canary beta alpha; do
    [ "$i" -le 3 ] || break
    line="$(awk -v ref="refs/heads/$branch" '$2 == ref { print $1 " " $2; exit }' "$heads_file")"
    [ -n "$line" ] || continue
    commit="$(printf "%s" "$line" | awk '{print substr($1, 1, 12)}')"
    printf "test.%s.name=%s\n" "$i" "$branch"
    printf "test.%s.target=%s\n" "$i" "$branch"
    printf "test.%s.commit=%s\n" "$i" "$commit"
    i=$((i + 1))
  done
  return 0
}

install_node_dependencies() {
  if [ ! -f package.json ]; then
    return 0
  fi
  if ! command -v npm >/dev/null 2>&1; then
    return 69
  fi
  if [ -n "${NPM_REGISTRY:-}" ]; then
    printf "[%s] npm registry=%s\n" "$(timestamp)" "$NPM_REGISTRY" >> "$LOG_FILE"
  fi
  npm install --no-audit --no-fund >> "$LOG_FILE" 2>&1
  npm_code="$?"
  cleanup_install_generated_changes
  return "$npm_code"
}

sync_origin_repo() {
  if [ -n "${OFFICIAL_REPO:-}" ] && git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "$OFFICIAL_REPO" >> "$LOG_FILE" 2>&1 || true
  fi
}

ensure_tavern_mutation_ready() {
  action="$1"
  if http_ok || is_running || [ -n "$(candidate_pids | head -n 1)" ]; then
    write_status "error" "Please stop SillyTavern before $action" true 77
    emit_status
    return 77
  fi

  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
    emit_status
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi

  if ! command -v git >/dev/null 2>&1; then
    write_status "error" "git command not found in Termux" false 69
    emit_status
    return 69
  fi

  cd "$TAVERN_DIR" || {
    write_status "error" "failed to enter SillyTavern directory" false 74
    emit_status
    return 74
  }

  if [ ! -d .git ]; then
    write_status "error" "SillyTavern directory is not a git repository" false 65
    emit_status
    return 65
  fi

  cleanup_install_generated_changes
  if has_tracked_changes; then
    write_status "error" "SillyTavern has local tracked changes; update or rollback is blocked" false 78
    cat "$STATUS_FILE"
    printf "\n==== Git local changes ====\n"
    tracked_changes
    printf "==== end Git local changes ====\n"
    return 78
  fi
  sync_origin_repo
  return 0
}

emit_git_version_info() {
  printf "directory=%s\n" "$TAVERN_DIR"
  if [ -f package.json ]; then
    package_version="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' package.json | head -n 1)"
    [ -n "$package_version" ] && printf "package.version=%s\n" "$package_version"
  fi
  if command -v git >/dev/null 2>&1 && [ -d .git ]; then
    cleanup_install_generated_changes
    printf "git.branch=%s\n" "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf unknown)"
    printf "git.commit=%s\n" "$(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
    printf "git.describe=%s\n" "$(git describe --tags --always --dirty 2>/dev/null || printf unknown)"
    printf "git.remote=%s\n" "$(git remote get-url origin 2>/dev/null || printf unknown)"
    upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
    [ -n "$upstream" ] && printf "git.upstream=%s\n" "$upstream"
    changes="$(tracked_changes)"
    if [ -n "$changes" ]; then
      printf "git.localChanges=1\n"
    else
      printf "git.localChanges=0\n"
    fi
    if [ -s "$ROLLBACK_FILE" ]; then
      printf "rollback.target=%s\n" "$(cat "$ROLLBACK_FILE" 2>/dev/null || true)"
    fi
    printf "\n==== Git local changes ====\n"
    if [ -n "$changes" ]; then
      printf "%s\n" "$changes"
    else
      printf "clean\n"
    fi
  else
    printf "git=unavailable or not a git repository\n"
  fi
}

wait_for_http() {
  tries="${1:-10}"
  i=0
  while [ "$i" -lt "$tries" ]; do
    if http_ok; then
      return 0
    fi
    i=$((i + 1))
    sleep 1
  done
  return 1
}

open_browser_when_ready() {
  if ! command -v am >/dev/null 2>&1; then
    return 0
  fi

  (
    i=0
    while [ "$i" -lt 60 ]; do
      if http_ok; then
        am start -a android.intent.action.VIEW -d "http://127.0.0.1:$TAVERN_PORT" >/dev/null 2>&1 || true
        exit 0
      fi
      i=$((i + 1))
      sleep 1
    done
  ) &
}

cmd_selftest() {
  nonce="${1:-}"
  if [ -z "$nonce" ]; then
    write_status "error" "missing selftest nonce" false 64
    return 64
  fi

  now="$(timestamp)"
  {
    printf "timestamp=%s\n" "$now"
    printf "command=selftest\n"
    printf "nonce=%s\n" "$nonce"
    printf "uid=%s\n" "$(id -u 2>/dev/null || printf unknown)"
    printf "pwd=%s\n" "$(pwd)"
    printf "home=%s\n" "$HOME_DIR"
    printf "prefix=%s\n" "${PREFIX:-unknown}"
    printf "state_dir=%s\n" "$STATE_DIR"
  } > "$PROOF_FILE"

  write_command "selftest" "$nonce"
  write_status "selftest-ok" "Termux script executed selftest" false 0
  cat "$PROOF_FILE"
}

cmd_status() {
  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if http_ok; then
    write_status "running" "SillyTavern HTTP endpoint is responding" true 0
  elif is_running; then
    if http_probe_available; then
      write_status "unreachable" "SillyTavern process exists, but HTTP endpoint is not responding" true 75
    else
      write_status "running-unknown" "SillyTavern process exists; install curl for HTTP verification" true 0
    fi
  elif [ -n "$(candidate_pids | head -n 1)" ]; then
    if http_probe_available; then
      write_status "unreachable" "SillyTavern process exists, but HTTP endpoint is not responding" true 75
    else
      write_status "running-unknown" "SillyTavern process exists; install curl for HTTP verification" true 0
    fi
  elif [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
  else
    write_status "stopped" "SillyTavern process is not running" false 0
  fi
  emit_status
  emit_tavern_dir_candidates
}

cmd_log() {
  write_command "log"
  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if is_running || http_ok; then
    write_status "log" "SillyTavern recent log exported" true 0
  elif [ -n "$(candidate_pids | head -n 1)" ]; then
    write_status "log" "SillyTavern recent log exported; process exists but HTTP endpoint is not responding" true 75
  elif [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
  else
    write_status "log" "SillyTavern recent log exported; process is not running" false 0
  fi
  emit_status
  emit_tavern_dir_candidates
}

cmd_log_live() {
  write_command "log-live"
  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if is_running || http_ok; then
    write_status "log" "SillyTavern live log synced" true 0
  elif [ -n "$(candidate_pids | head -n 1)" ]; then
    write_status "log" "SillyTavern live log synced; process exists but HTTP endpoint is not responding" true 75
  elif [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
  else
    write_status "log" "SillyTavern live log synced; process is not running" false 0
  fi
  cat "$STATUS_FILE"
  emit_tavern_dir_candidates
  emit_live_log_delta
}

cmd_console() {
  write_command "console"
  if http_ok; then
    write_status "running" "SillyTavern is already running and HTTP endpoint is responding" true 0
    open_browser_when_ready
    emit_status
    return 0
  fi

  if is_running; then
    if http_probe_available; then
      write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
      emit_status
      return 75
    fi
    write_status "running-unknown" "SillyTavern process already exists; install curl for HTTP verification" true 0
    emit_status
    return 0
  fi

  if [ -n "$(candidate_pids | head -n 1)" ]; then
    if http_probe_available; then
      write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
      emit_status
      return 75
    fi
    write_status "running-unknown" "SillyTavern process already exists; install curl for HTTP verification" true 0
    emit_status
    return 0
  fi

  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
    emit_status
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi

  if ! command -v node >/dev/null 2>&1; then
    write_status "error" "node command not found in Termux" false 69
    emit_status
    return 69
  fi

  cd "$TAVERN_DIR" || {
    write_status "error" "failed to enter SillyTavern directory" false 74
    emit_status
    return 74
  }

  printf "\n[%s] ===== Lukoa launcher foreground session =====\n" "$(timestamp)" | tee -a "$LOG_FILE"
  printf "[%s] Termux foreground logging is enabled. Log file: %s\n" "$(timestamp)" "$LOG_FILE" | tee -a "$LOG_FILE"
  write_status "starting" "SillyTavern is starting in Termux foreground log mode" true 0
  printf "%s\n" "$$" > "$PID_FILE"
  open_browser_when_ready

  if [ -f "./start.sh" ]; then
    runner="$(start_script_runner)"
    printf "[%s] starting with %s ./start.sh in %s\n" "$(timestamp)" "$runner" "$TAVERN_DIR" | tee -a "$LOG_FILE"
    "$runner" ./start.sh 2>&1 | tee -a "$LOG_FILE"
    code="$?"
  elif [ -f "server.js" ]; then
    printf "[%s] starting with node server.js in %s\n" "$(timestamp)" "$TAVERN_DIR" | tee -a "$LOG_FILE"
    node server.js 2>&1 | tee -a "$LOG_FILE"
    code="$?"
  else
    write_status "error" "no start.sh or server.js found in SillyTavern directory" false 65
    emit_status
    rm -f "$PID_FILE"
    return 65
  fi

  rm -f "$PID_FILE"
  write_status "stopped" "SillyTavern foreground session exited" false "$code"
  emit_status
  return "$code"
}

cmd_start() {
  if is_running; then
    write_command "start"
    if http_ok; then
      write_status "running" "SillyTavern is already running and HTTP endpoint is responding" true 0
    elif ! http_probe_available; then
      write_status "running-unknown" "SillyTavern is already running; install curl for HTTP verification" true 0
    else
      write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
      emit_status
      return 75
    fi
    emit_status
    return 0
  fi

  if [ -n "$(candidate_pids | head -n 1)" ]; then
    write_command "start"
    if http_ok; then
      write_status "running" "SillyTavern is already running and HTTP endpoint is responding" true 0
    elif ! http_probe_available; then
      write_status "running-unknown" "SillyTavern is already running; install curl for HTTP verification" true 0
    else
      write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
      emit_status
      return 75
    fi
    emit_status
    return 0
  fi

  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if [ ! -d "$TAVERN_DIR" ]; then
    write_command "start"
    collect_tavern_dir_candidates
    write_tavern_dir_error
    emit_status
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi

  if ! command -v node >/dev/null 2>&1; then
    write_command "start"
    write_status "error" "node command not found in Termux" false 69
    emit_status
    return 69
  fi

  cd "$TAVERN_DIR" || {
    write_status "error" "failed to enter SillyTavern directory" false 74
    emit_status
    return 74
  }

  write_command "start"
  printf "\n[%s] ===== Lukoa launcher start session =====\n" "$(timestamp)" >> "$LOG_FILE"
  if [ -f "./start.sh" ]; then
    runner="$(start_script_runner)"
    printf "[%s] starting with %s ./start.sh in %s\n" "$(timestamp)" "$runner" "$TAVERN_DIR" >> "$LOG_FILE"
    nohup "$runner" ./start.sh >> "$LOG_FILE" 2>&1 &
  elif [ -f "server.js" ]; then
    printf "[%s] starting with node server.js in %s\n" "$(timestamp)" "$TAVERN_DIR" >> "$LOG_FILE"
    nohup node server.js >> "$LOG_FILE" 2>&1 &
  else
    write_status "error" "no start.sh or server.js found in SillyTavern directory" false 65
    emit_status
    return 65
  fi

  pid="$!"
  printf "%s\n" "$pid" > "$PID_FILE"
  if http_probe_available && wait_for_http 45; then
    write_status "running" "SillyTavern HTTP endpoint is responding" true 0
    emit_status
    return 0
  fi

  if is_running; then
    write_status "starting" "SillyTavern launch command accepted, but HTTP endpoint is not ready" true 75
    emit_status
    return 75
  fi

  write_status "error" "SillyTavern process exited immediately; check tavern.log" false 70
  emit_status
  return 70
}

cmd_stop() {
  write_command "stop"
  if ! is_running; then
    rm -f "$PID_FILE"
    if kill_candidate_pids; then
      if http_ok; then
        write_status "error" "SillyTavern HTTP endpoint is still responding after killing candidate processes" true 76
        emit_status
        return 76
      fi
      write_status "stopped" "SillyTavern stopped by killing candidate processes because pid file was missing" false 0
      emit_status
      return 0
    fi

    if http_ok; then
      if kill_candidate_pids; then
        if http_ok; then
          write_status "error" "SillyTavern HTTP endpoint is still responding after killing candidate processes" true 76
          emit_status
          return 76
        fi
        write_status "stopped" "SillyTavern stopped by killing candidate processes because pid file was missing" false 0
        emit_status
        return 0
      fi
      write_status "error" "SillyTavern HTTP endpoint is still responding, but pid file is missing and no candidate process was found" true 76
      emit_status
      return 76
    fi
    write_status "stopped" "SillyTavern was not running" false 0
    emit_status
    return 0
  fi

  pid="$(cat "$PID_FILE")"
  kill "$pid" 2>/dev/null || true
  sleep 1

  if kill -0 "$pid" 2>/dev/null; then
    kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$PID_FILE"
  if http_ok; then
    write_status "error" "SillyTavern process was killed, but HTTP endpoint is still responding" true 76
    emit_status
    return 76
  fi
  write_status "stopped" "SillyTavern stopped" false 0
  emit_status
}

cmd_official_versions() {
  write_command "official-versions"
  write_status "official-versions" "Official SillyTavern versions collected" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern official versions ====\n"
  emit_official_versions
  code="$?"
  if [ "$code" -ne 0 ]; then
    write_status "error" "Failed to read official SillyTavern versions" false "$code"
    cat "$STATUS_FILE"
    printf "\n==== official version error ====\n"
    cat "$STATE_DIR/official-version-error.log" 2>/dev/null || true
    printf "\n==== end official version error ====\n"
    return "$code"
  fi
  printf "==== end SillyTavern official versions ====\n"
}

cmd_install() {
  write_command "install"
  target="${1:-release}"
  if ! valid_version_target "$target"; then
    write_status "error" "Invalid SillyTavern install target" false 64
    emit_status
    return 64
  fi
  if http_ok || is_running || [ -n "$(candidate_pids | head -n 1)" ]; then
    write_status "error" "Please stop SillyTavern before installing" true 77
    emit_status
    return 77
  fi
  need=""
  command -v git >/dev/null 2>&1 || need="$need git"
  if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
    need="$need nodejs"
  fi
  command -v curl >/dev/null 2>&1 || need="$need curl"
  if [ -n "$need" ]; then
    printf "step=pkg install%s\n" "$need"
    if command -v pkg >/dev/null 2>&1; then
      pkg install -y $need
      dependency_code="$?"
    elif command -v apt >/dev/null 2>&1; then
      apt update && apt install -y $need
      dependency_code="$?"
    else
      dependency_code=69
    fi
    printf "installDependencyExitCode=%s\n" "$dependency_code"
    if [ "$dependency_code" -ne 0 ]; then
      write_status "error" "Termux dependency install failed before SillyTavern install" false "$dependency_code"
      emit_status
      return "$dependency_code"
    fi
  fi
  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  collect_tavern_dir_candidates
  if [ "${TAVERN_CANDIDATE_COUNT:-0}" -gt 1 ] && [ ! -e "$TAVERN_DIR" ]; then
    write_tavern_dir_error
    cat "$STATUS_FILE"
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi
  if [ -e "$TAVERN_DIR" ] && [ -n "$(ls -A "$TAVERN_DIR" 2>/dev/null || true)" ]; then
    write_status "error" "SillyTavern directory already exists and is not empty" false 73
    cat "$STATUS_FILE"
    printf "\ndirectory=%s\n" "$TAVERN_DIR"
    return 73
  fi

  parent="$(dirname "$TAVERN_DIR")"
  mkdir -p "$parent"
  printf "\n[%s] ===== Lukoa launcher tavern install =====\n" "$(timestamp)" >> "$LOG_FILE"
  printf "[%s] target=%s repo=%s\n" "$(timestamp)" "$target" "$OFFICIAL_REPO" >> "$LOG_FILE"
  git clone -b "$target" "$OFFICIAL_REPO" "$TAVERN_DIR" >> "$LOG_FILE" 2>&1
  clone_code="$?"
  if [ "$clone_code" -ne 0 ]; then
    write_status "error" "git clone failed; check tavern.log" false "$clone_code"
    cat "$STATUS_FILE"
    printf "\n==== Recent install log ====\n"
    tail -n 120 "$LOG_FILE" 2>/dev/null || true
    printf "==== end SillyTavern install ====\n"
    return "$clone_code"
  fi

  cd "$TAVERN_DIR" || {
    write_status "error" "failed to enter SillyTavern directory" false 74
    emit_status
    return 74
  }
  npm_code=0
  install_node_dependencies
  npm_code="$?"
  if [ "$npm_code" -eq 0 ]; then
    write_status "installed" "SillyTavern installed successfully" false 0
    code=0
  else
    write_status "error" "npm install failed; check tavern.log" false "$npm_code"
    code="$npm_code"
  fi

  cat "$STATUS_FILE"
  printf "\n==== SillyTavern install ====\n"
  printf "directory=%s\n" "$TAVERN_DIR"
  printf "target=%s\n" "$target"
  printf "exitCode=%s\n" "$code"
  printf "npmExitCode=%s\n" "$npm_code"
  printf "\n==== Current SillyTavern version ====\n"
  emit_git_version_info
  printf "\n==== Recent install log ====\n"
  tail -n 120 "$LOG_FILE" 2>/dev/null || true
  printf "==== end SillyTavern install ====\n"
  return "$code"
}

cmd_update() {
  write_command "update"
  requested_target="${1:-}"
  ensure_tavern_mutation_ready "updating source files"
  preflight_code="$?"
  if [ "$preflight_code" -ne 0 ]; then
    return "$preflight_code"
  fi
  if [ -z "$requested_target" ]; then
    write_status "error" "No SillyTavern update target selected" false 64
    emit_status
    return 64
  fi
  if ! valid_version_target "$requested_target"; then
    write_status "error" "Invalid SillyTavern update target" false 64
    emit_status
    return 64
  fi

  before_full="$(git rev-parse HEAD 2>/dev/null || printf unknown)"
  before="$(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
  printf "\n[%s] ===== Lukoa launcher tavern update =====\n" "$(timestamp)" >> "$LOG_FILE"
  printf "[%s] before=%s target=%s\n" "$(timestamp)" "$before_full" "$requested_target" >> "$LOG_FILE"

  git fetch --all --tags --prune >> "$LOG_FILE" 2>&1
  fetch_code="$?"
  if [ "$fetch_code" -ne 0 ]; then
    write_status "error" "git fetch failed; check tavern.log" false "$fetch_code"
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern update ====\n"
    printf "directory=%s\n" "$TAVERN_DIR"
    printf "before=%s\n" "$before"
    printf "after=%s\n" "$before"
    printf "exitCode=%s\n" "$fetch_code"
    printf "\n==== Recent update log ====\n"
    tail -n 100 "$LOG_FILE" 2>/dev/null || true
    printf "==== end SillyTavern update ====\n"
    return "$fetch_code"
  fi

  checkout_update_target "$requested_target"
  git_code="$?"
  after="$(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
  npm_code=0
  if [ "$git_code" -eq 0 ]; then
    printf "%s\n" "$before_full" > "$ROLLBACK_FILE"
    printf "[%s] rollback target saved: %s\n" "$(timestamp)" "$before_full" >> "$LOG_FILE"
    install_node_dependencies
    npm_code="$?"
  fi

  if [ "$git_code" -eq 0 ] && [ "$npm_code" -eq 0 ]; then
    write_status "updated" "SillyTavern source updated successfully" false 0
    code=0
  elif [ "$git_code" -eq 80 ]; then
    write_status "error" "Could not find a remote branch to update" false 80
    code=80
  elif [ "$git_code" -ne 0 ]; then
    write_status "error" "git update failed; check tavern.log" false "$git_code"
    code="$git_code"
  elif [ "$npm_code" -eq 69 ]; then
    write_status "error" "npm command not found in Termux" false 69
    code=69
  else
    write_status "error" "npm install failed; check tavern.log" false "$npm_code"
    code="$npm_code"
  fi

  cat "$STATUS_FILE"
  printf "\n==== SillyTavern update ====\n"
  printf "directory=%s\n" "$TAVERN_DIR"
  printf "target=%s\n" "$requested_target"
  printf "before=%s\n" "$before"
  printf "after=%s\n" "$after"
  printf "exitCode=%s\n" "$code"
  printf "npmExitCode=%s\n" "$npm_code"
  if [ -s "$ROLLBACK_FILE" ]; then
    printf "rollback.target=%s\n" "$(cat "$ROLLBACK_FILE" 2>/dev/null || true)"
  fi
  printf "\n==== Git status ====\n"
  git status --short 2>/dev/null || true
  printf "\n==== Recent update log ====\n"
  tail -n 120 "$LOG_FILE" 2>/dev/null || true
  printf "\n==== Current SillyTavern version ====\n"
  emit_git_version_info
  printf "==== end SillyTavern update ====\n"
  return "$code"
}

cmd_rollback() {
  write_command "rollback"
  requested_target="${1:-}"
  ensure_tavern_mutation_ready "rolling back source files"
  preflight_code="$?"
  if [ "$preflight_code" -ne 0 ]; then
    return "$preflight_code"
  fi
  if [ -z "$requested_target" ]; then
    write_status "error" "No SillyTavern rollback target selected" false 64
    emit_status
    return 64
  fi
  if ! valid_version_target "$requested_target"; then
    write_status "error" "Invalid SillyTavern rollback target" false 64
    emit_status
    return 64
  fi

  before_full="$(git rev-parse HEAD 2>/dev/null || printf unknown)"
  before="$(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
  printf "\n[%s] ===== Lukoa launcher tavern rollback =====\n" "$(timestamp)" >> "$LOG_FILE"
  printf "[%s] before=%s target=%s\n" "$(timestamp)" "$before_full" "$requested_target" >> "$LOG_FILE"

  git fetch --all --tags --prune >> "$LOG_FILE" 2>&1
  fetch_code="$?"
  if [ "$fetch_code" -ne 0 ]; then
    write_status "error" "git fetch failed before rollback; check tavern.log" false "$fetch_code"
    cat "$STATUS_FILE"
    printf "\n==== Recent rollback log ====\n"
    tail -n 120 "$LOG_FILE" 2>/dev/null || true
    printf "==== end SillyTavern rollback ====\n"
    return "$fetch_code"
  fi
  checkout_requested_target "$requested_target"
  git_code="$?"
  after="$(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
  npm_code=0
  if [ "$git_code" -eq 0 ]; then
    printf "%s\n" "$before_full" > "$ROLLBACK_FILE"
    install_node_dependencies
    npm_code="$?"
  fi

  if [ "$git_code" -eq 0 ] && [ "$npm_code" -eq 0 ]; then
    write_status "rolled-back" "SillyTavern source rolled back successfully" false 0
    code=0
  elif [ "$git_code" -ne 0 ]; then
    write_status "error" "git rollback failed; check tavern.log" false "$git_code"
    code="$git_code"
  elif [ "$npm_code" -eq 69 ]; then
    write_status "error" "npm command not found in Termux" false 69
    code=69
  else
    write_status "error" "npm install after rollback failed; check tavern.log" false "$npm_code"
    code="$npm_code"
  fi

  cat "$STATUS_FILE"
  printf "\n==== SillyTavern rollback ====\n"
  printf "directory=%s\n" "$TAVERN_DIR"
  printf "target=%s\n" "$requested_target"
  printf "before=%s\n" "$before"
  printf "after=%s\n" "$after"
  printf "exitCode=%s\n" "$code"
  printf "npmExitCode=%s\n" "$npm_code"
  if [ -s "$ROLLBACK_FILE" ]; then
    printf "rollback.target=%s\n" "$(cat "$ROLLBACK_FILE" 2>/dev/null || true)"
  fi
  printf "\n==== Recent rollback log ====\n"
  tail -n 120 "$LOG_FILE" 2>/dev/null || true
  printf "\n==== Current SillyTavern version ====\n"
  emit_git_version_info
  printf "==== end SillyTavern rollback ====\n"
  return "$code"
}

cmd_version() {
  write_command "version"
  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
    emit_status
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi

  cd "$TAVERN_DIR" || {
    write_status "error" "failed to enter SillyTavern directory" false 74
    emit_status
    return 74
  }

  write_status "version" "SillyTavern version information collected" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern version ====\n"
  emit_git_version_info
  printf "==== end SillyTavern version ====\n"
}

backup_relative_dir_for_kind() {
  kind="${1:-${BACKUP_KIND:-manual}}"
  case "$kind" in
    auto) printf "%s" "$AUTO_BACKUP_LIBRARY_RELATIVE_DIR" ;;
    *) printf "%s" "$MANUAL_BACKUP_LIBRARY_RELATIVE_DIR" ;;
  esac
}

backup_dir() {
  if [ -n "${LUKOA_BACKUP_DIR_OVERRIDE:-}" ]; then
    printf "%s" "$LUKOA_BACKUP_DIR_OVERRIDE"
    return 0
  fi
  relative_dir="$(backup_relative_dir_for_kind "${1:-${BACKUP_KIND:-manual}}")"
  downloads_root="$(shared_download_dir || true)"
  if [ -n "$downloads_root" ]; then
    printf "%s/%s" "$downloads_root" "$relative_dir"
  else
    case "${1:-${BACKUP_KIND:-manual}}" in
      auto) printf "%s/backups/zd" "$STATE_DIR" ;;
      *) printf "%s/backups/sd" "$STATE_DIR" ;;
    esac
  fi
}

backup_library_dirs() {
  printf "%s/%s\n" "$HOME_DIR/storage/downloads" "$MANUAL_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s/%s\n" "$HOME_DIR/storage/downloads" "$AUTO_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s/%s\n" "/storage/emulated/0/Download" "$MANUAL_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s/%s\n" "/storage/emulated/0/Download" "$AUTO_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s/%s\n" "/sdcard/Download" "$MANUAL_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s/%s\n" "/sdcard/Download" "$AUTO_BACKUP_LIBRARY_RELATIVE_DIR"
  printf "%s\n" "$STATE_DIR/backups/sd"
  printf "%s\n" "$STATE_DIR/backups/zd"
}

emit_backup_library_dirs() {
  index=1
  for library_dir in $(backup_library_dirs); do
    printf "allowed.backupDir%s=%s\n" "$index" "$library_dir"
    index=$((index + 1))
  done
}

shared_download_dir() {
  for candidate in "$HOME_DIR/storage/downloads" "/storage/emulated/0/Download" "/sdcard/Download"; do
    [ -n "$candidate" ] || continue
    if [ -d "$candidate" ] && [ -w "$candidate" ]; then
      printf "%s" "$candidate"
      return 0
    fi
  done

  if command -v termux-setup-storage >/dev/null 2>&1; then
    termux-setup-storage >/dev/null 2>&1 || true
    sleep 1
    for candidate in "$HOME_DIR/storage/downloads" "/storage/emulated/0/Download" "/sdcard/Download"; do
      [ -n "$candidate" ] || continue
      if [ -d "$candidate" ] && [ -w "$candidate" ]; then
        printf "%s" "$candidate"
        return 0
      fi
    done
  fi

  return 1
}

canonical_existing_path() {
  target="$1"
  if [ -d "$target" ]; then
    (cd "$target" 2>/dev/null && pwd -P) || printf "%s" "$target"
    return
  fi
  if [ -e "$target" ]; then
    target_dir="$(dirname "$target")"
    target_base="$(basename "$target")"
    (cd "$target_dir" 2>/dev/null && printf "%s/%s" "$(pwd -P)" "$target_base") || printf "%s" "$target"
    return
  fi
  target_dir="$(dirname "$target")"
  target_base="$(basename "$target")"
  (cd "$target_dir" 2>/dev/null && printf "%s/%s" "$(pwd -P)" "$target_base") || printf "%s" "$target"
}

path_is_inside() {
  child="$(canonical_existing_path "$1")"
  parent="$(canonical_existing_path "$2")"
  [ "$child" = "$parent" ] && return 0
  case "$child" in
    "$parent"/*) return 0 ;;
    *) return 1 ;;
  esac
}

backup_write_test() {
  test_dir="$1"
  test_file="$test_dir/.lukoa-write-test-$$"
  if (umask 077 && : > "$test_file") 2>/dev/null; then
    rm -f "$test_file"
    return 0
  fi
  rm -f "$test_file" 2>/dev/null || true
  return 1
}

read_config_data_root() {
  config_path="$TAVERN_DIR/config.yaml"
  [ -f "$config_path" ] || return 1
  raw="$(
    sed -n 's/^[[:space:]]*dataRoot[[:space:]]*:[[:space:]]*//p' "$config_path" 2>/dev/null |
      head -n 1 |
      tr -d '\r'
  )"
  [ -n "$raw" ] || return 1
  case "$raw" in
    \"*\"|\'*\') ;;
    *) raw="${raw%%#*}" ;;
  esac
  raw="$(printf "%s" "$raw" | sed "s/^[[:space:]]*//; s/[[:space:]]*$//; s/^\"//; s/\"$//; s/^'//; s/'$//")"
  [ -n "$raw" ] || return 1
  printf "%s" "$raw"
}

resolve_tavern_data_root() {
  configured="$(read_config_data_root || true)"
  if [ -z "$configured" ]; then
    configured="data"
  fi

  case "$configured" in
    /*)
      printf "%s" "$configured"
      ;;
    '$HOME')
      printf "%s" "$HOME_DIR"
      ;;
    '$HOME'/*)
      printf "%s/%s" "$HOME_DIR" "${configured#\$HOME/}"
      ;;
    '${HOME}')
      printf "%s" "$HOME_DIR"
      ;;
    '${HOME}'/*)
      printf "%s/%s" "$HOME_DIR" "$(printf "%s" "$configured" | sed 's#^\${HOME}/##')"
      ;;
    ~/*)
      printf "%s/%s" "$HOME_DIR" "${configured#~/}"
      ;;
    *)
      printf "%s/%s" "$TAVERN_DIR" "$configured"
      ;;
  esac
}

safe_remove_backup_temp_dir() {
  temp_dir="$1"
  case "$temp_dir" in
    "$STATE_DIR"/backup-manifest-*) rm -rf "$temp_dir" ;;
  esac
}

safe_remove_restore_temp_dir() {
  temp_dir="$1"
  case "$temp_dir" in
    "$STATE_DIR"/restore-staging-*) rm -rf "$temp_dir" ;;
  esac
}

archive_contains_entry() {
  entry="$1"
  list_file="$2"
  awk -v entry="$entry" '
    $0 == entry || $0 == entry "/" || index($0, entry "/") == 1 { found = 1 }
    END { exit found ? 0 : 1 }
  ' "$list_file"
}

safe_cleanup_auto_backups() {
  cleanup_dir="$1"
  keep_count="$2"
  [ -d "$cleanup_dir" ] || return 0
  case "$cleanup_dir" in
    ""|"/"|"$HOME_DIR"|"$TAVERN_DIR") return 73 ;;
  esac

  ls -1t "$cleanup_dir"/zd-*.tar.gz "$cleanup_dir"/sillytavern-auto-*.tar.gz 2>/dev/null |
    tail -n +"$((keep_count + 1))" |
    while IFS= read -r old_file; do
      [ -n "$old_file" ] || continue
      case "$old_file" in
        "$cleanup_dir"/zd-*.tar.gz|"$cleanup_dir"/sillytavern-auto-*.tar.gz)
          [ -f "$old_file" ] || continue
          rm -f "$old_file" && printf "%s\n" "$old_file"
          ;;
      esac
    done
}

safe_backup_label() {
  label="$1"
  printf "%s" "$label" |
    tr '\r\n\t' '---' |
    sed 's#[/\\:*?"<>|]#-#g; s/^[[:space:]-]*//; s/[[:space:]-]*$//; s/--*/-/g; s/^\.*//'
}

backup_filename_conflict_path() {
  target_name="$1"
  current_path="${2:-}"
  current_canon=""
  if [ -n "$current_path" ]; then
    current_canon="$(canonical_existing_path "$current_path")"
  fi
  for library_dir in $(backup_library_dirs); do
    candidate="$library_dir/$target_name"
    [ -e "$candidate" ] || continue
    candidate_canon="$(canonical_existing_path "$candidate")"
    if [ -n "$current_canon" ] && [ "$candidate_canon" = "$current_canon" ]; then
      continue
    fi
    printf "%s" "$candidate_canon"
    return 0
  done
  return 1
}

unique_library_backup_destination() {
  dest_dir="$1"
  source_name="$2"
  safe_name="$(safe_backup_label "${source_name%.tar.gz}")"
  [ -n "$safe_name" ] || safe_name="imported-backup"
  suffix=0
  while :; do
    if [ "$suffix" -eq 0 ]; then
      target_name="$safe_name.tar.gz"
    else
      target_name="$safe_name($suffix).tar.gz"
    fi
    dest="$dest_dir/$target_name"
    if [ ! -e "$dest" ] && ! backup_filename_conflict_path "$target_name" >/dev/null 2>&1; then
      printf "%s" "$dest"
      return 0
    fi
    suffix=$((suffix + 1))
  done
}

cmd_backup() {
  write_command "backup"
  BACKUP_KIND="${1:-manual}"
  AUTO_KEEP="${2:-5}"
  BACKUP_LABEL="${3:-}"
  case "$BACKUP_KIND" in
    auto) ;;
    *) BACKUP_KIND="manual" ;;
  esac
  case "$AUTO_KEEP" in
    ''|*[!0-9]*) AUTO_KEEP=5 ;;
  esac
  if [ "$AUTO_KEEP" -lt 1 ]; then
    AUTO_KEEP=1
  fi
  if [ "$AUTO_KEEP" -gt 50 ]; then
    AUTO_KEEP=50
  fi

  adopt_detected_tavern_dir >/dev/null 2>&1 || true
  if [ ! -d "$TAVERN_DIR" ]; then
    collect_tavern_dir_candidates
    write_tavern_dir_error
    emit_status
    emit_tavern_dir_candidates
    return "$(missing_tavern_dir_exit_code)"
  fi

  if ! command -v tar >/dev/null 2>&1; then
    write_status "error" "tar command not found in Termux" false 69
    emit_status
    return 69
  fi

  BACKUP_DIR="$(backup_dir)"
  if ! mkdir -p "$BACKUP_DIR"; then
    write_status "error" "failed to create backup directory: $BACKUP_DIR" false 73
    emit_status
    return 73
  fi
  if ! backup_write_test "$BACKUP_DIR"; then
    write_status "error" "backup directory is not writable: $BACKUP_DIR" false 73
    emit_status
    return 73
  fi
  if path_is_inside "$BACKUP_DIR" "$TAVERN_DIR"; then
    write_status "error" "backup directory is inside SillyTavern; backup is blocked to avoid recursive archives" false 73
    emit_status
    return 73
  fi

  stamp="$(date +"%Y%m%d-%H%M%S")"
  safe_label=""
  if [ "$BACKUP_KIND" = "manual" ] && [ -n "$BACKUP_LABEL" ]; then
    label_base="$(printf "%s" "$BACKUP_LABEL" | sed 's/[.][Tt][Aa][Rr][.][Gg][Zz]$//; s/[.][Tt][Gg][Zz]$//')"
    safe_label="$(safe_backup_label "$label_base")"
    [ -n "$safe_label" ] || safe_label="custom"
  fi
  if [ "$BACKUP_KIND" = "auto" ]; then
    archive_base="zd-$stamp"
  elif [ -n "$safe_label" ]; then
    archive_base="$safe_label"
  else
    archive_base="sd-$stamp"
  fi
  archive="$BACKUP_DIR/$archive_base.tar.gz"
  suffix=1
  while [ -e "$archive" ] || backup_filename_conflict_path "$(basename "$archive")" >/dev/null 2>&1; do
    archive="$BACKUP_DIR/$archive_base($suffix).tar.gz"
    suffix=$((suffix + 1))
  done

  parent="$(dirname "$TAVERN_DIR")"
  base="$(basename "$TAVERN_DIR")"

  TAVERN_CANON="$(canonical_existing_path "$TAVERN_DIR")"
  DATA_ROOT_RAW="$(resolve_tavern_data_root)"
  DATA_ROOT_CANON="$(canonical_existing_path "$DATA_ROOT_RAW")"
  CONFIG_DATA_ROOT="$(read_config_data_root || true)"
  if [ -z "$CONFIG_DATA_ROOT" ]; then
    CONFIG_DATA_ROOT="data"
  fi

  manifest_dir="$STATE_DIR/backup-manifest-$$"
  manifest_name="LUKOA_BACKUP_MANIFEST.txt"
  manifest_file="$manifest_dir/$manifest_name"
  expected_file="$STATE_DIR/backup-expected.txt"
  list_file="$STATE_DIR/backup-archive-list.txt"
  missing_file="$STATE_DIR/backup-missing.txt"
  error_file="$STATE_DIR/backup-error.log"
  rm -f "$expected_file" "$list_file" "$missing_file" "$error_file"
  : > "$expected_file"
  : > "$missing_file"
  : > "$error_file"
  if ! mkdir -p "$manifest_dir"; then
    write_status "error" "failed to create backup manifest directory" false 73
    emit_status
    return 73
  fi

  record_expected() {
    label="$1"
    source_path="$2"
    archive_entry="$3"
    if [ -e "$source_path" ]; then
      printf "%s|%s|%s\n" "$label" "$source_path" "$archive_entry" >> "$expected_file"
    fi
  }

  tavern_entry_for() {
    source_path="$(canonical_existing_path "$1")"
    if [ "$source_path" = "$TAVERN_CANON" ]; then
      printf "%s" "$base"
      return 0
    fi
    case "$source_path" in
      "$TAVERN_CANON"/*)
        printf "%s/%s" "$base" "${source_path#$TAVERN_CANON/}"
        return 0
        ;;
    esac
    return 1
  }

  record_tavern_expected() {
    label="$1"
    source_path="$2"
    if [ -e "$source_path" ]; then
      entry="$(tavern_entry_for "$source_path" || true)"
      if [ -n "$entry" ]; then
        record_expected "$label" "$source_path" "$entry"
      fi
    fi
  }

  EXTERNAL_DATA_ROOT=""
  EXTERNAL_DATA_ENTRY=""
  if [ -d "$DATA_ROOT_CANON" ] && ! path_is_inside "$DATA_ROOT_CANON" "$TAVERN_CANON"; then
    EXTERNAL_DATA_ROOT="$DATA_ROOT_CANON"
    EXTERNAL_DATA_ENTRY="$(basename "$DATA_ROOT_CANON")"
    record_expected "external-dataRoot" "$EXTERNAL_DATA_ROOT" "$EXTERNAL_DATA_ENTRY"
  elif [ -e "$DATA_ROOT_CANON" ]; then
    record_tavern_expected "dataRoot" "$DATA_ROOT_CANON"
  fi

  record_expected "tavern-root" "$TAVERN_CANON" "$base"
  record_tavern_expected "legacy-data" "$TAVERN_DIR/data"
  record_tavern_expected "config.yaml" "$TAVERN_DIR/config.yaml"
  record_tavern_expected ".env" "$TAVERN_DIR/.env"
  record_tavern_expected "root-secrets.json" "$TAVERN_DIR/secrets.json"
  record_tavern_expected "plugins" "$TAVERN_DIR/plugins"
  record_tavern_expected "third-party-extensions" "$TAVERN_DIR/public/scripts/extensions/third-party"

  {
    printf "schema=lukoa-sillytavern-backup-v2\n"
    printf "createdAt=%s\n" "$(timestamp)"
    printf "kind=%s\n" "$BACKUP_KIND"
    printf "label=%s\n" "${safe_label:-none}"
    printf "source=%s\n" "$TAVERN_CANON"
    printf "configuredDataRoot=%s\n" "$CONFIG_DATA_ROOT"
    printf "resolvedDataRoot=%s\n" "$DATA_ROOT_CANON"
    printf "externalDataRoot=%s\n" "${EXTERNAL_DATA_ROOT:-none}"
    printf "archive=%s\n" "$archive"
    printf "backupDir=%s\n" "$BACKUP_DIR"
    printf "autoKeep=%s\n" "$AUTO_KEEP"
    printf "\n[important paths checked]\n"
    cat "$expected_file"
  } > "$manifest_file"

  parent="$(dirname "$TAVERN_DIR")"
  base="$(basename "$TAVERN_DIR")"
  cd "$parent" || {
    write_status "error" "failed to enter SillyTavern parent directory" false 74
    safe_remove_backup_temp_dir "$manifest_dir"
    emit_status
    return 74
  }

  if [ -n "$EXTERNAL_DATA_ROOT" ]; then
    external_parent="$(dirname "$EXTERNAL_DATA_ROOT")"
    external_base="$(basename "$EXTERNAL_DATA_ROOT")"
    tar -czf "$archive" \
      --exclude="$base/node_modules" \
      --exclude="$base/.git" \
      --exclude="$base/.cache" \
      --exclude="$base/cache" \
      "$base" \
      -C "$manifest_dir" "$manifest_name" \
      -C "$external_parent" "$external_base" 2>"$error_file"
  else
    tar -czf "$archive" \
      --exclude="$base/node_modules" \
      --exclude="$base/.git" \
      --exclude="$base/.cache" \
      --exclude="$base/cache" \
      "$base" \
      -C "$manifest_dir" "$manifest_name" 2>"$error_file"
  fi
  code="$?"

  verify_code=0
  if [ "$code" -eq 0 ]; then
    if [ ! -s "$archive" ]; then
      verify_code=72
      printf "backup archive is empty\n" >> "$error_file"
    elif ! tar -tzf "$archive" > "$list_file" 2>>"$error_file"; then
      verify_code=71
      printf "archive list verification failed\n" >> "$error_file"
    else
      while IFS='|' read -r label source_path archive_entry; do
        [ -n "$label" ] || continue
        if ! archive_contains_entry "$archive_entry" "$list_file"; then
          printf "%s missing: source=%s archiveEntry=%s\n" "$label" "$source_path" "$archive_entry" >> "$missing_file"
        fi
      done < "$expected_file"
      if [ -s "$missing_file" ]; then
        verify_code=72
        printf "important backup entries are missing\n" >> "$error_file"
        cat "$missing_file" >> "$error_file"
      fi
    fi
  fi

  if [ "$code" -eq 0 ]; then
    size="$(ls -lh "$archive" 2>/dev/null | awk '{print $5}')"
    if [ "$verify_code" -eq 0 ] && [ "$BACKUP_KIND" = "auto" ]; then
      removed="$(
        safe_cleanup_auto_backups "$BACKUP_DIR" "$AUTO_KEEP"
      )"
    else
      removed=""
    fi
    if [ "$verify_code" -eq 0 ]; then
      write_status "backup" "SillyTavern $BACKUP_KIND backup created and verified: $archive" false 0
    else
      code="$verify_code"
      rm -f "$archive" 2>/dev/null || true
      write_status "error" "SillyTavern backup archive verification failed" false "$code"
    fi
  else
    write_status "error" "SillyTavern backup failed" false "$code"
  fi
  safe_remove_backup_temp_dir "$manifest_dir"
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup ====\n"
  printf "kind=%s\n" "$BACKUP_KIND"
  printf "label=%s\n" "${safe_label:-none}"
  printf "archive=%s\n" "$archive"
  printf "backupDir=%s\n" "$BACKUP_DIR"
  printf "source=%s\n" "$TAVERN_CANON"
  printf "dataRoot=%s\n" "$DATA_ROOT_CANON"
  printf "externalDataRoot=%s\n" "${EXTERNAL_DATA_ROOT:-none}"
  if [ "$BACKUP_KIND" = "auto" ]; then
    printf "autoKeep=%s\n" "$AUTO_KEEP"
    if [ -n "${removed:-}" ]; then
      printf "\n==== removed old auto backups ====\n"
      printf "%s\n" "$removed"
    fi
  fi
  [ -n "${size:-}" ] && printf "size=%s\n" "$size"
  if [ "$BACKUP_DIR" = "$STATE_DIR/backups/sd" ] || [ "$BACKUP_DIR" = "$STATE_DIR/backups/zd" ] || [ "$BACKUP_DIR" = "$STATE_DIR/backups" ]; then
    printf "notice=Termux storage is not available. Run termux-setup-storage if you want backups in Android Downloads.\n"
  fi
  printf "\n==== verified important paths ====\n"
  if [ -s "$expected_file" ]; then
    cat "$expected_file"
  else
    printf "no important paths were detected\n"
  fi
  if [ "$code" -ne 0 ]; then
    printf "\n==== backup error ====\n"
    cat "$error_file" 2>/dev/null || true
    if [ -s "$missing_file" ]; then
      printf "\n==== missing important entries ====\n"
      cat "$missing_file" 2>/dev/null || true
    fi
  fi
  printf "==== end SillyTavern backup ====\n"
  return "$code"
}

cmd_backup_list() {
  write_command "backup-list"
  MANUAL_BACKUP_DIR="$(backup_dir manual)"
  AUTO_BACKUP_DIR="$(backup_dir auto)"
  PRIVATE_BACKUP_DIR="$STATE_DIR/backups"
  write_status "backup-list" "SillyTavern backup directories listed" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backups ====\n"
  printf "manualDir=%s\n" "$MANUAL_BACKUP_DIR"
  printf "autoDir=%s\n" "$AUTO_BACKUP_DIR"
  printf "privateDir=%s\n" "$PRIVATE_BACKUP_DIR"
  seen_dirs="|"
  for dir in $(backup_library_dirs); do
    dir_key="$(canonical_existing_path "$dir")"
    case "$seen_dirs" in
      *"|$dir_key|"*) continue ;;
    esac
    seen_dirs="${seen_dirs}${dir_key}|"
    printf "\n-- %s --\n" "$dir"
    if [ -d "$dir" ]; then
      ls -1t "$dir"/*.tar.gz 2>/dev/null | head -n 20 | while IFS= read -r backup_file; do
        [ -n "$backup_file" ] || continue
        printf "backup.file=%s\n" "$backup_file"
        ls -lh "$backup_file" 2>/dev/null || true
      done
      if ! ls -1 "$dir"/*.tar.gz >/dev/null 2>&1; then
        printf "no backup files\n"
      fi
    else
      printf "directory does not exist\n"
    fi
  done
  printf "==== end SillyTavern backups ====\n"
}

archive_allowed_for_restore() {
  archive_path="$(canonical_existing_path "$1")"
  for restore_dir in $(backup_library_dirs); do
    if [ -d "$restore_dir" ] && path_is_inside "$archive_path" "$restore_dir"; then
      return 0
    fi
  done
  return 1
}

manifest_value_from_archive() {
  archive_path="$1"
  key="$2"
  tar -xOzf "$archive_path" LUKOA_BACKUP_MANIFEST.txt 2>/dev/null |
    sed -n "s/^$key=//p" |
    head -n 1
}

restore_root_name_from_archive() {
  archive_path="$1"
  restore_source="$(manifest_value_from_archive "$archive_path" "source" || true)"
  restore_root="$(basename "${restore_source:-SillyTavern}" 2>/dev/null || printf "SillyTavern")"
  restore_root="$(printf "%s" "$restore_root" | tr -d '\r\n')"
  case "$restore_root" in
    ""|"."|".."|*/*|*\\*)
      restore_root="SillyTavern"
      ;;
  esac
  printf "%s" "$restore_root"
}

validate_restore_archive() {
  archive_path="$1"
  list_file="$2"
  error_file="$3"
  expected_root="${4:-}"
  [ -n "$expected_root" ] || expected_root="$(restore_root_name_from_archive "$archive_path")"
  if ! tar -tzf "$archive_path" > "$list_file" 2>>"$error_file"; then
    if grep -qi "permission denied" "$error_file" 2>/dev/null; then
      printf "error=termux-storage-permission\n" >> "$error_file"
      printf "fix.command=termux-setup-storage\n" >> "$error_file"
      printf "fix.steps=Open Termux, run termux-setup-storage, allow storage permission, then retry.\n" >> "$error_file"
    fi
    printf "restore archive cannot be listed\n" >> "$error_file"
    return 71
  fi
  if awk '
    $0 ~ /^\// || $0 ~ /(^|\/)\.\.(\/|$)/ { bad = 1; print "unsafe archive entry: " $0 }
    END { exit bad ? 1 : 0 }
  ' "$list_file" >> "$error_file"; then
    :
  else
    return 72
  fi
  if ! archive_contains_entry "$expected_root" "$list_file"; then
    printf "restore archive does not contain expected root: %s\n" "$expected_root" >> "$error_file"
    return 73
  fi
  return 0
}

ensure_restore_archive_readable() {
  archive_path="$1"
  if [ -r "$archive_path" ]; then
    return 0
  fi

  case "$archive_path" in
    /storage/emulated/0/*|/sdcard/*|"$HOME_DIR"/storage/*)
      if command -v termux-setup-storage >/dev/null 2>&1; then
        termux-setup-storage >/dev/null 2>&1 || true
        sleep 1
      fi
      ;;
  esac

  if [ -r "$archive_path" ]; then
    return 0
  fi

  write_status "error" "Termux cannot read the backup archive. Grant Termux storage permission first." false 73
  cat "$STATUS_FILE"
  printf "\n==== restore storage permission ====\n"
  printf "error=termux-storage-permission\n"
  printf "archive=%s\n" "$archive_path"
  printf "fix.command=termux-setup-storage\n"
  printf "fix.steps=Open Termux, run termux-setup-storage, allow storage permission, then retry.\n"
  printf "==== end restore storage permission ====\n"
  return 73
}

unique_backup_destination() {
  dest_dir="$1"
  source_name="$2"
  safe_name="$(safe_backup_label "${source_name%.tar.gz}")"
  [ -n "$safe_name" ] || safe_name="imported-backup"
  dest="$dest_dir/$safe_name.tar.gz"
  suffix=1
  while [ -e "$dest" ]; do
    dest="$dest_dir/$safe_name($suffix).tar.gz"
    suffix=$((suffix + 1))
  done
  printf "%s" "$dest"
}

archive_allowed_for_import() {
  import_path="$(canonical_existing_path "$1")"
  downloads_root="$HOME_DIR/storage/downloads"
  public_downloads_root="/storage/emulated/0/Download"
  sdcard_downloads_root="/sdcard/Download"
  import_dir="$STATE_DIR/imports"
  if [ -d "$downloads_root" ] && path_is_inside "$import_path" "$downloads_root"; then
    return 0
  fi
  if [ -d "$public_downloads_root" ] && path_is_inside "$import_path" "$public_downloads_root"; then
    return 0
  fi
  if [ -d "$sdcard_downloads_root" ] && path_is_inside "$import_path" "$sdcard_downloads_root"; then
    return 0
  fi
  if [ -d "$import_dir" ] && path_is_inside "$import_path" "$import_dir"; then
    return 0
  fi
  if archive_allowed_for_restore "$import_path"; then
    return 0
  fi
  return 1
}

cmd_backup_delete() {
  write_command "backup-delete"
  archive="${1:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided for deletion" false 64
    emit_status
    return 64
  fi
  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be deleted" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi
  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup deletion is limited to Lukoa backup directories" false 73
    cat "$STATUS_FILE"
    printf "\n"
    emit_backup_library_dirs
    return 73
  fi
  if rm -f "$archive"; then
    write_status "backup-delete" "SillyTavern backup deleted" false 0
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup delete ====\n"
    printf "deleted.file=%s\n" "$archive"
    printf "==== end SillyTavern backup delete ====\n"
    cmd_backup_list
    return 0
  fi
  write_status "error" "Failed to delete backup archive" false 74
  emit_status
  return 74
}

cmd_backup_export() {
  write_command "backup-export"
  archive="${1:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided for export" false 64
    emit_status
    return 64
  fi
  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be exported" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi
  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup export is limited to Lukoa backup directories" false 73
    emit_status
    return 73
  fi
  if [ ! -s "$archive" ]; then
    write_status "error" "Backup archive is empty; export blocked" false 72
    emit_status
    return 72
  fi
  if command -v gzip >/dev/null 2>&1 && ! gzip -t "$archive" >/dev/null 2>&1; then
    write_status "error" "Backup archive is not a valid gzip file; export blocked" false 72
    emit_status
    return 72
  fi
  downloads_root="$(shared_download_dir || true)"
  if [ -z "$downloads_root" ]; then
    write_status "error" "Termux storage is not writable. Run termux-setup-storage before exporting backups." false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup export ====\n"
    printf "error=storage not writable\n"
    printf "fix.command=termux-setup-storage\n"
    printf "expected.downloads=%s\n" "$HOME_DIR/storage/downloads"
    printf "expected.publicDownloads=%s\n" "/storage/emulated/0/Download"
    printf "==== end SillyTavern backup export ====\n"
    return 73
  fi
  export_dir="$downloads_root/lukoa/exports"
  if ! mkdir -p "$export_dir" 2>/dev/null || ! backup_write_test "$export_dir"; then
    write_status "error" "Termux storage is not writable. Run termux-setup-storage before exporting backups." false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup export ====\n"
    printf "error=export directory not writable\n"
    printf "fix.command=termux-setup-storage\n"
    printf "export.dir=%s\n" "$export_dir"
    printf "==== end SillyTavern backup export ====\n"
    return 73
  fi
  archive_name="$(basename "$archive")"
  exported="$(unique_backup_destination "$export_dir" "$archive_name")"
  if ! cp "$archive" "$exported"; then
    write_status "error" "Failed to copy backup archive to export directory" false 74
    emit_status
    return 74
  fi
  if [ ! -s "$exported" ]; then
    rm -f "$exported" 2>/dev/null || true
    write_status "error" "Exported backup is empty; export blocked" false 72
    emit_status
    return 72
  fi
  source_size="$(wc -c < "$archive" 2>/dev/null | tr -d '[:space:]')"
  exported_size="$(wc -c < "$exported" 2>/dev/null | tr -d '[:space:]')"
  if [ -n "$source_size" ] && [ -n "$exported_size" ] && [ "$source_size" != "$exported_size" ]; then
    rm -f "$exported" 2>/dev/null || true
    write_status "error" "Exported backup size mismatch; export blocked" false 74
    emit_status
    return 74
  fi
  write_status "backup-export" "SillyTavern backup exported" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup export ====\n"
  printf "source.file=%s\n" "$archive"
  printf "source.size=%s\n" "$source_size"
  printf "export.dir=%s\n" "$export_dir"
  printf "exported.file=%s\n" "$exported"
  printf "exported.size=%s\n" "$exported_size"
  printf "==== end SillyTavern backup export ====\n"
  cmd_backup_list
}

cmd_backup_export_to() {
  write_command "backup-export-to"
  archive="${1:-}"
  destination="${2:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided for export" false 64
    emit_status
    return 64
  fi
  if [ -z "$destination" ]; then
    write_status "error" "No destination path was provided for export" false 64
    emit_status
    return 64
  fi
  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be exported" false 64
      emit_status
      return 64
      ;;
  esac
  case "$destination" in
    /*.tar.gz) ;;
    *)
      write_status "error" "Export destination must be an absolute .tar.gz path" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi
  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup export is limited to Lukoa backup directories" false 73
    emit_status
    return 73
  fi
  if [ ! -s "$archive" ]; then
    write_status "error" "Backup archive is empty; export blocked" false 72
    emit_status
    return 72
  fi
  if command -v gzip >/dev/null 2>&1 && ! gzip -t "$archive" >/dev/null 2>&1; then
    write_status "error" "Backup archive is not a valid gzip file; export blocked" false 72
    emit_status
    return 72
  fi

  dest_dir="$(dirname "$destination")"
  dest_base="$(basename "$destination")"
  if [ ! -d "$dest_dir" ]; then
    write_status "error" "Export destination directory does not exist: $dest_dir" false 73
    emit_status
    return 73
  fi
  destination="$(canonical_existing_path "$dest_dir")/$dest_base"
  if path_is_inside "$destination" "$TAVERN_DIR"; then
    write_status "error" "Export destination is inside SillyTavern; export blocked" false 73
    emit_status
    return 73
  fi
  if ! path_is_inside "$destination" "/storage/emulated/0" &&
    ! path_is_inside "$destination" "$HOME_DIR/storage/shared" &&
    ! path_is_inside "$destination" "$HOME_DIR/storage/downloads"; then
    write_status "error" "Export destination must be in Android shared storage" false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup export ====\n"
    printf "destination.file=%s\n" "$destination"
    printf "allowed.public=%s\n" "/storage/emulated/0"
    printf "allowed.shared=%s\n" "$HOME_DIR/storage/shared"
    printf "allowed.downloads=%s\n" "$HOME_DIR/storage/downloads"
    printf "==== end SillyTavern backup export ====\n"
    return 73
  fi
  if ! backup_write_test "$dest_dir"; then
    write_status "error" "Export destination is not writable. Run termux-setup-storage." false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup export ====\n"
    printf "destination.dir=%s\n" "$dest_dir"
    printf "fix.command=termux-setup-storage\n"
    printf "==== end SillyTavern backup export ====\n"
    return 73
  fi
  if [ -e "$destination" ]; then
    destination_canon="$(canonical_existing_path "$destination")"
    if [ "$destination_canon" = "$archive" ]; then
      write_status "error" "Export destination cannot be the source backup" false 73
      emit_status
      return 73
    fi
  fi

  temp_destination="$dest_dir/.lukoa-export-$$-$dest_base"
  rm -f "$temp_destination" 2>/dev/null || true
  if ! cp "$archive" "$temp_destination"; then
    rm -f "$temp_destination" 2>/dev/null || true
    write_status "error" "Failed to copy backup archive to selected destination" false 74
    emit_status
    return 74
  fi
  if [ ! -s "$temp_destination" ]; then
    rm -f "$temp_destination" 2>/dev/null || true
    write_status "error" "Exported backup is empty; export blocked" false 72
    emit_status
    return 72
  fi
  source_size="$(wc -c < "$archive" 2>/dev/null | tr -d '[:space:]')"
  exported_size="$(wc -c < "$temp_destination" 2>/dev/null | tr -d '[:space:]')"
  if [ -n "$source_size" ] && [ -n "$exported_size" ] && [ "$source_size" != "$exported_size" ]; then
    rm -f "$temp_destination" 2>/dev/null || true
    write_status "error" "Exported backup size mismatch; export blocked" false 74
    emit_status
    return 74
  fi
  if ! mv -f "$temp_destination" "$destination"; then
    rm -f "$temp_destination" 2>/dev/null || true
    write_status "error" "Failed to place backup at selected destination" false 74
    emit_status
    return 74
  fi
  if [ ! -s "$destination" ]; then
    rm -f "$destination" 2>/dev/null || true
    write_status "error" "Exported backup is empty after move; export blocked" false 72
    emit_status
    return 72
  fi
  final_size="$(wc -c < "$destination" 2>/dev/null | tr -d '[:space:]')"
  if [ -n "$source_size" ] && [ -n "$final_size" ] && [ "$source_size" != "$final_size" ]; then
    rm -f "$destination" 2>/dev/null || true
    write_status "error" "Final exported backup size mismatch; export blocked" false 74
    emit_status
    return 74
  fi
  write_status "backup-export-to" "SillyTavern backup exported to selected destination" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup export ====\n"
  printf "source.file=%s\n" "$archive"
  printf "source.size=%s\n" "$source_size"
  printf "destination.file=%s\n" "$destination"
  printf "exported.file=%s\n" "$destination"
  printf "exported.size=%s\n" "$final_size"
  printf "==== end SillyTavern backup export ====\n"
}

cmd_backup_copy() {
  write_command "backup-copy"
  archive="${1:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided for copy" false 64
    emit_status
    return 64
  fi
  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be copied" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi
  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup copy is limited to Lukoa backup directories" false 73
    cat "$STATUS_FILE"
    printf "\n"
    emit_backup_library_dirs
    return 73
  fi
  copy_dir="$(dirname "$archive")"
  if ! backup_write_test "$copy_dir"; then
    write_status "error" "Backup directory is not writable: $copy_dir" false 73
    emit_status
    return 73
  fi
  copied="$(unique_library_backup_destination "$copy_dir" "$(basename "$archive")")"
  if [ "$copied" = "$archive" ]; then
    write_status "error" "Copy destination unexpectedly matched source" false 74
    emit_status
    return 74
  fi
  if ! cp "$archive" "$copied"; then
    write_status "error" "Failed to copy backup archive" false 74
    emit_status
    return 74
  fi
  write_status "backup-copy" "SillyTavern backup copied" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup copy ====\n"
  printf "source.file=%s\n" "$archive"
  printf "copied.file=%s\n" "$copied"
  printf "backup.file=%s\n" "$copied"
  printf "==== end SillyTavern backup copy ====\n"
  cmd_backup_list
}

cmd_backup_rename() {
  write_command "backup-rename"
  archive="${1:-}"
  new_label="${2:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided for rename" false 64
    emit_status
    return 64
  fi
  if [ -z "$new_label" ]; then
    write_status "error" "No new backup name was provided" false 64
    emit_status
    return 64
  fi
  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be renamed" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi
  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup rename is limited to Lukoa backup directories" false 73
    cat "$STATUS_FILE"
    printf "\n"
    emit_backup_library_dirs
    return 73
  fi
  rename_dir="$(dirname "$archive")"
  if ! backup_write_test "$rename_dir"; then
    write_status "error" "Backup directory is not writable: $rename_dir" false 73
    emit_status
    return 73
  fi
  safe_label="$(safe_backup_label "${new_label%.tar.gz}")"
  if [ -z "$safe_label" ]; then
    write_status "error" "New backup name becomes empty after safety filtering" false 64
    emit_status
    return 64
  fi
  target_name="$safe_label.tar.gz"
  renamed="$rename_dir/$target_name"
  conflict_path="$(backup_filename_conflict_path "$target_name" "$archive" || true)"
  if [ -n "$conflict_path" ]; then
    write_status "error" "Backup rename target already exists in a Lukoa backup directory; choose another name" false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup rename ====\n"
    printf "source.file=%s\n" "$archive"
    printf "target.file=%s\n" "$renamed"
    printf "conflict.file=%s\n" "$conflict_path"
    printf "error=target exists\n"
    printf "==== end SillyTavern backup rename ====\n"
    return 73
  fi
  if [ -e "$renamed" ]; then
    renamed_canon="$(canonical_existing_path "$renamed")"
    if [ "$renamed_canon" = "$archive" ]; then
      write_status "backup-rename" "SillyTavern backup name unchanged" false 0
      cat "$STATUS_FILE"
      printf "\n==== SillyTavern backup rename ====\n"
      printf "renamed.old=%s\n" "$archive"
      printf "renamed.file=%s\n" "$archive"
      printf "notice=name unchanged\n"
      printf "==== end SillyTavern backup rename ====\n"
      cmd_backup_list
      return 0
    fi
    write_status "error" "Backup rename target already exists; choose another name" false 73
    cat "$STATUS_FILE"
    printf "\n==== SillyTavern backup rename ====\n"
    printf "source.file=%s\n" "$archive"
    printf "target.file=%s\n" "$renamed"
    printf "error=target exists\n"
    printf "==== end SillyTavern backup rename ====\n"
    return 73
  fi
  if ! mv "$archive" "$renamed"; then
    write_status "error" "Failed to rename backup archive" false 74
    emit_status
    return 74
  fi
  write_status "backup-rename" "SillyTavern backup renamed" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup rename ====\n"
  printf "renamed.old=%s\n" "$archive"
  printf "renamed.file=%s\n" "$renamed"
  printf "==== end SillyTavern backup rename ====\n"
  cmd_backup_list
}

cmd_backup_import() {
  write_command "backup-import"
  source_archive="${1:-}"
  if [ -z "$source_archive" ]; then
    write_status "error" "No backup archive path was provided for import" false 64
    emit_status
    return 64
  fi
  case "$source_archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz backup archives can be imported" false 64
      emit_status
      return 64
      ;;
  esac
  if [ ! -f "$source_archive" ]; then
    write_status "error" "Backup archive not found: $source_archive" false 66
    emit_status
    return 66
  fi
  source_archive="$(canonical_existing_path "$source_archive")"
  if ! archive_allowed_for_import "$source_archive"; then
    write_status "error" "Backup import must read from Downloads, Lukoa import directory, or an existing Lukoa backup directory" false 73
    cat "$STATUS_FILE"
    printf "\nallowed.downloads=%s\n" "$HOME_DIR/storage/downloads"
    printf "allowed.publicDownloads=%s\n" "/storage/emulated/0/Download"
    printf "allowed.imports=%s\n" "$STATE_DIR/imports"
    return 73
  fi
  import_list_file="$STATE_DIR/import-archive-list.txt"
  import_error_file="$STATE_DIR/import-error.log"
  rm -f "$import_list_file" "$import_error_file"
  : > "$import_error_file"
  import_root_name="$(restore_root_name_from_archive "$source_archive")"
  validate_restore_archive "$source_archive" "$import_list_file" "$import_error_file" "$import_root_name"
  validate_code="$?"
  if [ "$validate_code" -ne 0 ]; then
    write_status "error" "Imported backup failed safety validation" false "$validate_code"
    cat "$STATUS_FILE"
    printf "\n==== import error ====\n"
    cat "$import_error_file" 2>/dev/null || true
    printf "==== end SillyTavern backup import ====\n"
    return "$validate_code"
  fi
  dest_dir="$(backup_dir manual)"
  if ! mkdir -p "$dest_dir" || ! backup_write_test "$dest_dir"; then
    write_status "error" "Backup directory is not writable: $dest_dir" false 73
    emit_status
    return 73
  fi
  if archive_allowed_for_restore "$source_archive"; then
    imported="$source_archive"
  else
    imported="$(unique_library_backup_destination "$dest_dir" "$(basename "$source_archive")")"
    if ! cp "$source_archive" "$imported"; then
      write_status "error" "Failed to copy imported backup into backup directory" false 74
      emit_status
      return 74
    fi
  fi
  write_status "backup-import" "SillyTavern backup imported" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern backup import ====\n"
  printf "source.file=%s\n" "$source_archive"
  printf "imported.file=%s\n" "$imported"
  printf "backup.file=%s\n" "$imported"
  printf "==== end SillyTavern backup import ====\n"
  cmd_backup_list
}

cmd_restore() {
  write_command "restore"
  archive="${1:-}"
  if [ -z "$archive" ]; then
    write_status "error" "No backup archive path was provided" false 64
    emit_status
    return 64
  fi

  if http_ok || is_running || [ -n "$(candidate_pids | head -n 1)" ]; then
    write_status "error" "Please stop SillyTavern before applying a backup" true 77
    emit_status
    return 77
  fi

  if ! command -v tar >/dev/null 2>&1; then
    write_status "error" "tar command not found in Termux" false 69
    emit_status
    return 69
  fi

  if [ ! -f "$archive" ]; then
    write_status "error" "Backup archive not found: $archive" false 66
    emit_status
    return 66
  fi

  case "$archive" in
    *.tar.gz) ;;
    *)
      write_status "error" "Only .tar.gz SillyTavern backups can be applied" false 64
      emit_status
      return 64
      ;;
  esac

  archive="$(canonical_existing_path "$archive")"
  if ! archive_allowed_for_restore "$archive"; then
    write_status "error" "Backup restore is limited to Lukoa backup directories to avoid applying the wrong file" false 73
    cat "$STATUS_FILE"
    printf "\n"
    emit_backup_library_dirs
    return 73
  fi
  if ! ensure_restore_archive_readable "$archive"; then
    return 73
  fi

  restore_archive="$archive"
  restore_root_name="$(restore_root_name_from_archive "$restore_archive")"
  restore_stamp="$(date +"%Y%m%d-%H%M%S")"
  restore_list_file="$STATE_DIR/restore-archive-list.txt"
  restore_error_file="$STATE_DIR/restore-error.log"
  restore_temp_dir="$STATE_DIR/restore-staging-$$"
  stamp="$restore_stamp"
  list_file="$restore_list_file"
  error_file="$restore_error_file"
  temp_dir="$restore_temp_dir"
  pre_restore_archive="none"
  rm -f "$list_file" "$error_file"
  : > "$error_file"

  validate_restore_archive "$restore_archive" "$list_file" "$error_file" "$restore_root_name"
  validate_code="$?"
  if [ "$validate_code" -ne 0 ]; then
    write_status "error" "Backup archive failed safety validation" false "$validate_code"
    cat "$STATUS_FILE"
    printf "\n==== restore error ====\n"
    cat "$error_file" 2>/dev/null || true
    printf "==== end SillyTavern restore ====\n"
    return "$validate_code"
  fi

  mkdir -p "$temp_dir" || {
    write_status "error" "failed to create restore staging directory" false 73
    emit_status
    return 73
  }

  if ! tar -xzf "$restore_archive" -C "$temp_dir" 2>>"$error_file"; then
    safe_remove_restore_temp_dir "$temp_dir"
    write_status "error" "failed to extract backup archive" false 74
    cat "$STATUS_FILE"
    printf "\n==== restore error ====\n"
    cat "$error_file" 2>/dev/null || true
    printf "==== end SillyTavern restore ====\n"
    return 74
  fi

  restore_source_dir="$temp_dir/$restore_root_name"
  if [ ! -d "$restore_source_dir" ]; then
    safe_remove_restore_temp_dir "$temp_dir"
    write_status "error" "backup archive does not contain the expected tavern directory" false 73
    emit_status
    return 73
  fi

  parent="$(dirname "$TAVERN_DIR")"
  base="$(basename "$TAVERN_DIR")"
  mkdir -p "$parent"
  rollback_dir="$parent/$base.lukoa-before-restore-$stamp"
  failed_restore_dir="$parent/$base.lukoa-failed-restore-$stamp"

  if [ -e "$TAVERN_DIR" ]; then
    if ! mv "$TAVERN_DIR" "$rollback_dir" 2>>"$error_file"; then
      safe_remove_restore_temp_dir "$temp_dir"
      write_status "error" "failed to move current SillyTavern aside before restore" false 74
      cat "$STATUS_FILE"
      printf "\n==== restore error ====\n"
      cat "$error_file" 2>/dev/null || true
      printf "==== end SillyTavern restore ====\n"
      return 74
    fi
  else
    rollback_dir=""
  fi

  if ! mv "$restore_source_dir" "$TAVERN_DIR" 2>>"$error_file"; then
    [ -n "$rollback_dir" ] && [ -e "$rollback_dir" ] && mv "$rollback_dir" "$TAVERN_DIR" 2>/dev/null || true
    safe_remove_restore_temp_dir "$temp_dir"
    write_status "error" "failed to place restored SillyTavern directory; original directory was restored if possible" false 74
    cat "$STATUS_FILE"
    printf "\n==== restore error ====\n"
    cat "$error_file" 2>/dev/null || true
    printf "==== end SillyTavern restore ====\n"
    return 74
  fi

  external_root="$(manifest_value_from_archive "$restore_archive" "externalDataRoot" || true)"
  external_restored="none"
  external_rollback="none"
  if [ -n "$external_root" ] && [ "$external_root" != "none" ]; then
    external_base="$(basename "$external_root")"
    external_stage="$temp_dir/$external_base"
    if [ -e "$external_stage" ] && ! path_is_inside "$external_root" "$TAVERN_DIR"; then
      external_parent="$(dirname "$external_root")"
      mkdir -p "$external_parent"
      if [ -e "$external_root" ]; then
        external_rollback="$external_root.lukoa-before-restore-$stamp"
        if ! mv "$external_root" "$external_rollback" 2>>"$error_file"; then
          mv "$TAVERN_DIR" "$failed_restore_dir" 2>/dev/null || true
          [ -n "$rollback_dir" ] && [ -e "$rollback_dir" ] && mv "$rollback_dir" "$TAVERN_DIR" 2>/dev/null || true
          safe_remove_restore_temp_dir "$temp_dir"
          write_status "error" "failed to move current external dataRoot aside; restore was rolled back if possible" false 74
          cat "$STATUS_FILE"
          printf "\n==== restore error ====\n"
          cat "$error_file" 2>/dev/null || true
          printf "==== end SillyTavern restore ====\n"
          return 74
        fi
      fi
      if mv "$external_stage" "$external_root" 2>>"$error_file"; then
        external_restored="$external_root"
      else
        [ "$external_rollback" != "none" ] && [ -e "$external_rollback" ] && mv "$external_rollback" "$external_root" 2>/dev/null || true
        mv "$TAVERN_DIR" "$failed_restore_dir" 2>/dev/null || true
        [ -n "$rollback_dir" ] && [ -e "$rollback_dir" ] && mv "$rollback_dir" "$TAVERN_DIR" 2>/dev/null || true
        safe_remove_restore_temp_dir "$temp_dir"
        write_status "error" "failed to restore external dataRoot; restore was rolled back if possible" false 74
        cat "$STATUS_FILE"
        printf "\n==== restore error ====\n"
        cat "$error_file" 2>/dev/null || true
        printf "==== end SillyTavern restore ====\n"
        return 74
      fi
    fi
  fi

  safe_remove_restore_temp_dir "$temp_dir"
  write_status "restored" "SillyTavern backup applied successfully" false 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern restore ====\n"
  printf "archive=%s\n" "$restore_archive"
  printf "restoredTo=%s\n" "$TAVERN_DIR"
  printf "previousDirectory=%s\n" "${rollback_dir:-none}"
  printf "preRestoreSafetyBackup=%s\n" "${pre_restore_archive:-unknown}"
  printf "externalDataRootRestored=%s\n" "$external_restored"
  printf "externalDataRootPrevious=%s\n" "$external_rollback"
  printf "notice=If dependencies are missing after restore, run update or npm install before starting SillyTavern.\n"
  printf "==== end SillyTavern restore ====\n"
}

cmd_restart() {
  old_quiet="${LUKOA_QUIET:-0}"
  LUKOA_QUIET=1
  cmd_stop
  stop_code="$?"
  LUKOA_QUIET="$old_quiet"
  if [ "$stop_code" -ne 0 ]; then
    emit_status
    return "$stop_code"
  fi
  cmd_start
}

main() {
  command="${1:-status}"
  case "$command" in
    selftest)
      shift
      cmd_selftest "$@"
      ;;
    status)
      cmd_status
      ;;
    log|logs|tail)
      cmd_log
      ;;
    log-live|live-log|delta-log)
      cmd_log_live
      ;;
    console|foreground)
      cmd_console
      ;;
    start)
      cmd_start
      ;;
    stop)
      cmd_stop
      ;;
    restart)
      cmd_restart
      ;;
    official-versions|tavern-official-versions)
      cmd_official_versions
      ;;
    install|tavern-install)
      shift
      cmd_install "$@"
      ;;
    update|tavern-update)
      shift
      cmd_update "$@"
      ;;
    rollback|tavern-rollback)
      shift
      cmd_rollback "$@"
      ;;
    version|tavern-version)
      cmd_version
      ;;
    backup|tavern-backup)
      shift
      cmd_backup "$@"
      ;;
    backup-list|tavern-backup-list)
      cmd_backup_list
      ;;
    backup-delete|tavern-backup-delete)
      shift
      cmd_backup_delete "$@"
      ;;
    backup-export|tavern-backup-export)
      shift
      cmd_backup_export "$@"
      ;;
    backup-export-to|tavern-backup-export-to)
      shift
      cmd_backup_export_to "$@"
      ;;
    backup-copy|tavern-backup-copy)
      shift
      cmd_backup_copy "$@"
      ;;
    backup-import|tavern-backup-import)
      shift
      cmd_backup_import "$@"
      ;;
    backup-rename|tavern-backup-rename)
      shift
      cmd_backup_rename "$@"
      ;;
    restore|tavern-restore)
      shift
      cmd_restore "$@"
      ;;
    *)
      write_status "error" "unknown command: $command" false 64
      return 64
      ;;
  esac
}

main "$@"
