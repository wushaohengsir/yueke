# 通用师生约课平台（BookMate）

面向培训机构、独立教师与学员的轻量级在线约课系统。《项目实践 1》课程项目，学号 24320220。

## 问题与方案

线下培训靠微信群口头约课：时段冲突频发、课时账目不清、请假补课无凭据。本平台聚焦约课闭环：老师开放可授时段，学员自助约课请假，系统自动防冲突并沉淀记录。四种角色：游客（免登录浏览）、学员（约课/请假/课时）、老师（课表/审批/时段模板）、管理员（审核/看板/用户科目管理）。

**核心特性**：课时按课程拆分不通用（student_credit）；约课为 45 分钟时间段；并发防冲突（事务+唯一索引）；请假审批流（批准返还对应课程课时）；课时包合同（签署生效自动入账）。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router
- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus + Spring Security（JWT）
- 数据库：MySQL 8.0（15 张规范化表，schema 见 docs/sql/）
- 部署：Docker Compose（nginx + springboot + mysql）

## 目录结构

```
├── frontend/        # Vue 3 前端（移动端 + 老师端 + 管理端）
├── backend/         # Spring Boot 后端
├── docs/sql/        # 数据库 schema
├── docs/24320220/   # 个人课程文档
├── daily/           # 每日日报
├── prompts/         # AI 提示词导出
└── todos.md         # 需求清单（每日勾选）
```

## 运行

```
# 后端（:8080）
cd backend && mvn clean package -DskipTests && java -jar target/backend-0.1.0.jar
# 前端（:5173）
cd frontend && npm install && npm run dev
```

演示账号（密码均 123456）：学员 13800000000 / 老师 13800000001 / 管理员 13900000000

## 版本

V0.1 概念验证 → V0.5 中期（四角色联调）→ V1.0 验收（合同/看板/部署）。每个版本及时 commit 双源推送（GitHub + Gitee）便于回滚，详见 git log 与 daily/。

## 需求文档

飞书（公开）：https://kcng3bohctwr.feishu.cn/docx/OvBLdIyq8og7Urx1Rfsc9Gl9niJ
