# 仓库结构说明

帮助新维护者或新的 AI 对话快速看懂这个仓库该从哪里下手。详细的构建、发版和约定见根目录 `AGENTS.md`。

## 顶层结构

```text
.
├─ app/                         Android 主工程
├─ docs/                        长期文档与项目管理资料
├─ tools/                       开发辅助脚本（目前是 lukoa-tavern.sh 的 PC 沙盒回归）
├─ gradle/                      Gradle Wrapper
├─ .github/                     GitHub 工作流、Issue/PR 模板、Dependabot
├─ AGENTS.md                    项目速览与约定
├─ README.md                    面向用户和访客的项目介绍
├─ CHANGELOG.md                 版本记录
├─ CONTRIBUTING.md              提交、验证、发版约定
├─ build-debug.ps1              本地构建 debug APK
├─ generate-release-notes.ps1   从 CHANGELOG 汇总发版说明
└─ publish-github-release.ps1   发布 GitHub Release
```

## 源码布局

所有 Kotlin 源码都在 `app/src/main/java/moe/lukoa/launcher/` 单包下，靠文件名前缀分组：

- `LukoaLauncherScreen.kt`：页面级状态编排、跨模块协调、事件分发。
- `LauncherNavigation.kt`、`SectionSwitcher.kt`：底部导航与页面切换。
- `Launcher*Section.kt`、`*Section.kt`：各页面 UI（启动、版本管理、设置、备份、工具箱、文档、修复工具、扩展管理、用户管理）。
- `Launcher*Dialogs.kt`、`SettingsDialogs.kt`、`LauncherDialogPrimitives.kt`：弹窗。
- `Launcher*Coordinator.kt`、`Launcher*State.kt`、`*Store.kt`：状态与持久化。
- `Tavern*.kt`：酒馆路径、实例档案、版本、镜像源、体检、扩展、上传限制。
- `Termux*.kt`：命令构建、执行、结果解析、日志展示、唤醒策略。
- `Backup*.kt`、`AutoBackup*.kt`：备份、恢复、备份内容检查、自动备份调度。
- `GithubUpdate*.kt`：启动器自身的更新检测与安装。
- `*Policy.kt`、`*Guard.kt`、`*Codec.kt`、`*Parser.kt`：纯逻辑，基本都有同名 `*Test.kt`。

`app/src/main/assets/lukoa-tavern.sh` 是下发到 Termux 的脚本，App 侧通过 `TermuxScriptCommandBuilder.kt` 组装调用参数。

## 阅读顺序

想做功能、修问题或重构，建议按这个顺序看：

1. `AGENTS.md`
2. `LukoaLauncherScreen.kt`（先看结构，不用逐行读）
3. `LauncherNavigation.kt`
4. 你要改的那个 `*Section.kt` 和对应的 `*Dialogs.kt`
5. 相关的 `Tavern*` / `Termux*` / `Backup*` 纯逻辑文件及其测试
6. 如果涉及 shell 行为，再看 `lukoa-tavern.sh` 里对应的子命令

## 文档分层

- 根目录：给用户和访客看
- `docs/project-management/`：当前主线、AI 协作记录、session-notes 交接
- `docs/development/`：仓库结构和维护说明

## 你想改什么时，该先去哪

- 了解项目当前状态：`docs/project-management/README.md` 和最新的 session-note
- 发版：`AGENTS.md` 的发版一节、`CHANGELOG.md`、发布脚本
- 改 UI：对应的 `*Section` / `*Dialogs` 文件
- 改 Termux 相关行为：`TermuxCommandRunner.kt`、`TermuxScriptCommandBuilder.kt` 和相关 parser/codec
- 改脚本行为：`lukoa-tavern.sh`，并检查 `BundledShellScriptRegressionTest.kt` 是否需要跟着更新；涉及更新 / 回退时再跑 `bash tools/simulate-tavern-script.sh`，它会在假仓库里真实执行这些子命令
