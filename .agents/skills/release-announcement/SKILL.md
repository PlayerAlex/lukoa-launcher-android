---
name: release-announcement
description: Use when the user asks to publish a beta or stable release, write or rewrite release notes, update a GitHub Release, summarize changes for external users, or prepare any public-facing update announcement for this repository.
---

# Release Announcement

这个技能专门约束这个项目的发版文案和发版动作。

## 先读这些文件

- `AGENTS.md`
- `CHANGELOG.md`
- `CONTRIBUTING.md`
- `publish-github-release.ps1`
- `generate-release-notes.ps1`
- `app/build.gradle.kts`

## 目标

把 Release 文案写得像一个正常 App 的更新公告，而不是开发日志。

## 文案规则

- 默认使用简体中文。
- 先写“用户会感受到什么”，再写必要的技术信息。
- 优先写功能变化、体验优化、稳定性修复、风险提示。
- 只有当内部重构真的改变了稳定性、速度、错误率或维护风险时，才把它翻译成用户能理解的收益去写。
- 不要把下面这些内容当成主公告：
  - “我把某个文件拆了”
  - “做了重构”
  - “改了脚本”
  - “看 commit history”
  - “修了一堆问题”
- 不要出现私人邮箱、QQ、旧身份或任何私人联系方式。

## 推荐结构

测试版或正式版公告默认按这个结构写：

```md
## 本次更新

一句话概括这版主要改善了什么。

### 新增与优化

- ...
- ...

### 修复

- ...
- ...

### 说明

- 如果是测试版，在这里说明这是测试版
- 如果有已知风险或反馈方式，在这里补充
```

如果这版几乎全是稳定性和维护性改动，也不要直接写“重构”。改写成：

- 优化启动/更新/检测流程的稳定性
- 减少某类异常或误判
- 改善日志、错误提示、回退或恢复体验

## 发版流程

1. 先确认 `app/build.gradle.kts` 的 `versionName` 和 `versionCode` 已更新。
2. 先跑仓库最低校验命令。
3. 文案写完后，再执行 `publish-github-release.ps1`。
4. 测试版必须带 `-PreRelease`。
5. 发完后确认 GitHub Release 页面和 APK 资产都存在。

## 禁止假设

- 不要假设内部重构一定值得写进公开公告。
- 不要假设自动生成的 release note 已经足够对外。
- 不要假设测试版用户能看懂内部文件名或脚本名。

## 交付前检查

- 文案是否是面向用户的
- 是否没有私人信息
- 是否写清这版主要改善点
- 是否把内部重构翻译成了用户收益
- 是否明确测试版/正式版身份
