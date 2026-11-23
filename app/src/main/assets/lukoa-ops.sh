#!/data/data/com.termux/files/usr/bin/sh
# LUKOA_OPS_SCRIPT_REV=2026-11-23-ops-c-1

set -u

APP_NAME="lukoa-launcher"
HOME_DIR="${LUKOA_HOME:-${HOME:-/data/data/com.termux/files/home}}"
STATE_DIR="${LUKOA_STATE_DIR:-$HOME_DIR/.local/state/$APP_NAME}"
CONFIG_FILE="${LUKOA_CONFIG_FILE:-$HOME_DIR/.config/$APP_NAME/config.env}"
LAUNCHER_TAVERN_ROOT_DIR="$HOME_DIR/LukoaLauncher"
LEGACY_DEFAULT_TAVERN_DIR="$HOME_DIR/SillyTavern"
TAVERN_PORT="${TAVERN_PORT:-8000}"

if [ -f "$CONFIG_FILE" ]; then
  # shellcheck disable=SC1090
  . "$CONFIG_FILE"
fi
[ -n "${LUKOA_TAVERN_DIR:-}" ] && TAVERN_DIR="$LUKOA_TAVERN_DIR"
[ -n "${LUKOA_TAVERN_PORT:-}" ] && TAVERN_PORT="$LUKOA_TAVERN_PORT"
[ -n "${LUKOA_TAVERN_PROFILE_ID:-}" ] && TAVERN_PROFILE_ID="$LUKOA_TAVERN_PROFILE_ID"

profile_slot_number() {
  profile_id="${1:-main}"
  [ "$profile_id" = "main" ] && { printf "1"; return 0; }
  slot="${profile_id#profile-}"
  case "$slot" in ''|*[!0-9]*) printf "2" ;; *) [ "$slot" -lt 2 ] && printf "2" || printf "%s" "$slot" ;; esac
}

launcher_managed_profile_dir() {
  slot="$(profile_slot_number "${1:-${TAVERN_PROFILE_ID:-main}}")"
  [ "$slot" -le 1 ] && printf "%s/SillyTavern" "$LAUNCHER_TAVERN_ROOT_DIR" || printf "%s/SillyTavern%s" "$LAUNCHER_TAVERN_ROOT_DIR" "$slot"
}

profile_default_tavern_dir() {
  profile_id="${1:-main}"
  [ "$profile_id" = "main" ] && printf "%s" "$LEGACY_DEFAULT_TAVERN_DIR" || launcher_managed_profile_dir "$profile_id"
}

sanitize_profile_state_key() {
  sanitized="$(printf "%s" "${1:-main}" | LC_ALL=C tr -c 'A-Za-z0-9._-' '_')"
  [ -z "$sanitized" ] && sanitized="main"
  printf "%s" "$sanitized"
}

PROFILE_STATE_KEY="$(sanitize_profile_state_key "${TAVERN_PROFILE_ID:-main}")"
RUNTIME_STATE_DIR="$STATE_DIR/profiles/$PROFILE_STATE_KEY"
mkdir -p "$STATE_DIR" "$RUNTIME_STATE_DIR"
PID_FILE="$RUNTIME_STATE_DIR/pid"
STATUS_FILE="$RUNTIME_STATE_DIR/status.json"
LOG_FILE="$RUNTIME_STATE_DIR/tavern.log"
NODE_MEMORY_FILE="$RUNTIME_STATE_DIR/node-memory.env"

case "$TAVERN_PORT" in ''|*[!0-9]*) TAVERN_PORT=8000 ;; esac
[ "$TAVERN_PORT" -lt 1 ] || [ "$TAVERN_PORT" -gt 65535 ] && TAVERN_PORT=8000

strip_trailing_slashes() {
  p="$1"
  while [ "${#p}" -gt 1 ] && [ "${p%/}" != "$p" ]; do p="${p%/}"; done
  printf "%s" "$p"
}

expand_launcher_path() {
  p="$1"
  [ -z "$p" ] && { printf ""; return 0; }
  if [ "$p" = "~" ] || [ "$p" = "\$HOME" ]; then strip_trailing_slashes "$HOME_DIR"; return 0; fi
  if [ "${p#~/}" != "$p" ]; then strip_trailing_slashes "$HOME_DIR/${p#~/}"; return 0; fi
  if [ "${p#\$HOME/}" != "$p" ]; then strip_trailing_slashes "$HOME_DIR/${p#\$HOME/}"; return 0; fi
  strip_trailing_slashes "$p"
}

[ -z "${TAVERN_DIR:-}" ] && TAVERN_DIR="$(profile_default_tavern_dir "${TAVERN_PROFILE_ID:-main}")"
TAVERN_DIR="$(expand_launcher_path "$TAVERN_DIR")"

is_int_in_range() {
  case "$1" in ''|*[!0-9]*) return 1 ;; esac
  [ "$1" -ge "$2" ] && [ "$1" -le "$3" ]
}

timestamp() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }
json_escape() { printf "%s" "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

write_status() {
  st="$1"; msg="$2"; running="$3"; code="${4:-0}"
  cat > "$STATUS_FILE.$$" <<EOF
{
  "app": "$APP_NAME",
  "time": "$(timestamp)",
  "status": "$st",
  "running": $running,
  "exitCode": $code,
  "message": "$(json_escape "$msg")",
  "tavernDir": "$(json_escape "$TAVERN_DIR")",
  "port": $TAVERN_PORT,
  "profileId": "$(json_escape "${TAVERN_PROFILE_ID:-main}")",
  "runtimeStateDir": "$(json_escape "$RUNTIME_STATE_DIR")",
  "pidFile": "$PID_FILE",
  "logFile": "$LOG_FILE"
}
EOF
  mv -f "$STATUS_FILE.$$" "$STATUS_FILE"
}

is_running_quick() {
  [ -f "$PID_FILE" ] || return 1
  p="$(cat "$PID_FILE" 2>/dev/null || true)"
  case "$p" in ''|*[!0-9]*) return 1 ;; esac
  kill -0 "$p" 2>/dev/null
}

tavern_running_value() {
  is_running_quick && printf "true" || printf "false"
}

