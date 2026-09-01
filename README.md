# 通用师生约课平台（BookMate）

面向培训机构、独立教师与学员的轻量级在线约课系统。课程《项目实践 1》立项项目。

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Element Plus / Vant + Pinia + Vue Router
- 后端：Java 17 + Spring Boot 3 + Spring MVC + MyBatis-Plus + Spring Security（JWT）
- 数据库：MySQL 8.0
- 部署：Docker Compose（nginx + springboot + mysql）

## 目录规划

```
yueke/
├── frontend/   # Vue 3 前端（移动端 Web + 管理后台）
├── backend/    # Spring Boot 后端
└── docs/       # 需求文档、原型、立项/中期/期末报告
```

## 角色

游客 / 学员 / 老师 / 管理员

## 版本与回滚

每个版本更新均提交 Commit（见 `git log`），便于回滚。里程碑：V0.1（概念验证）→ V0.5（中期）→ V1.0（验收）。

## 相关文档

- 需求文档（飞书，公开）：https://kcng3bohctwr.feishu.cn/docx/OvBLdIyq8og7Urx1Rfsc9Gl9niJ
