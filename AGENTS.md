# AGENTS.md

给进入这个仓库的 AI 或新维护者看的项目速览。这里写的是事实和背景，不是清单式的禁令；遇到拿不准的地方，直接问仓库所有者。

## 项目是什么

- 露科亚启动器：Android App，Kotlin + Jetpack Compose，包名 `moe.lukoa.launcher`。
- 用途：让用户在手机上更顺手地使用 Termux + SillyTavern（酒馆）。App 负责界面、状态、版本管理、备份恢复、更新检测、诊断导出；真正的 shell 命令由 `app/src/main/assets/lukoa-tavern.sh` 通过 Termux 的 `RUN_COMMAND` 执行。
- 当前主线：`main` 分支，最新正式版 1.0（2026-08-11）。历史版本见 `CHANGELOG.md` 和 GitHub Release。
- 许可证：PolyForm Noncommercial 1.0.0。
- 面向用户的文案默认简体中文，目标读者是新手。公开内容（README、Release、Issue 回复）里不放私人联系方式。

## 仓库结构

```text
app/src/main/java/moe/lukoa/launcher/   全部 Kotlin 源码（单包，按文件名前缀分组）
app/src/main/assets/lukoa-tavern.sh     下发到 Termux 的核心脚本（约 4500 行）
app/src/test/java/...                   JVM 单元测试 + Robolectric Compose UI 测试
docs/project-management/                主线状态、AI 协作记录、session-notes 交接
docs/development/repository-map.md      仓库结构与阅读顺序
tools/simulate-tavern-script.sh         在 PC / CI 上用假酒馆仓库跑 lukoa-tavern.sh 的更新、回退链路
build-debug.ps1                         本地构建 debug APK
publish-github-release.ps1              改版本号 -> 构建 -> 提交 -> tag -> 推送 -> 发 Release
generate-release-notes.ps1              从 CHANGELOG 汇总稳定版公告
```

源码文件按前缀大致分组：

| 前缀 | 职责 |
| --- | --- |
| `LukoaLauncherScreen` | 页面级状态编排与事件分发（最大的文件，约 4000 行） |
| `Launcher*Section` / `*Section` | 各页面 UI：启动、版本管理、设置、备份、工具箱、文档、修复工具、扩展/用户管理 |
| `Launcher*Dialogs` / `SettingsDialogs` | 各类弹窗 |
| `LauncherNavigation` / `SectionSwitcher` | 底部导航与页面切换 |
| `Tavern*` | 酒馆相关：路径、实例档案、版本、镜像源、体检、扩展、上传限制 |
| `Termux*` | Termux 命令构建、执行、结果解析、日志展示、唤醒策略 |
| `Backup*` / `AutoBackup*` | 手动/自动备份、恢复预览、备份内容检查 |
| `*Policy` / `*Guard` / `*Codec` / `*Parser` / `*Store` | 纯逻辑，基本都有对应单元测试 |
| `GithubUpdate*` | 启动器自身的 APK 更新检测与安装 |

## 一些约定和它们的原因

- **新逻辑优先放进现有前缀分组的独立文件**，而不是继续加进 `LukoaLauncherScreen.kt` 或 `TermuxCommandRunner.kt`。这两个文件已经很大，继续膨胀会让改动难以验证。
- **纯逻辑拆成独立文件并配单元测试**。项目里已有 100 多个测试类，新逻辑跟着补一个成本很低。
- **能在 App 侧做的判断就在 App 侧做**。每次进 Termux 都有明显延迟，把校验、状态判断留在 App 里体验更好。
- **酒馆路径默认 `~/SillyTavern`，允许用户手填**，App 不自动改路径。
- **"版本管理"指 SillyTavern 版本**，不是启动器自身版本；"自动读取当前酒馆版本"是用户依赖的功能。
- **备份/恢复、停止、回退、更新、应用备份属于高风险操作**，改动时优先保证数据不丢、确认提示清楚。
- 发版时 `app/build.gradle.kts` 里的 `versionName` 和 `versionCode` 一起改；测试版发 prerelease。

