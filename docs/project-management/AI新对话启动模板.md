# AI 新对话启动模板

开新 AI 对话时可以直接复制下面这段。

---

你现在接手的是 GitHub 仓库 `PlayerAlex/lukoa-launcher-android`，本地路径是 `<填你的路径>`。

请先读这些文件，再给方案或改代码：

1. `AGENTS.md`（项目速览、结构、构建与发版方式）
2. `docs/project-management/README.md`（当前主线与风险）
3. `docs/project-management/session-notes/` 里日期最新的一份
4. `CHANGELOG.md` 顶部几个版本

项目信息：

- Android 启动器 App，Kotlin + Jetpack Compose，帮助用户在手机上使用 Termux + SillyTavern。
- 真正的 shell 命令由 `app/src/main/assets/lukoa-tavern.sh` 通过 Termux 执行。
- 已经发到 1.0 正式版，不是从零开始的项目。

协作方式：

- 默认用简体中文。
- 改代码前先用两三句话说明你理解的目标和第一步。
- 新逻辑优先放进现有前缀分组的独立文件，纯逻辑配单元测试；不要把东西继续堆进 `LukoaLauncherScreen.kt`。
- 能在 App 侧完成的校验和状态判断放 App 侧，只有必须执行 shell 的才交给 Termux。
- 涉及发版时同步改 `app/build.gradle.kts` 版本号和 `CHANGELOG.md`。

---

## 追加一句本次目标

例如：

- 这次只修首次启动引导的问题，不动设置页。
- 这次只处理 Termux 后台常驻检测，不改发版脚本。
- 这次只整理文档和测试记录，不动 App 功能。