resolve_tavern_data_root() {
  cfg="$TAVERN_DIR/config.yaml"
  raw=""
  if [ -f "$cfg" ]; then
    raw="$(awk '/^[[:space:]]*dataRoot[[:space:]]*:/ { sub(/^[^:]*:[[:space:]]*/, ""); sub(/[[:space:]]*#.*$/, ""); gsub(/^["'\'']|["'\'']$/, ""); gsub(/^[[:space:]]+|[[:space:]]+$/, ""); print; exit }' "$cfg" 2>/dev/null || true)"
  fi
  [ -z "$raw" ] && raw="data"
  case "$raw" in
    /*) strip_trailing_slashes "$raw" ;;
    ~*|"\$HOME"*) expand_launcher_path "$raw" ;;
    *) strip_trailing_slashes "$TAVERN_DIR/${raw#./}" ;;
  esac
}

dir_kb() {
  [ -e "${1:-}" ] || { printf "0"; return 0; }
  k="$(du -sk "$1" 2>/dev/null | awk '{print $1}')"
  case "$k" in ''|*[!0-9]*) k=0 ;; esac
  printf "%s" "$k"
}

run_tavern_config_action() {
  cfg_action="$1"
  cfg_payload="${2:-}"
  [ -d "$TAVERN_DIR" ] || {
    write_status "error" "SillyTavern directory not found: $TAVERN_DIR" false 66
    cat "$STATUS_FILE"
    return 66
  }
  command -v node >/dev/null 2>&1 || {
    write_status "error" "node command not found in Termux" "$(tavern_running_value)" 69
    cat "$STATUS_FILE"
    return 69
  }
  cfg_out="$(
    LUKOA_CONFIG_ACTION="$cfg_action" \
    LUKOA_CONFIG_PAYLOAD="$cfg_payload" \
    LUKOA_TAVERN_DIR="$TAVERN_DIR" \
    node -e '
const fs = require("fs");
const path = require("path");
const { createRequire } = require("module");

const action = process.env.LUKOA_CONFIG_ACTION || "read";
const payload = process.env.LUKOA_CONFIG_PAYLOAD || "";
const tavernDir = process.env.LUKOA_TAVERN_DIR || "";
const configPath = path.join(tavernDir, "config.yaml");
const defaultPath = path.join(tavernDir, "default", "config.yaml");
const fail = (msg, code = 65) => { console.error(msg); process.exit(code); };
const b64 = (v) => Buffer.from(String(v ?? ""), "utf8").toString("base64url");

let yamlMod = null;
let jsYamlMod = null;
try {
  const req = createRequire(path.join(tavernDir, "package.json"));
  try { yamlMod = req("yaml"); } catch (_) {}
  if (!yamlMod) { try { jsYamlMod = req("js-yaml"); } catch (_) {} }
} catch (_) {}
if (!yamlMod && !jsYamlMod) fail("YAML parser not found in SillyTavern node_modules; run repair dependencies first", 69);

function parseObj(text) {
  if (yamlMod && typeof yamlMod.parseDocument === "function") {
    const doc = yamlMod.parseDocument(text || "");
    if (doc.errors && doc.errors.length > 0) fail("config.yaml syntax error: " + doc.errors[0].message, 65);
    return { doc, obj: doc.toJS() || {} };
  }
  const obj = (jsYamlMod.load(text || "") || {});
  return { doc: null, obj };
}

const getBool = (v, def) => (typeof v === "boolean" ? v : def);
const getStr = (v, def) => (typeof v === "string" ? v : def);
const getInt = (v, def) => (Number.isInteger(v) ? v : def);
const getStrList = (v, def) => (Array.isArray(v) ? v.map(x => String(x ?? "").trim()).filter(Boolean) : def);

let backupPath = "";
if (action === "set") {
  let patch;
  try {
    patch = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    if (!patch || typeof patch !== "object" || Array.isArray(patch)) throw new Error("not object");
  } catch (_) {
    fail("Invalid config patch payload", 64);
  }
  const baseText = fs.existsSync(configPath)
    ? fs.readFileSync(configPath, "utf8")
    : (fs.existsSync(defaultPath) ? fs.readFileSync(defaultPath, "utf8") : "{}\n");
  const { doc, obj } = parseObj(baseText);
  const setPath = (keys, val) => {
    if (doc) {
      doc.setIn(keys, val);
    } else {
      let cur = obj;
      for (let i = 0; i < keys.length - 1; i++) {
        if (!cur[keys[i]] || typeof cur[keys[i]] !== "object" || Array.isArray(cur[keys[i]])) cur[keys[i]] = {};
        cur = cur[keys[i]];
      }
      cur[keys[keys.length - 1]] = val;
    }
  };
  if (typeof patch.listen === "boolean") setPath(["listen"], patch.listen);
  if (typeof patch.whitelistMode === "boolean") setPath(["whitelistMode"], patch.whitelistMode);
  if (Array.isArray(patch.whitelist)) {
    const wl = patch.whitelist.map(x => String(x || "").trim()).filter(Boolean);
    if (wl.some(x => /[\r\n\0]/.test(x) || x.length > 128)) fail("Invalid whitelist entry", 64);
    setPath(["whitelist"], wl);
  }
  if (typeof patch.basicAuthMode === "boolean") setPath(["basicAuthMode"], patch.basicAuthMode);
  if (typeof patch.basicAuthUsername === "string") {
    const u = patch.basicAuthUsername.trim();
    if (!u || u.length > 64 || /[\r\n\0]/.test(u)) fail("Invalid basic auth username", 64);
    setPath(["basicAuthUser", "username"], u);
  }
  if (patch.updateBasicAuthPassword === true && typeof patch.basicAuthPassword === "string") {
    const p = patch.basicAuthPassword;
    if (!p || p.length > 128 || /[\r\n\0]/.test(p)) fail("Invalid basic auth password", 64);
    setPath(["basicAuthUser", "password"], p);
  }
  if (typeof patch.enableUserAccounts === "boolean") setPath(["enableUserAccounts"], patch.enableUserAccounts);
  if (typeof patch.enableExtensions === "boolean") setPath(["enableExtensions"], patch.enableExtensions);
  if (typeof patch.enableServerPlugins === "boolean") setPath(["enableServerPlugins"], patch.enableServerPlugins);
  if (typeof patch.lazyLoadCharacters === "boolean") setPath(["performance", "lazyLoadCharacters"], patch.lazyLoadCharacters);
  if (typeof patch.chatBackupEnabled === "boolean") setPath(["backups", "chat", "enabled"], patch.chatBackupEnabled);
  if (Number.isInteger(patch.chatMaxTotalBackups)) {
    const n = patch.chatMaxTotalBackups;
    if (n !== -1 && (n < 5 || n > 500)) fail("chatMaxTotalBackups out of range", 64);
    setPath(["backups", "chat", "maxTotalBackups"], n);
  }
  if (typeof patch.requestProxyEnabled === "boolean") setPath(["requestProxy", "enabled"], patch.requestProxyEnabled);
  if (typeof patch.requestProxyUrl === "string") {
    const u = patch.requestProxyUrl.trim();
    if (u && !/^(https?|socks4a?|socks5h?):\/\/[^\s]+$/i.test(u)) fail("Invalid requestProxy URL", 64);
    setPath(["requestProxy", "url"], u);
  }
  if (Array.isArray(patch.requestProxyBypass)) {
    const bp = patch.requestProxyBypass.map(x => String(x || "").trim()).filter(Boolean);
    if (bp.some(x => /[\r\n\0]/.test(x) || x.length > 128)) fail("Invalid requestProxy bypass entry", 64);
    setPath(["requestProxy", "bypass"], bp);
  }
  const outText = doc ? doc.toString() : jsYamlMod.dump(obj, { lineWidth: -1 });
  parseObj(outText);
  if (fs.existsSync(configPath)) {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    const stamp = `${now.getFullYear()}${pad(now.getMonth()+1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
    backupPath = path.join(tavernDir, `config.yaml.lukoa-backup-${stamp}`);
    fs.copyFileSync(configPath, backupPath);
    const oldBackups = fs.readdirSync(tavernDir)
      .filter(f => f.startsWith("config.yaml.lukoa-backup-"))
      .sort()
      .reverse();
    for (const stale of oldBackups.slice(5)) {
      try { fs.rmSync(path.join(tavernDir, stale), { force: true }); } catch (_) {}
    }
  }
  const tmpPath = path.join(tavernDir, `config.yaml.lukoa-tmp.${process.pid}`);
  fs.writeFileSync(tmpPath, outText, "utf8");
  fs.renameSync(tmpPath, configPath);
}

const exists = fs.existsSync(configPath);
const readText = exists
  ? fs.readFileSync(configPath, "utf8")
  : (fs.existsSync(defaultPath) ? fs.readFileSync(defaultPath, "utf8") : "");
const { obj } = parseObj(readText);
const basicUser = (obj.basicAuthUser && typeof obj.basicAuthUser === "object") ? obj.basicAuthUser : {};
const perf = (obj.performance && typeof obj.performance === "object") ? obj.performance : {};
const backupsChat = (obj.backups && obj.backups.chat && typeof obj.backups.chat === "object") ? obj.backups.chat : {};
const proxy = (obj.requestProxy && typeof obj.requestProxy === "object") ? obj.requestProxy : {};

console.log("==== SillyTavern config ====");
console.log("config.path=" + b64(configPath));
console.log("config.exists=" + (exists ? "true" : "false"));
if (backupPath) console.log("config.backup=" + b64(backupPath));
console.log("config.listen=" + getBool(obj.listen, false));
console.log("config.whitelistMode=" + getBool(obj.whitelistMode, true));
console.log("config.whitelist=" + b64(getStrList(obj.whitelist, ["::1", "127.0.0.1"]).join("\n")));
console.log("config.basicAuthMode=" + getBool(obj.basicAuthMode, false));
console.log("config.basicAuthUsername=" + b64(getStr(basicUser.username, "user")));
console.log("config.basicAuthHasPassword=" + (getStr(basicUser.password, "").length > 0));
console.log("config.enableUserAccounts=" + getBool(obj.enableUserAccounts, false));
console.log("config.enableExtensions=" + getBool(obj.enableExtensions, true));
console.log("config.enableServerPlugins=" + getBool(obj.enableServerPlugins, false));
console.log("config.lazyLoadCharacters=" + getBool(perf.lazyLoadCharacters, false));
console.log("config.chatBackupEnabled=" + getBool(backupsChat.enabled, true));
console.log("config.chatMaxTotalBackups=" + getInt(backupsChat.maxTotalBackups, 50));
console.log("config.requestProxyEnabled=" + getBool(proxy.enabled, false));
console.log("config.requestProxyUrl=" + b64(getStr(proxy.url, "")));
console.log("config.requestProxyBypass=" + b64(getStrList(proxy.bypass, ["localhost", "127.0.0.1"]).join("\n")));
console.log("==== end SillyTavern config ====");
' 2>&1
  )"
  cfg_code="$?"
  if [ "$cfg_code" -ne 0 ]; then
    write_status "error" "SillyTavern config $cfg_action failed" "$(tavern_running_value)" "$cfg_code"
    cat "$STATUS_FILE"
    printf "\n%s\n" "$cfg_out"
    return "$cfg_code"
  fi
  write_status "config-$cfg_action" "SillyTavern config $cfg_action completed" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n%s\n" "$cfg_out"
}

emit_storage_snapshot() {
  st_freed="${1:-}"
  st_data="$(resolve_tavern_data_root)"
  st_total="$(dir_kb "$TAVERN_DIR")"
  st_data_kb="$(dir_kb "$st_data")"
  st_chats_kb=0
  st_chars_kb=0
  st_thumbs_kb=0
  st_backups_kb=0
  if [ -d "$st_data" ]; then
    for d in "$st_data"/*/chats "$st_data"/*/"group chats" "$st_data/chats" "$st_data/group chats"; do
      [ -d "$d" ] && [ ! -L "$d" ] && st_chats_kb=$((st_chats_kb + $(dir_kb "$d")))
    done
    for d in "$st_data"/*/characters "$st_data/characters"; do
      [ -d "$d" ] && [ ! -L "$d" ] && st_chars_kb=$((st_chars_kb + $(dir_kb "$d")))
    done
    for d in "$st_data"/*/thumbnails "$st_data/thumbnails"; do
      [ -d "$d" ] && [ ! -L "$d" ] && st_thumbs_kb=$((st_thumbs_kb + $(dir_kb "$d")))
    done
    for d in "$st_data"/*/backups "$st_data/backups"; do
      [ -d "$d" ] && [ ! -L "$d" ] && st_backups_kb=$((st_backups_kb + $(dir_kb "$d")))
    done
  fi
  st_nm_kb="$(dir_kb "$TAVERN_DIR/node_modules")"
  st_ext_kb=$(( $(dir_kb "$TAVERN_DIR/public/scripts/extensions/third-party") + $(dir_kb "$TAVERN_DIR/public/scripts/extensions/.lukoa-disabled-third-party") ))
  st_ext_rb_kb="$(dir_kb "$TAVERN_DIR/public/scripts/extensions/.lukoa-extension-rollbacks")"
  st_logs_kb=$(( $(dir_kb "$LOG_FILE") + $(dir_kb "$LOG_FILE.old") ))
  st_old_log_kb="$(dir_kb "$LOG_FILE.old")"
  st_npm_cache_kb="$(dir_kb "$HOME_DIR/.npm")"
  st_leftover_kb=0
  st_leftover_cnt=0
  st_parent="$(dirname "$TAVERN_DIR")"
  st_data_parent="$(dirname "$st_data")"
  for p in \
    "$TAVERN_DIR"/.lukoa-before-repair-* \
    "$TAVERN_DIR"/public/scripts/extensions/.lukoa-extension-staging \
    "$st_parent"/*.lukoa-before-restore-* \
    "$st_parent"/.lukoa-clone-staging-* \
    "$st_data_parent"/*.lukoa-before-restore-*; do
    [ -e "$p" ] && [ ! -L "$p" ] || continue
    st_leftover_kb=$((st_leftover_kb + $(dir_kb "$p")))
    st_leftover_cnt=$((st_leftover_cnt + 1))
  done
  st_cfg_bk_kb=0
  st_cfg_bk_cnt=0
  for p in "$TAVERN_DIR"/config.yaml.lukoa-backup-*; do
    [ -f "$p" ] && [ ! -L "$p" ] || continue
    st_cfg_bk_kb=$((st_cfg_bk_kb + $(dir_kb "$p")))
    st_cfg_bk_cnt=$((st_cfg_bk_cnt + 1))
  done
  printf "\n==== Lukoa storage ====\n"
  [ -n "$st_freed" ] && printf "storage.freedKb=%s\n" "$st_freed"
  printf "storage.tavernTotalKb=%s\n" "$st_total"
  printf "storage.dataTotalKb=%s\n" "$st_data_kb"
  printf "storage.chatsKb=%s\n" "$st_chats_kb"
  printf "storage.charactersKb=%s\n" "$st_chars_kb"
  printf "storage.thumbnailsKb=%s\n" "$st_thumbs_kb"
  printf "storage.chatBackupsKb=%s\n" "$st_backups_kb"
  printf "storage.nodeModulesKb=%s\n" "$st_nm_kb"
  printf "storage.extensionsKb=%s\n" "$st_ext_kb"
  printf "storage.extensionRollbacksKb=%s\n" "$st_ext_rb_kb"
  printf "storage.logsKb=%s\n" "$st_logs_kb"
  printf "storage.oldLogKb=%s\n" "$st_old_log_kb"
  printf "storage.npmCacheKb=%s\n" "$st_npm_cache_kb"
  printf "storage.leftoverDirsKb=%s\n" "$st_leftover_kb"
  printf "storage.leftoverDirsCount=%s\n" "$st_leftover_cnt"
  printf "storage.configBackupsKb=%s\n" "$st_cfg_bk_kb"
  printf "storage.configBackupsCount=%s\n" "$st_cfg_bk_cnt"
  printf "==== end Lukoa storage ====\n"
}

cmd_storage_scan() {
  write_status "storage-scan" "Storage usage scanned" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_storage_snapshot
}

cmd_storage_clean() {
  cats="${1:-}"
  [ -n "$cats" ] || { write_status "error" "No cleanup categories selected" "$(tavern_running_value)" 64; cat "$STATUS_FILE"; return 64; }
  freed=0
  st_data="$(resolve_tavern_data_root)"
  st_parent="$(dirname "$TAVERN_DIR")"
  st_data_parent="$(dirname "$st_data")"
  old_ifs="$IFS"
  IFS=','
  for cat_key in $cats; do
    IFS="$old_ifs"
    case "$cat_key" in
      old_logs)
        if [ -f "$LOG_FILE.old" ] && [ ! -L "$LOG_FILE.old" ]; then
          freed=$((freed + $(dir_kb "$LOG_FILE.old")))
          rm -f "$LOG_FILE.old" 2>/dev/null || true
        fi
        ;;
      npm_cache)
        if [ -d "$HOME_DIR/.npm" ] && [ ! -L "$HOME_DIR/.npm" ]; then
          freed=$((freed + $(dir_kb "$HOME_DIR/.npm")))
          rm -rf "$HOME_DIR/.npm" 2>/dev/null || true
        fi
        ;;
      thumbnails)
        if [ -d "$st_data" ]; then
          for d in "$st_data"/*/thumbnails "$st_data/thumbnails"; do
            if [ -d "$d" ] && [ ! -L "$d" ]; then
              freed=$((freed + $(dir_kb "$d")))
              rm -rf "$d"/* 2>/dev/null || true
            fi
          done
        fi
        ;;
      extension_rollbacks)
        rb_dir="$TAVERN_DIR/public/scripts/extensions/.lukoa-extension-rollbacks"
        if [ -d "$rb_dir" ] && [ ! -L "$rb_dir" ]; then
          freed=$((freed + $(dir_kb "$rb_dir")))
          rm -rf "$rb_dir" 2>/dev/null || true
        fi
        ;;
      leftover_dirs)
        for p in \
          "$TAVERN_DIR"/.lukoa-before-repair-* \
          "$TAVERN_DIR"/public/scripts/extensions/.lukoa-extension-staging \
          "$st_parent"/*.lukoa-before-restore-* \
          "$st_parent"/.lukoa-clone-staging-* \
          "$st_data_parent"/*.lukoa-before-restore-*; do
          if [ -e "$p" ] && [ ! -L "$p" ]; then
            freed=$((freed + $(dir_kb "$p")))
            rm -rf "$p" 2>/dev/null || true
          fi
        done
        ;;
      config_backups)
        for p in "$TAVERN_DIR"/config.yaml.lukoa-backup-*; do
          if [ -f "$p" ] && [ ! -L "$p" ]; then
            freed=$((freed + $(dir_kb "$p")))
            rm -f "$p" 2>/dev/null || true
          fi
        done
        ;;
      *)
        write_status "error" "Unsupported cleanup category: $cat_key" "$(tavern_running_value)" 64
        cat "$STATUS_FILE"
        return 64
        ;;
    esac
  done
  IFS="$old_ifs"
  write_status "storage-clean" "Safe storage cleanup completed" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_storage_snapshot "$freed"
}

run_tavern_server_plugins_action() {
  sp_action="$1"
  sp_payload="${2:-}"
  [ -d "$TAVERN_DIR" ] || {
    write_status "error" "SillyTavern directory not found: $TAVERN_DIR" false 66
    cat "$STATUS_FILE"
    return 66
  }
  command -v node >/dev/null 2>&1 || {
    write_status "error" "node command not found in Termux" "$(tavern_running_value)" 69
    cat "$STATUS_FILE"
    return 69
  }
  sp_out="$(
    LUKOA_SP_ACTION="$sp_action" \
    LUKOA_SP_PAYLOAD="$sp_payload" \
    LUKOA_TAVERN_DIR="$TAVERN_DIR" \
    node -e '
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");
const action = process.env.LUKOA_SP_ACTION || "list";
const payload = process.env.LUKOA_SP_PAYLOAD || "";
const tavernDir = process.env.LUKOA_TAVERN_DIR || "";
const pluginsRoot = path.join(tavernDir, "plugins");
const fail = (m, c = 65) => { console.error(m); process.exit(c); };
const b64 = (v) => Buffer.from(String(v ?? ""), "utf8").toString("base64url");
const dec = (v) => Buffer.from(v, "base64url").toString("utf8");
const safeDir = (n) => {
  if (!n || n === "." || n === ".." || n.startsWith(".") || !/^[A-Za-z0-9._-]+$/.test(n)) fail("Invalid plugin directory name", 64);
  const root = fs.realpathSync(pluginsRoot);
  const t = path.resolve(root, n);
  if (path.dirname(t) !== root) fail("Plugin target escaped plugins root", 64);
  return t;
};
const runCmd = (cmd, args, cwd, timeout = 180000) => spawnSync(cmd, args, { encoding: "utf8", cwd, timeout, maxBuffer: 2 * 1024 * 1024, env: { ...process.env, GIT_TERMINAL_PROMPT: "0" } });
const dirKb = (dir) => {
  let bytes = 0, cnt = 0;
  const q = [dir];
  while (q.length && cnt < 20000) {
    const cur = q.pop();
    let es;
    try { es = fs.readdirSync(cur, { withFileTypes: true }); } catch (_) { continue; }
    for (const e of es) {
      cnt++;
      if (e.isSymbolicLink()) continue;
      const p = path.join(cur, e.name);
      if (e.isDirectory()) q.push(p);
      else if (e.isFile()) { try { bytes += fs.statSync(p).size; } catch (_) {} }
    }
  }
  return Math.ceil(bytes / 1024);
};

if (!fs.existsSync(pluginsRoot) && action === "install") fs.mkdirSync(pluginsRoot, { recursive: true });
if (action === "install") {
  const url = dec(payload).trim();
  if (!/^https:\/\/[^\s]+$/i.test(url)) fail("Invalid plugin repository URL", 64);
  const rawName = url.replace(/\/+$/, "").split("/").pop().replace(/\.git$/i, "");
  const target = safeDir(rawName);
  if (fs.existsSync(target)) fail("Plugin directory already exists", 73);
  const stage = path.join(pluginsRoot, `.lukoa-plugin-stage-${process.pid}-${Date.now()}`);
  const cl = runCmd("git", ["clone", "--depth", "1", url, stage], pluginsRoot);
  if (cl.error || cl.status !== 0) {
    try { fs.rmSync(stage, { recursive: true, force: true }); } catch (_) {}
    fail("Plugin git clone failed: " + String(cl.stderr || "").slice(-400), cl.status || 74);
  }
  if (!fs.existsSync(path.join(stage, "index.js")) && !fs.existsSync(path.join(stage, "package.json"))) {
    try { fs.rmSync(stage, { recursive: true, force: true }); } catch (_) {}
    fail("Repository does not look like a SillyTavern server plugin (missing index.js or package.json)", 65);
  }
  if (fs.existsSync(path.join(stage, "package.json"))) {
    try {
      const pkg = JSON.parse(fs.readFileSync(path.join(stage, "package.json"), "utf8"));
      if (pkg && pkg.dependencies && Object.keys(pkg.dependencies).length > 0) {
        runCmd("npm", ["install", "--omit=dev", "--no-audit", "--no-fund"], stage, 300000);
      }
    } catch (_) {}
  }
  fs.renameSync(stage, target);
} else if (action === "update") {
  const target = safeDir(dec(payload).trim());
  const st = fs.lstatSync(target);
  if (st.isSymbolicLink() || !st.isDirectory()) fail("Plugin target is not a regular directory", 64);
  const dirty = runCmd("git", ["status", "--porcelain"], target, 10000);
  if (dirty.status !== 0) fail("Plugin is not a Git repository", 65);
  if (String(dirty.stdout || "").trim()) fail("Plugin has local changes", 78);
  const pull = runCmd("git", ["pull", "--ff-only"], target);
  if (pull.error || pull.status !== 0) fail("Plugin update failed: " + String(pull.stderr || "").slice(-400), pull.status || 74);
  if (fs.existsSync(path.join(target, "package.json"))) {
    try {
      const pkg = JSON.parse(fs.readFileSync(path.join(target, "package.json"), "utf8"));
      if (pkg && pkg.dependencies && Object.keys(pkg.dependencies).length > 0) {
        runCmd("npm", ["install", "--omit=dev", "--no-audit", "--no-fund"], target, 300000);
      }
    } catch (_) {}
  }
} else if (action === "delete") {
  const target = safeDir(dec(payload).trim());
  const st = fs.lstatSync(target);
  if (st.isSymbolicLink() || !st.isDirectory()) fail("Plugin target is not a regular directory", 64);
  fs.rmSync(target, { recursive: true, force: false });
}

let enabledInConfig = false;
try {
  const cfgPath = path.join(tavernDir, "config.yaml");
  if (fs.existsSync(cfgPath)) {
    const raw = fs.readFileSync(cfgPath, "utf8");
    enabledInConfig = /^\s*enableServerPlugins\s*:\s*true\b/m.test(raw);
  }
} catch (_) {}

console.log("==== SillyTavern server plugins ====");
console.log("plugin.root=" + b64(pluginsRoot));
console.log("plugin.enabledInConfig=" + enabledInConfig);
if (fs.existsSync(pluginsRoot)) {
  for (const e of fs.readdirSync(pluginsRoot, { withFileTypes: true })) {
    if (!e.isDirectory() || e.isSymbolicLink() || e.name.startsWith(".")) continue;
    const p = path.join(pluginsRoot, e.name);
    let name = e.name, ver = "", desc = "";
    const pkgPath = path.join(p, "package.json");
    if (fs.existsSync(pkgPath)) {
      try {
        const pkg = JSON.parse(fs.readFileSync(pkgPath, "utf8"));
        if (pkg.name) name = String(pkg.name);
        if (pkg.version) ver = String(pkg.version);
        if (pkg.description) desc = String(pkg.description);
      } catch (_) {}
    }
    const revRes = runCmd("git", ["rev-parse", "--short", "HEAD"], p, 3000);
    const rev = revRes.status === 0 ? String(revRes.stdout || "").trim() : "";
    const repoRes = rev ? runCmd("git", ["remote", "get-url", "origin"], p, 3000) : { status: 1 };
    const repo = repoRes.status === 0 ? String(repoRes.stdout || "").trim() : "";
    console.log(`plugin.record=${b64(e.name)}|${b64(name)}|${b64(ver)}|${b64(desc)}|${dirKb(p)}|${b64(repo)}|${rev}|${rev ? "true" : "false"}`);
  }
}
console.log("==== end SillyTavern server plugins ====");
' 2>&1
  )"
  sp_code="$?"
  if [ "$sp_code" -ne 0 ]; then
    write_status "error" "SillyTavern server plugin $sp_action failed" "$(tavern_running_value)" "$sp_code"
    cat "$STATUS_FILE"
    printf "\n%s\n" "$sp_out"
    return "$sp_code"
  fi
  write_status "server-plugins-$sp_action" "SillyTavern server plugin $sp_action completed" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n%s\n" "$sp_out"
}

emit_git_stash_list() {
  printf "\n==== SillyTavern git stash ====\n"
  if [ -d "$TAVERN_DIR/.git" ] && command -v git >/dev/null 2>&1; then
    printf "stash.available=true\n"
    git -C "$TAVERN_DIR" stash list --format='%gd	%gs	%ci' 2>/dev/null | while IFS="$(printf '\t')" read -r s_ref s_msg s_date; do
      [ -n "$s_ref" ] || continue
      printf "stash.entry=%s\t%s\t%s\n" "$s_ref" "$s_msg" "$s_date"
    done
  else
    printf "stash.available=false\n"
  fi
  printf "==== end SillyTavern git stash ====\n"
}

cmd_git_stash_list() {
  write_status "git-stash-list" "Git stash list collected" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_git_stash_list
}

cmd_git_stash_apply() {
  if is_running_quick; then
    write_status "error" "Please stop SillyTavern before applying a git stash" true 77
    cat "$STATUS_FILE"
    return 77
  fi
  idx="${1:-}"
  is_int_in_range "$idx" 0 999 || { write_status "error" "Invalid stash index" false 64; cat "$STATUS_FILE"; return 64; }
  [ -d "$TAVERN_DIR/.git" ] || { write_status "error" "Not a Git repository" false 65; cat "$STATUS_FILE"; return 65; }
  if ! git -C "$TAVERN_DIR" stash apply "stash@{$idx}" 2>&1; then
    write_status "error" "Failed to apply git stash (conflicts may exist)" false 74
    cat "$STATUS_FILE"
    emit_git_stash_list
    return 74
  fi
  write_status "git-stash-apply" "Git stash applied (entry kept in stash list)" false 0
  cat "$STATUS_FILE"
  emit_git_stash_list
}

cmd_git_stash_drop() {
  idx="${1:-}"
  is_int_in_range "$idx" 0 999 || { write_status "error" "Invalid stash index" "$(tavern_running_value)" 64; cat "$STATUS_FILE"; return 64; }
  [ -d "$TAVERN_DIR/.git" ] || { write_status "error" "Not a Git repository" "$(tavern_running_value)" 65; cat "$STATUS_FILE"; return 65; }
  if ! git -C "$TAVERN_DIR" stash drop "stash@{$idx}" 2>&1; then
    write_status "error" "Failed to drop git stash" "$(tavern_running_value)" 74
    cat "$STATUS_FILE"
    return 74
  fi
  write_status "git-stash-drop" "Git stash entry dropped" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_git_stash_list
}

cmd_quick_import_paths() {
  qi_data="$(resolve_tavern_data_root)"
  qi_user="${1:-default-user}"
  case "$qi_user" in ''|*[!A-Za-z0-9._-]*) qi_user="default-user" ;; esac
  qi_base="$qi_data/$qi_user"
  [ -d "$qi_base" ] || qi_base="$qi_data"
  qi_chat_chars_b64=""
  if [ -d "$qi_base/chats" ] && command -v node >/dev/null 2>&1; then
    qi_chat_chars_b64="$(
      LUKOA_CHATS_DIR="$qi_base/chats" node -e '
const fs = require("fs");
try {
  const names = fs.readdirSync(process.env.LUKOA_CHATS_DIR, { withFileTypes: true })
    .filter(e => e.isDirectory() && !e.isSymbolicLink() && !e.name.startsWith("."))
    .map(e => e.name)
    .sort((a, b) => a.localeCompare(b))
    .slice(0, 200);
  process.stdout.write(Buffer.from(names.join("\n"), "utf8").toString("base64url"));
} catch (_) {}
' 2>/dev/null || true
    )"
  fi
  write_status "quick-import-paths" "Quick import directories resolved" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern quick import paths ====\n"
  printf "import.dataRoot=%s\n" "$qi_data"
  printf "import.userRoot=%s\n" "$qi_base"
  printf "import.dir.character=%s\n" "$qi_base/characters"
  printf "import.dir.world=%s\n" "$qi_base/worlds"
  printf "import.dir.preset=%s\n" "$qi_base/OpenAI Settings"
  printf "import.dir.theme=%s\n" "$qi_base/themes"
  printf "import.dir.chats=%s\n" "$qi_base/chats"
  [ -n "$qi_chat_chars_b64" ] && printf "import.chatChars=%s\n" "$qi_chat_chars_b64"
  printf "==== end SillyTavern quick import paths ====\n"
}

cmd_quick_import_batch() {
  qi_kind="${1:-}"
  qi_paths_b64="${2:-}"
  qi_user="${3:-default-user}"
  qi_chat_char_b64="${4:-}"
  case "$qi_user" in ''|*[!A-Za-z0-9._-]*) qi_user="default-user" ;; esac
  command -v node >/dev/null 2>&1 || {
    write_status "error" "node command not found in Termux" "$(tavern_running_value)" 69
    cat "$STATUS_FILE"
    return 69
  }
  qi_data="$(resolve_tavern_data_root)"
  qi_base="$qi_data/$qi_user"
  [ -d "$qi_base" ] || qi_base="$qi_data"
  qi_out="$(
    LUKOA_QI_KIND="$qi_kind" \
    LUKOA_QI_PATHS_B64="$qi_paths_b64" \
    LUKOA_QI_BASE="$qi_base" \
    LUKOA_QI_CHAT_CHAR_B64="$qi_chat_char_b64" \
    LUKOA_HOME_DIR="$HOME_DIR" \
    node -e '
const fs = require("fs");
const path = require("path");
const fail = (m, c = 64) => { console.error(m); process.exit(c); };
const b64 = (v) => Buffer.from(String(v ?? ""), "utf8").toString("base64url");
const dec = (v) => v ? Buffer.from(v, "base64url").toString("utf8") : "";

const kind = process.env.LUKOA_QI_KIND || "";
const base = process.env.LUKOA_QI_BASE || "";
const homeDir = process.env.LUKOA_HOME_DIR || "";
const subMap = {
  character: "characters",
  world: "worlds",
  preset: "OpenAI Settings",
  theme: "themes",
  chat: "chats",
};
if (!subMap[kind]) fail("Unsupported import kind: " + kind, 64);

let destDir = path.join(base, subMap[kind]);
if (kind === "chat") {
  const charName = dec(process.env.LUKOA_QI_CHAT_CHAR_B64 || "").trim();
  if (!charName || charName === "." || charName === ".." || charName.length > 120 || /[\/\\\0\r\n]/.test(charName)) {
    fail("Invalid chat target character folder", 64);
  }
  destDir = path.join(destDir, charName);
}

let rawPaths;
try {
  rawPaths = dec(process.env.LUKOA_QI_PATHS_B64 || "")
    .split(/\r?\n/)
    .map(s => s.trim())
    .filter(Boolean);
} catch (_) {
  fail("Invalid staged paths payload", 64);
}
if (rawPaths.length === 0 || rawPaths.length > 50) fail("Staged file count out of range", 64);

const normHome = homeDir.replace(/\\/g, "/").replace(/\/+$/, "");
const allowedPrefixes = [
  "/storage/emulated/0/Download/LukoaLauncher/imports/",
  `${normHome}/storage/downloads/LukoaLauncher/imports/`,
];

fs.mkdirSync(destDir, { recursive: true });
const pad = (n) => String(n).padStart(2, "0");
const now = new Date();
const stamp = `${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;

const imported = [];
for (const rawPath of rawPaths) {
  const normRaw = rawPath.replace(/\\/g, "/");
  if (normRaw.includes("..") || !allowedPrefixes.some(p => normRaw.startsWith(p))) {
    fail("Staged import file is outside LukoaLauncher/imports", 64);
  }
  let src = rawPath;
  const isRegular = (p) => {
    try { const st = fs.lstatSync(p); return st.isFile() && !st.isSymbolicLink(); } catch (_) { return false; }
  };
  if (!isRegular(src)) {
    const alt = path.join(homeDir, "storage", "downloads", "LukoaLauncher", "imports", path.posix.basename(normRaw));
    if (isRegular(alt)) src = alt;
    else fail("termux-storage-permission: staged import file cannot be read", 73);
  }
  const origName = path.posix.basename(normRaw);
  let dest = path.join(destDir, origName);
  if (fs.existsSync(dest)) {
    const ext = path.extname(origName);
    const stem = ext ? origName.slice(0, -ext.length) : origName;
    let attempt = 0;
    do {
      const suffix = attempt === 0 ? ` (导入 ${stamp})` : ` (导入 ${stamp}-${attempt})`;
      dest = path.join(destDir, `${stem}${suffix}${ext}`);
      attempt++;
    } while (fs.existsSync(dest) && attempt < 1000);
  }
  fs.copyFileSync(src, dest);
  try { fs.rmSync(src, { force: true }); } catch (_) {}
  imported.push(dest);
}

console.log("==== SillyTavern quick import ====");
console.log("import.kind=" + kind);
console.log("import.count=" + imported.length);
console.log("import.targetFile=" + imported[imported.length - 1]);
console.log("import.targetFiles=" + b64(imported.join("\n")));
console.log("==== end SillyTavern quick import ====");
' 2>&1
  )"
  qi_code="$?"
  if [ "$qi_code" -ne 0 ]; then
    write_status "error" "Quick import batch failed" "$(tavern_running_value)" "$qi_code"
    cat "$STATUS_FILE"
    printf "\n%s\n" "$qi_out"
    return "$qi_code"
  fi
  write_status "quick-import" "Files imported into SillyTavern successfully" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n%s\n" "$qi_out"
}

read_node_memory_mb() {
  [ -f "$NODE_MEMORY_FILE" ] && [ ! -L "$NODE_MEMORY_FILE" ] || return 1
  nm_val="$(awk -F= '/^LUKOA_NODE_MEMORY_MB=/ { sub(/^[^=]*=/, ""); gsub(/['\''"[:space:]]/, ""); print; exit }' "$NODE_MEMORY_FILE" 2>/dev/null || true)"
  is_int_in_range "$nm_val" 512 16384 || return 1
  printf "%s" "$nm_val"
}

emit_node_memory_status() {
  nm_cur="$(read_node_memory_mb || true)"
  printf "\n==== Lukoa node memory ====\n"
  if [ -n "$nm_cur" ]; then
    printf "nodeMemory.configured=true\n"
    printf "nodeMemory.megabytes=%s\n" "$nm_cur"
  else
    printf "nodeMemory.configured=false\n"
    printf "nodeMemory.megabytes=\n"
  fi
  printf "==== end Lukoa node memory ====\n"
}

cmd_node_memory_status() {
  write_status "node-memory-status" "Node.js memory limit status collected" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_node_memory_status
}

cmd_node_memory_reset() {
  if [ -f "$NODE_MEMORY_FILE" ] && [ ! -L "$NODE_MEMORY_FILE" ]; then
    rm -f "$NODE_MEMORY_FILE" || {
      write_status "error" "Failed to remove Node.js memory configuration file" "$(tavern_running_value)" 74
      cat "$STATUS_FILE"
      return 74
    }
  fi
  write_status "node-memory-reset" "Node.js memory limit reset to default" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  emit_node_memory_status
}

cmd_proxy_test() {
  pt_payload="${1:-}"
  command -v node >/dev/null 2>&1 || {
    write_status "error" "node command not found in Termux" "$(tavern_running_value)" 69
    cat "$STATUS_FILE"
    return 69
  }
  pt_out="$(
    LUKOA_PROXY_B64="$pt_payload" node -e '
const net = require("net");
const { spawnSync } = require("child_process");
const b64 = (v) => Buffer.from(String(v ?? ""), "utf8").toString("base64url");
let proxyUrl = "";
try {
  proxyUrl = Buffer.from(process.env.LUKOA_PROXY_B64 || "", "base64url").toString("utf8").trim();
} catch (_) {}
if (!/^(https?|socks4a?|socks5h?):\/\/[^\s]+$/i.test(proxyUrl)) {
  console.log("==== Lukoa proxy test ====");
  console.log("proxy.ok=false");
  console.log("proxy.tcpOk=false");
  console.log("proxy.detail=" + b64("代理地址格式不正确"));
  console.log("==== end Lukoa proxy test ====");
  process.exit(0);
}
let parsed;
try { parsed = new URL(proxyUrl); } catch (_) {
  console.log("==== Lukoa proxy test ====");
  console.log("proxy.ok=false");
  console.log("proxy.tcpOk=false");
  console.log("proxy.detail=" + b64("无法解析代理主机与端口"));
  console.log("==== end Lukoa proxy test ====");
  process.exit(0);
}
const host = parsed.hostname;
const defaultPort = parsed.protocol === "https:" ? 443 : (parsed.protocol.startsWith("socks") ? 1080 : 80);
const port = parsed.port ? parseInt(parsed.port, 10) : defaultPort;
const t0 = Date.now();

const emit = (ok, tcpOk, httpCode, latencyMs, detail) => {
  console.log("==== Lukoa proxy test ====");
  console.log("proxy.ok=" + (ok ? "true" : "false"));
  console.log("proxy.tcpOk=" + (tcpOk ? "true" : "false"));
  if (httpCode) console.log("proxy.httpCode=" + httpCode);
  if (latencyMs >= 0) console.log("proxy.latencyMs=" + latencyMs);
  console.log("proxy.detail=" + b64(detail));
  console.log("==== end Lukoa proxy test ====");
};

const sock = net.createConnection({ host, port });
let done = false;
const finishTcp = (tcpOk, errMsg) => {
  if (done) return;
  done = true;
  try { sock.destroy(); } catch (_) {}
  const tcpLatency = Date.now() - t0;
  if (!tcpOk) {
    emit(false, false, "", tcpLatency, `无法连接到代理端口 ${host}:${port}（${errMsg || "连接被拒绝或超时"}）。请确认代理软件已启动。`);
    return;
  }
  const hasCurl = spawnSync("sh", ["-c", "command -v curl >/dev/null 2>&1"]).status === 0;
  if (!hasCurl) {
    emit(true, true, "", tcpLatency, `代理端口 ${host}:${port} TCP 可达（耗时 ${tcpLatency}ms）。`);
    return;
  }
  const targets = ["https://cp.cloudflare.com/generate_204", "http://connectivitycheck.gstatic.com/generate_204"];
  for (const target of targets) {
    const cStart = Date.now();
    const res = spawnSync("curl", ["-sS", "--proxy", proxyUrl, "--connect-timeout", "6", "--max-time", "10", "-o", "/dev/null", "-w", "%{http_code}", target], { encoding: "utf8", timeout: 12000 });
    const code = String(res.stdout || "").trim();
    const ms = Date.now() - cStart;
    if (res.status === 0 && /^[1-5][0-9][0-9]$/.test(code)) {
      emit(true, true, code, ms, `代理通道连接正常（HTTP ${code}，耗时 ${ms}ms）。`);
      return;
    }
  }
  emit(false, true, "", tcpLatency, `代理端口 ${host}:${port} 可达，但通过代理访问测试节点失败，请检查代理协议或上游节点是否可用。`);
};
sock.setTimeout(3500, () => finishTcp(false, "连接超时"));
sock.on("connect", () => finishTcp(true, ""));
sock.on("error", (err) => finishTcp(false, err && err.code ? err.code : "连接失败"));
' 2>&1
  )"
  write_status "proxy-test" "Proxy connectivity test completed" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n%s\n" "$pt_out"
}

cmd_quick_import() {
  qi_kind="${1:-}"
  qi_src="${2:-}"
  qi_user="${3:-default-user}"
  case "$qi_kind" in
    character) qi_sub="characters" ;;
    world) qi_sub="worlds" ;;
    preset) qi_sub="OpenAI Settings" ;;
    theme) qi_sub="themes" ;;
    *) write_status "error" "Unsupported import kind: $qi_kind" "$(tavern_running_value)" 64; cat "$STATUS_FILE"; return 64 ;;
  esac
  case "$qi_user" in ''|*[!A-Za-z0-9._-]*) qi_user="default-user" ;; esac
  case "$qi_src" in
    "/storage/emulated/0/Download/LukoaLauncher/imports/"*|"$HOME_DIR/storage/downloads/LukoaLauncher/imports/"*) ;;
    *) write_status "error" "Staged import file is outside LukoaLauncher/imports" "$(tavern_running_value)" 64; cat "$STATUS_FILE"; return 64 ;;
  esac
  if [ ! -f "$qi_src" ] || [ -L "$qi_src" ]; then
    alt_src="$HOME_DIR/storage/downloads/LukoaLauncher/imports/$(basename "$qi_src")"
    if [ -f "$alt_src" ] && [ ! -L "$alt_src" ]; then
      qi_src="$alt_src"
    else
      write_status "error" "termux-storage-permission: staged import file cannot be read" "$(tavern_running_value)" 73
      cat "$STATUS_FILE"
      return 73
    fi
  fi
  qi_data="$(resolve_tavern_data_root)"
  qi_base="$qi_data/$qi_user"
  [ -d "$qi_base" ] || qi_base="$qi_data"
  qi_dest_dir="$qi_base/$qi_sub"
  mkdir -p "$qi_dest_dir" || { write_status "error" "Failed to create import target directory" "$(tavern_running_value)" 74; cat "$STATUS_FILE"; return 74; }
  qi_name="$(basename "$qi_src")"
  qi_dest="$qi_dest_dir/$qi_name"
  if [ -e "$qi_dest" ]; then
    qi_stamp="$(date +%m%d-%H%M%S 2>/dev/null || printf import)"
    case "$qi_name" in
      *.*) qi_dest="$qi_dest_dir/${qi_name%.*} (导入 $qi_stamp).${qi_name##*.}" ;;
      *) qi_dest="$qi_dest_dir/$qi_name (导入 $qi_stamp)" ;;
    esac
  fi
  cp "$qi_src" "$qi_dest" || { write_status "error" "Failed to copy file into SillyTavern directory" "$(tavern_running_value)" 74; cat "$STATUS_FILE"; return 74; }
  rm -f "$qi_src" 2>/dev/null || true
  write_status "quick-import" "File imported into SillyTavern successfully" "$(tavern_running_value)" 0
  cat "$STATUS_FILE"
  printf "\n==== SillyTavern quick import ====\n"
  printf "import.kind=%s\n" "$qi_kind"
  printf "import.targetFile=%s\n" "$qi_dest"
  printf "==== end SillyTavern quick import ====\n"
}

main() {
  cmd="${1:-}"
  case "$cmd" in
    config-read|tavern-config-read) run_tavern_config_action read "" ;;
    config-set|tavern-config-set) shift; run_tavern_config_action set "${1:-}" ;;
    storage-scan|tavern-storage-scan) cmd_storage_scan ;;
    storage-clean|tavern-storage-clean) shift; cmd_storage_clean "${1:-}" ;;
    server-plugins-list|tavern-server-plugins-list) run_tavern_server_plugins_action list "" ;;
    server-plugins-install|tavern-server-plugins-install) shift; run_tavern_server_plugins_action install "${1:-}" ;;
    server-plugins-update|tavern-server-plugins-update) shift; run_tavern_server_plugins_action update "${1:-}" ;;
    server-plugins-delete|tavern-server-plugins-delete) shift; run_tavern_server_plugins_action delete "${1:-}" ;;
    git-stash-list|tavern-git-stash-list) cmd_git_stash_list ;;
    git-stash-apply|tavern-git-stash-apply) shift; cmd_git_stash_apply "${1:-}" ;;
    git-stash-drop|tavern-git-stash-drop) shift; cmd_git_stash_drop "${1:-}" ;;
    quick-import-paths|tavern-quick-import-paths) shift; cmd_quick_import_paths "${1:-default-user}" ;;
    quick-import|tavern-quick-import) shift; cmd_quick_import "$@" ;;
    quick-import-batch|tavern-quick-import-batch) shift; cmd_quick_import_batch "$@" ;;
    node-memory-status|tavern-node-memory-status) cmd_node_memory_status ;;
    node-memory-reset|tavern-node-memory-reset) cmd_node_memory_reset ;;
    proxy-test|tavern-proxy-test) shift; cmd_proxy_test "${1:-}" ;;
    *) write_status "error" "unknown ops command: $cmd" false 64; cat "$STATUS_FILE"; return 64 ;;
  esac
}

main "$@"
