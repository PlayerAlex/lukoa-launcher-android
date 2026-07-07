# 仓库结构说明

这份文档不是讲所有实现细节，而是帮助新的维护者或新的 AI 对话先快速看懂这个仓库该从哪里下手。

## 顶层结构

```text
.
├─ app/                         Android 主工程
├─ docs/                        长期文档与项目管理资料
├─ gradle/                      Gradle Wrapper
├─ .github/                     GitHub 工作流、Issue/PR 模板
├─ AGENTS.md                    给后续 AI 的仓库级规则
├─ README.md                    面向用户和访客的项目介绍
├─ CHANGELOG.md                 版本记录
├─ CONTRIBUTING.md              提交、验证、发版约定
├─ build-debug.ps1              本地构建 debug APK
├─ generate-release-notes.ps1   生成发版说明
└─ publish-github-release.ps1   发布 GitHub Release
```

## 最重要的入口

### 1. 仓库规则

- `AGENTS.md`
  后续 AI 进入仓库后必须先看，里面定义了文案、发布、模块化、Termux 使用边界等规则。

### 2. 用户入口

- `README.md`
  面向 GitHub 访客和普通用户。

### 3. 开发与发布约定

- `CONTRIBUTING.md`
- `CHANGELOG.md`

### 4. 项目管理与交接

- `docs/project-management/README.md`
- `docs/project-management/AI新对话启动模板.md`
- `docs/project-management/lukua-launcher-AI项目管理模板.xlsx`

## App 代码阅读顺序

如果是继续做功能、修问题或重构，建议先看这些文件：

1. `app/src/main/java/moe/lukoa/launcher/LukoaLauncherScreen.kt`
2. `app/src/main/java/moe/lukoa/launcher/LauncherNavigation.kt`
3. `app/src/main/java/moe/lukoa/launcher/LauncherLaunchSections.kt`
4. `app/src/main/java/moe/lukoa/launcher/LauncherVersionManagementSection.kt`
5. `app/src/main/java/moe/lukoa/launcher/LauncherSettingsSection.kt`
6. `app/src/main/java/moe/lukoa/launcher/LauncherDialogs.kt`
7. `app/src/main/java/moe/lukoa/launcher/TermuxCommandRunner.kt`

## 当前仓库的组织原则

### App 侧优先

- 能在 App 侧完成的校验、状态判断、确认提示、输入防呆，优先在 App 侧做。
- 不要把所有逻辑都推给 `Termux`，否则会拖慢处理，也会让行为更难追踪。
- 只有确实必须落到 shell 执行的事情，再交给 `Termux`。

### 模块化优先

- 不要把新逻辑重新堆回 `LukoaLauncherScreen.kt`
- 不要让 `TermuxCommandRunner.kt` 继续无限膨胀
- 遇到 parser / codec / guard / manager / reducer / store 这类纯逻辑，优先单独拆文件

### 文档分层

- 根目录：给用户和访客看
- `docs/project-management/`：给版本管理、交接、AI 协作看
- `docs/development/`：给继续维护仓库结构的人看

## 你想改什么时，该先去哪

- 想了解项目当前状态：先看 `docs/project-management/`
- 想发版：先看 `CONTRIBUTING.md`、`CHANGELOG.md`、发布脚本
- 想改 UI：先看各个 `Section` / `Dialog` 文件
- 想改 Termux 相关行为：先看 `TermuxCommandRunner.kt` 和相关 parser/codec
- 想做长期整理：先看这份文件和 `AGENTS.md`
