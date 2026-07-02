# 露科亚启动器

一个运行在 Android 上、专门给 `Termux + SillyTavern` 准备的启动器。

它的目标不是替代 Termux，而是把启动、停止、日志查看、版本切换、备份、镜像源切换和新手引导这些操作收进一个更顺手的移动端 App 里。

## 项目定位

- 前端：Android App，`Kotlin + Jetpack Compose`
- 后端执行：依赖 `Termux` 执行真正的 Shell 命令
- 服务对象：需要在手机上使用 `SillyTavern` 的用户

## 目前功能

- 启动、停止、检测 SillyTavern 状态
- 同步 Termux 调用返回和运行日志
- 新手引导、权限检查、Termux 唤醒
- 酒馆版本读取、安装、更新、回退
- 官方源 / 镜像源切换与可用性检测
- 手动备份、自动备份、导入、导出、应用备份
- GitHub 更新检测与 APK 内更新
- 诊断日志导出，方便排查问题

## 运行前提

- Android 8.0 及以上
- 已安装 `Termux`
- 已授予 `RUN_COMMAND` 权限
- 首次使用时需要按引导完成 Termux 初始化

## 仓库结构

- `app/`
  Android 主工程
- `app/src/main/java/moe/lukoa/launcher/`
  启动器界面、状态管理、Termux 调用、备份和版本管理逻辑
- `app/src/main/assets/lukoa-tavern.sh`
  下发到 Termux 执行的核心脚本
- `build-debug.ps1`
  本地构建 debug APK
- `publish-github-release.ps1`
  自动构建、打 tag、推送并发布 GitHub Release
- `generate-release-notes.ps1`
  生成 release 公告

## 本地构建

先准备 Android SDK，然后在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-debug.ps1 -AndroidHome "你的 Android SDK 路径"
```

生成的 APK 默认在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 发布到 GitHub Release

先确认你已经安装并登录 `gh`：

```powershell
gh auth status
```

然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\publish-github-release.ps1 -VersionName 0.8.24 -VersionCode 154
```

这个脚本会自动完成：

1. 更新 `app/build.gradle.kts` 里的版本号
2. 构建 APK
3. 复制带版本号的 APK 到 `outputs/`
4. 提交当前改动
5. 打 tag
6. 推送分支和 tag
7. 创建 GitHub Release 并上传 APK

## 许可证

本项目使用 `MIT License`。

这意味着别人可以在保留原作者版权和许可证文本的前提下，自由使用、修改、商用和分发这份代码。

## 仓库地址

- 本项目不会默认绑定某个作者的 GitHub 仓库。
- 如果你要启用启动器 APK 更新提醒，请在设置里手动填写你自己的 `用户名/仓库名`。
