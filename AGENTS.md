# AGENTS.md

后续 AI 进入这个仓库后，先读本文件，再动代码或发版。

## 项目是什么

- 这是一个 Android 启动器，技术栈是 Kotlin + Jetpack Compose。
- App 负责引导、状态管理、版本管理、备份恢复、更新检测和诊断导出。
- 真正执行酒馆相关 shell 命令的是 `app/src/main/assets/lukoa-tavern.sh`，通过 Termux 调用。

## 先看哪些文件

做改动前，优先读这些文件：

- `app/src/main/java/moe/lukoa/launcher/LukoaLauncherScreen.kt`
  这里是页面级状态编排和跨模块协调入口，不要把整块新界面继续堆回这里。
- `app/src/main/java/moe/lukoa/launcher/LauncherNavigation.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherLaunchSections.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherVersionManagementSection.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherSettingsSection.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherDialogs.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherHealthCheck.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherInputGuards.kt`
- `app/src/main/java/moe/lukoa/launcher/PendingLauncherTaskSupport.kt`
- `app/src/main/java/moe/lukoa/launcher/TermuxCommandRunner.kt`
- `publish-github-release.ps1`
- `generate-release-notes.ps1`
- `CHANGELOG.md`
- `CONTRIBUTING.md`

## 项目硬规则

这些不是建议，是这个仓库后续 AI 必须遵守的工作规则。

### 1. 对用户和公开文案

- 默认使用简体中文。
- 默认把用户当新手，文案先追求清楚、少误导、少术语。
- 公开文案、Release 文案、README、Issue 回复里禁止出现旧身份、私人邮箱、QQ 等私人信息。
- 更新公告必须像正常 App 的公告，优先写用户会感受到什么，不要把“重构了什么文件、拆了什么组件”当主内容。
- 除非用户明确要看内部实现，否则不要把纯内部重构写成公开更新重点。

### 2. 模块化和代码组织

- 新功能默认先找现有模块承接，不要把大块 UI 或业务继续塞进 `LukoaLauncherScreen.kt`。
- 启动页、设置页、版本管理页、对话框、导航栏已经拆开，新增界面优先继续按这个边界放。
- 纯逻辑优先拆到独立文件：例如 parser、codec、guard、state support、manager，不要让 Compose 页面直接塞满解析逻辑。
- 新增纯逻辑后，默认补单元测试。
- 能放 App 侧的防呆、校验、状态判断，优先放 App 侧；不要把本来可以在 App 完成的逻辑都丢给 Termux 脚本。

### 3. 业务约束

- 酒馆路径不要自动乱改，默认路径是 `~/SillyTavern`，允许用户手填。
- “版本管理”默认指的是 SillyTavern 版本，不是启动器版本。
- “自动读取当前酒馆版本”必须保留。
- 备份和恢复是高风险区，优先避免数据丢失，其次才是流程省事。
- 涉及停止、恢复、回退、更新、应用备份这类危险操作时，要优先保证确认逻辑清楚一致。

### 4. 发布规则

- 每做完一个功能或修完一个明确问题，默认发一个测试版。
- 改发版相关内容时，必须同时更新 `app/build.gradle.kts` 里的 `versionName` 和 `versionCode`。
- 发版前必须确认：
  - GitHub Release 文案是对外可读的
  - 没有私人信息
  - 测试版要明确是 prerelease
- 不要依赖发布脚本的默认兜底公告；发版前应明确提供 `-ReleaseNotes`、`-ReleaseNotesFile` 或有意识地使用 `-AutoNotes`。

## 该用哪些项目技能

- 如果任务包含“发版、写更新公告、整理 release notes、发测试版/稳定版”，先读 `.agents/skills/release-announcement/SKILL.md`
- 如果任务包含“加功能、修 bug、改页面、重构、模块化拆分”，先读 `.agents/skills/modular-android-development/SKILL.md`

## 已验证命令

在仓库根目录可用：

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

本地跑 Gradle 前，先检查根目录 `local.properties` 里的 `sdk.dir`；如果已经有本机 Android SDK 路径，就直接使用，不要先问用户 SDK 在哪。

本地构建 debug APK：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-debug.ps1 -AndroidHome "你的 Android SDK 路径"
```

发布 GitHub Release：

```powershell
powershell -ExecutionPolicy Bypass -File .\publish-github-release.ps1 -VersionName 0.8.33-beta9 -VersionCode 171 -ReleaseNotes "这里写正式公告" -PreRelease
```

## 高风险区域

- `app/src/main/assets/lukoa-tavern.sh`
- `app/src/main/java/moe/lukoa/launcher/TermuxCommandRunner.kt`
- 备份/恢复相关文件
- 路径配置和镜像源配置相关文件
- 发版脚本和版本号修改

这些区域改动后，不要只看能不能编译，要额外确认行为是否变了。

## 改完后的最低检查

- 跑 `:app:compileDebugKotlin`
- 跑 `testDebugUnitTest`
- 跑 `:app:lintDebug`
- 跑 `:app:assembleDebug`
- 只要这次改动碰到发布链路、签名链路或混淆链路，再跑 `:app:assembleRelease`
- 如果改了公开文案，再人工复查一遍是否像面向用户的 App 文案，而不是开发日志
