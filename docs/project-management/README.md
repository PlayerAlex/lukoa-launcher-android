# 项目管理资料

这份目录是给 `PlayerAlex/lukoa-launcher-android` 的长期开发管理准备的。

因为这个项目已经经历了很多版本，而且后续还会频繁切换到新的 AI 对话继续开发，所以需要把“主线版本、测试记录、AI 协作记录”放进仓库里，避免每次都从零重新解释项目状态。

## 目录结构

```text
docs/
  project-management/
    README.md
    AI新对话启动模板.md
    lukua-launcher-AI项目管理模板.xlsx
    session-notes/
```

## 每个文件是干什么的

- `lukua-launcher-AI项目管理模板.xlsx`
  这是项目主管理表，包含版本总表、发版检查、兼容性测试、AI 协作记录和项目总览。

- `AI新对话启动模板.md`
  这是给新 AI 对话直接复制使用的启动提示词模板，用来减少上下文断层。

- `README.md`
  说明这些资料怎么使用，以及未来应该怎么继续维护。

- `session-notes/`
  用来放“某一轮对话已经确认过什么、做到哪一步、下一轮该怎么接”的交接总结。

## 当前建议主线

- 当前主线版本：`v0.9.1-beta13`
- 该版本发布时间：`2026-07-08`
- 当前定位：`Beta / 预发布`
- 当前最重要的风险：托管目录迁移、分身目录删除和多实例切换这条链路虽然已经补齐，但还需要继续做真实设备回归，特别是大目录迁移和中途中断后的恢复体验。

补充说明：

- 如果这里的“当前建议主线”和代码、`CHANGELOG.md`、Git tag 不一致，优先相信代码和最新 `session-notes`。
- 当前仓库里已经有一部分资料曾停留在 `v0.9.1-beta4`，不要直接把旧描述当最终事实。

## 建议维护方式

以后每次准备继续开发、测试或发版时，按这个顺序走：

1. 先确认当前主线版本是不是还成立。
2. 在 `lukua-launcher-AI项目管理模板.xlsx` 的“版本总表”里登记本次版本目标。
3. 只围绕一个明确目标开发，不要一版里同时改很多不相关内容。
4. 每次让 AI 改代码后，在“AI协作记录”补一行。
5. 发 APK 前，跑“发版检查”。
6. 换手机或换系统测试时，在“兼容性测试”新增样本。

## 适合这个项目的 GitHub 使用方式

这个仓库里既有代码，也有项目管理资料，所以建议分工如下：

- 代码变更：继续走正常的 Git 提交、分支、发版流程。
- 项目状态记录：更新这个目录下的文档和表格。
- 对外用户可见内容：继续以根目录 `README.md`、`CHANGELOG.md`、GitHub Release 文案为主。
- 对内开发协作内容：集中放在 `docs/project-management/`。

## 给未来 AI 的最低要求

以后无论切到哪个新对话，最好都先让 AI 看这几样东西：

1. `AGENTS.md`
2. `docs/project-management/README.md`
3. `docs/project-management/AI新对话启动模板.md`
4. `docs/project-management/lukua-launcher-AI项目管理模板.xlsx`
5. 根目录 `CHANGELOG.md`
6. `docs/project-management/session-notes/` 下最新一份总结

这样新对话里的 AI 更容易知道：

- 这个项目不是从零开始
- 当前主线版本是哪一个
- 哪些问题已经做过
- 哪些兼容性风险还没收掉

## 后续建议

如果后面测试样本越来越多，可以继续在这个目录下新增：

- `test-reports/`
- `session-notes/`
- `release-checklists/`

但现阶段先维持现在这三个文件就够了，不要一开始把流程做得太重。
