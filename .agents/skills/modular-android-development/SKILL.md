---
name: modular-android-development
description: Use when the user asks to add a feature, fix a bug, refactor code, modularize screens, improve maintainability, or change Android UI or Termux integration behavior in this repository.
---

# Modular Android Development

这个技能专门约束这个项目后续代码怎么写，避免重新长回“大泥球”。

## 先读这些文件

- `AGENTS.md`
- `app/src/main/java/moe/lukoa/launcher/LukoaLauncherScreen.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherNavigation.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherLaunchSections.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherVersionManagementSection.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherSettingsSection.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherLaunchDialogs.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherBackupDialogs.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherProfileDialogs.kt`
- `app/src/main/java/moe/lukoa/launcher/SettingsDialogs.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherProfileCoordinator.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherPathSettingsState.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherMirrorSettingsState.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherHealthCheck.kt`
- `app/src/main/java/moe/lukoa/launcher/LauncherInputGuards.kt`
- `app/src/main/java/moe/lukoa/launcher/PendingLauncherTaskSupport.kt`
- `app/src/main/java/moe/lukoa/launcher/TermuxCommandRunner.kt`

## 模块化规则

### 1. 先找现有归属

- 导航相关放 `LauncherNavigation.kt`
- 启动页相关放 `LauncherLaunchSections.kt`
- 版本管理相关放 `LauncherVersionManagementSection.kt`
- 设置页相关放 `LauncherSettingsSection.kt`
- 对话框相关放 `LauncherDialogs.kt`
- 跨页共享的小 UI 原语可以放共享 section/primitives 文件，但不要随手复制一份

### 2. 不要把东西重新堆回大文件

- `LukoaLauncherScreen.kt` 主要负责状态编排、跨区协调、事件分发
- 不要把成段 Compose UI、表单、提示卡、整套对话框重新塞回 `LukoaLauncherScreen.kt`
- `TermuxCommandRunner.kt` 已经很大，新增行为前先考虑是否能拆出 codec、parser、guard、support、manager

### 3. 纯逻辑优先独立

遇到这些情况，优先新建独立纯逻辑文件：

- 解析输出
- 编码/解码参数
- 输入校验
- 状态归并
- 快速修复判断
- 版本/镜像/备份规则

如果能独立成纯逻辑，就顺手补单元测试。

### 4. App 侧优先

- 能在 App 侧判断、拦截、提示、确认、缓存的，优先在 App 侧做
- 不要把本来可以在 App 侧完成的防呆，全都推给 shell 脚本

### 5. 业务红线

- 酒馆路径不要自动乱改
- 默认路径是 `~/SillyTavern`
- 版本管理说的是 SillyTavern，不是启动器
- 备份/恢复优先保数据
- 高风险动作优先补确认和清楚提示

## 推荐工作流

1. 先确认改动属于哪个现有模块
2. 如果已有 parser/guard/support/manager 能复用，先复用
3. 新增纯逻辑时先拆文件，再写测试
4. 改动 UI 行为时，确认是不是应该只改某个 section 文件，而不是改总入口
5. 涉及备份、版本、路径、镜像、更新时，多看一眼相关 guard 和 parser

## 最低验证

至少执行：

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin testDebugUnitTest :app:lintDebug :app:assembleDebug
```

如果改到了发布链路、release 构建、危险操作或高风险数据流程，再加：

```powershell
.\gradlew.bat --no-daemon :app:assembleRelease
```
