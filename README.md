# 通用师生约课平台（BookMate）

面向培训机构、独立教师与学员的轻量级在线约课系统。《项目实践 1》课程项目

## 问题与方案

线下培训靠微信群口头约课：时段冲突频发、课时账目不清、请假补课无凭据。本平台聚焦约课闭环：老师开放可授时段，学员自助约课请假，系统自动防冲突并沉淀记录。

四种角色：游客（免登录浏览师资）、学员（约课/请假/课时）、老师（课表/审批/时段模板）、管理员（审核/看板/用户科目管理）。

核心特性：课时按课程拆分不通用；约课时段时长由老师控制；并发防冲突（事务+唯一索引）；请假审批返还课时；课时包合同签署自动入账。

## 技术栈

前端 Vue 3 + Vite + TypeScript + Pinia；后端 Java 17 + Spring Boot 3 + MyBatis-Plus + JWT；数据库 MySQL 8.0；部署 Docker Compose。

## 版本

V0.1 概念验证 → V0.5 四角色联调 → V1.0 验收（合同/看板/部署）。每个版本及时提交并双源推送（GitHub + Gitee）。

## 飞书文档

另一队组员提交 / 同步日志时，请以以下飞书文档为准：

- 需求文档（随需求在原文修订）：https://kcng3bohctwr.feishu.cn/docx/OvBLdIyq8og7Urx1Rfsc9Gl9niJ
- 开发日志（每次项目更新追加日志）：https://kcng3bohctwr.feishu.cn/docx/L4RldrFoMo2TW0xefmEc5hvkndg

更新流程见 CLAUDE.md「需求文档与日志同步」约定。