## 本地构建与验证

环境要求：JDK 17–22（Gradle 8.8 不支持 JDK 25，本机 Android Studio 自带的 JBR 是 25，需要另装或指定 JDK），Android SDK 需要 `platforms;android-35` 与 `build-tools;35.0.0`（首次构建时 AGP 会自动下载）。`local.properties` 已在 `.gitignore` 里，写入 `sdk.dir` 即可。

```powershell
# 编译 + 单元测试 + lint + debug APK
.\gradlew.bat --no-daemon :app:compileDebugKotlin testDebugUnitTest :app:lintDebug :app:assembleDebug

# 覆盖率报告 -> app/build/reports/jacoco/debugUnitTest/html/index.html
.\gradlew.bat --no-daemon :app:jacocoDebugUnitTestReport

# 只构建 debug APK
powershell -ExecutionPolicy Bypass -File .\build-debug.ps1 -AndroidHome "SDK 路径"

# 改了 lukoa-tavern.sh 的更新 / 回退相关逻辑时，再跑脚本沙盒（Windows 在 Git Bash 里跑；需要 git、node、npm）
bash tools/simulate-tavern-script.sh
```

脚本沙盒会在临时目录里造一个带 `1.13.0`、`1.14.0` 两个 tag 的假 SillyTavern 仓库，真实执行 `upload-limit-set`、`update`、`rollback`，检查启动器托管文件不拦截、用户改动在 `keep` 下拦截、在 `discard` 下进入命名 stash、上传限制补丁在切版本后补回等约定。CI 的 `tavern-script-simulation` job 每次推送都会跑；本地失败时会保留沙盒目录并打印失败步骤的完整输出，加 `--keep` 可以在成功时也保留。

Windows 提示：如果 Gradle 测试进程报 `ClassNotFoundException: Files\NVIDIA` 之类的奇怪错误，先检查系统 `Path` 里有没有多余的英文引号。

## 发版

```powershell
powershell -ExecutionPolicy Bypass -File .\publish-github-release.ps1 `
    -VersionName 1.0.1 -VersionCode 287 `
    -ReleaseNotes "公告正文" `
    -PreRelease   # 测试版加这个
```

- 公告写法：先说用户会感受到什么变化，再说修复和注意事项；内部重构只在它确实影响体验时才提。
- 也可以用 `-ReleaseNotesFile 文件` 或 `-AutoNotes`（从 `CHANGELOG.md` 汇总稳定版）。
- 脚本目前发布的是 debug 签名的 APK；release 构建没有配置签名。切换签名会让已安装用户无法覆盖升级，改之前需要仓库所有者决定。
- 发完顺手在 `CHANGELOG.md` 顶部补一段，格式跟已有条目一致。

## 高风险区域

改这些地方时，编译通过不代表行为没变，需要额外看一眼实际行为：

- `lukoa-tavern.sh` 与 `TermuxCommandRunner.kt`、`TermuxScriptCommandBuilder.kt`（其中更新 / 回退这一段可以用 `tools/simulate-tavern-script.sh` 在 PC 上跑到真实输出，其他子命令仍要真机）
- `Backup*`、`AutoBackup*`、`LauncherBackupCoordinator.kt`
- `TavernPathConfig.kt`、`TavernProfile*`、`TavernMirrorConfig.kt`
- `TavernUploadLimit*`（会修改 SillyTavern 的中间件源码）
- `RepairToolsSection.kt` 对应的修复命令
- 发布脚本与版本号

## 已知待办

- 真机验证：依赖修复、上传限制补丁在多个 SillyTavern 版本上的行为。
- 测试覆盖率约 21%（2026-07-13 基线）。脚本的更新 / 回退链路已有 PC 沙盒回归，备份恢复、修复工具等其他高风险链路仍以人工验证为主。
- Dependabot 提出的 Gradle/Kotlin/AndroidX 升级尚未合并，需要逐个验证。
- 不同品牌手机的后台保活策略与 Termux 小窗/分屏兼容性差异。
