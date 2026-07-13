# Contributing

这份文档只管三件事：怎么改、怎么验、怎么发。

## 1. 开发前先确认

- 使用仓库自带的 `gradlew` / `gradlew.bat`，不要依赖系统里某个不确定版本的 Gradle。
- Android SDK 需要能提供 `platforms;android-35` 和 `build-tools;35.0.0`。
- 涉及公开文案时，禁止带入私人邮箱、QQ、旧身份信息。
- 改动默认优先放在 App 侧做防呆和状态管理，不要把逻辑全堆给 Termux 脚本。

## 2. 提交前最低检查

在仓库根目录执行：

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

如果你在 PowerShell 里已经配置好 Android SDK，也可以继续使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-debug.ps1 -AndroidHome "你的 Android SDK 路径"
```

需要查看 Debug JVM 单元测试覆盖率时，执行：

```powershell
.\gradlew.bat --no-daemon :app:jacocoDebugUnitTestReport
```

HTML 报告会生成到 `app/build/reports/jacoco/debugUnitTest/html/index.html`，XML 报告会生成到
`app/build/reports/jacoco/debugUnitTest/report.xml`。该报告会排除 Compose 和 Android 的生成类，但仍会保留手写 UI、系统组件及业务代码；因此在尚未添加仪器化测试时，全模块覆盖率偏低是正常现象。

## 3. 提交约定

- 每次提交尽量只解决一类问题，不把无关改动混进去。
- 高风险链路改动后，至少补一条能覆盖该逻辑的单元测试或人工验证记录。
- 不要把本地草稿、临时文件、诊断包、个人配置一起提交。

## 4. 发测试版

本项目默认每做完一个功能或修完一个明显问题，就发一个测试版。

- 更新 `app/build.gradle.kts` 里的 `versionName` 和 `versionCode`
- 构建并确认 APK 可安装
- 推送分支和 tag
- 创建 GitHub prerelease
- 确认 Release 文案不含私人信息

## 5. 公开反馈前自查

- UI 文案是否足够新手友好
- 危险操作是否有明确确认
- 路径、备份、恢复是否会误导用户
- 版本管理指向的是 SillyTavern 版本，不是启动器版本
