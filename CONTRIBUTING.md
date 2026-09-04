# Contributing

这份文档只管三件事：怎么改、怎么验、怎么发。项目背景和约定见 `AGENTS.md`。

## 1. 环境

- 使用仓库自带的 `gradlew` / `gradlew.bat`。
- JDK 17–22（Gradle 8.8 不支持 JDK 25）。
- Android SDK 需要 `platforms;android-35` 和 `build-tools;35.0.0`，首次构建时 AGP 会自动补下。
- 在根目录 `local.properties` 写入 `sdk.dir=你的 SDK 路径`（该文件已被 gitignore）。

## 2. 提交前检查

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin testDebugUnitTest :app:lintDebug :app:assembleDebug
```

改到发布链路、签名或混淆时再追加 `:app:assembleRelease`。

改到 `lukoa-tavern.sh` 的更新 / 回退相关逻辑（`ensure_tavern_mutation_ready`、`cmd_update`、`cmd_rollback`、上传限制补丁的撤销与补回）时，再跑一次脚本沙盒，它会用假仓库真实执行这些子命令并核对输出格式：

```bash
bash tools/simulate-tavern-script.sh   # Windows 在 Git Bash 里跑；需要 git、node、npm
```

CI 里的 `tavern-script-simulation` job 跑的是同一个脚本。

覆盖率报告：

```powershell
.\gradlew.bat --no-daemon :app:jacocoDebugUnitTestReport
```

HTML 在 `app/build/reports/jacoco/debugUnitTest/html/index.html`。报告排除了 Compose 和 Android 生成类，但保留手写 UI，所以整体数字偏低是正常的。

## 3. 提交约定

- 一次提交尽量只解决一类问题。
- 高风险链路（备份恢复、Termux 命令、路径、脚本）改动后，补一条单元测试或在 session-note 里留一条人工验证记录。
- 本地草稿、诊断包、个人配置不要一起提交；发布脚本遇到未跟踪文件会停下来提醒。

## 4. 发版

每做完一个功能或修完一个明确问题，通常就发一个测试版：

1. 写好公告（用户视角，先说变化再说修复）。
2. 运行 `publish-github-release.ps1`，测试版带 `-PreRelease`。脚本会改版本号、构建、提交、打 tag、推送并创建 Release。
3. 在 `CHANGELOG.md` 顶部补一段。
4. 到 Release 页面确认 APK 已上传。

## 5. 公开反馈前自查

- 文案是否新手能看懂
- 危险操作是否有明确确认
- 路径、备份、恢复相关提示是否会误导
- "版本管理"指的是 SillyTavern 版本，不是启动器版本
- 公开内容里没有私人联系方式
