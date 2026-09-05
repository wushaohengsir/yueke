# CLAUDE.md — 通用师生约课平台（BookMate）

本文件是给 AI 编码助手的项目工作手册。在此仓库工作时请遵循以下约定。

## 项目概述

面向培训机构、独立教师与学员的轻量级在线约课系统（Web）。课程《项目实践 1》立项项目。
核心闭环：老师开放可授时段 → 学员自助约课/请假 → 系统自动防冲突并沉淀全部记录。
四种角色：**游客 / 学员 / 老师 / 管理员**。第一版不做在线支付。

需求基线（飞书，公开）：https://kcng3bohctwr.feishu.cn/docx/OvBLdIyq8og7Urx1Rfsc9Gl9niJ

## 目录结构

```
yueke/
├── CLAUDE.md          # 本文件
├── README.md
├── .gitignore
├── docs/
│   └── sql/schema.sql # MySQL 建表脚本（14 张规范化表，唯一事实来源）
├── frontend/          # （待建）Vue 3 前端：移动端 Web + 管理后台
└── backend/           # （待建）Spring Boot 后端
```

## 技术栈

| 层 | 选型 |
|----|------|
| 前端 | Vue 3 + Vite + TypeScript + Element Plus / Vant + Pinia + Vue Router |
| 后端 | Java 17 + Spring Boot 3 + Spring MVC + MyBatis-Plus + Spring Security（JWT） |
| 数据库 | MySQL 8.0（utf8mb4 / InnoDB） |
| 部署 | Docker Compose（nginx + springboot + mysql） |

## 本地开发环境（重要）

本机为 Windows，工具均为便携版，**不在系统 PATH**，调用前需指定路径：

- **Node**：`<workspace>/node/node.exe`（npm 用 `<workspace>/node/npm.cmd`，registry 用 `https://registry.npmmirror.com`）
- **GitHub CLI**：`<workspace>/bin/gh.exe`（已登录，对仓库有 WRITE 权限）
- **MySQL**：见下

`<workspace>` = `C:\Users\13681\.qwenworkcn\workspace\mth2ugx9m2ni92i0`

### MySQL（便携版 8.0.28）

- 位置：`<workspace>/mysql-8.0.28-winx64`，端口 `3306`，用户 `root`，**空密码**
- 启动：`cd <workspace>/mysql-8.0.28-winx64 && ./bin/mysqld.exe --console`（后台运行）
- 客户端：`./bin/mysql.exe -uroot`
- 数据库名：`yueke`；建表脚本：`docs/sql/schema.sql`（改表先改脚本再应用，保持可回放）

## 数据库设计约定

- 遵循 1NF→3NF/BCNF；M:N 一律 Junction 表（如 `teacher_subject`）
- 金额用分（INT）、课时用整数，不用 FLOAT
- 生命周期用 `status` + 时间戳，不物理删除
- **并发防冲突**：`booking` 表用生成列 `active_flag` + 唯一索引 `uk_teacher_active_slot(teacher_id, start_at, active_flag)` 保证同一老师同一时段仅一个活跃预约（MySQL 无部分唯一索引的替代方案）
- 详细范式方法见技能 `database-design`

## 编码约定

- 前端：组合式 API（`<script setup>`）+ TS 严格模式；组件按 `views/ components/ stores/ api/` 组织
- 后端：Controller / Service / Mapper 分层；DTO 与实体分离；统一返回体 + 全局异常处理；JWT 鉴权，角色越权拦截
- 接口风格 RESTful；密码 BCrypt 存储

## Git 工作流（强制）

- **每个版本/功能更新必须及时 commit 并 push，便于回滚**（用户明确要求）
- 分支：`main` 为主干；里程碑打 tag（`v0.1` / `v0.5` / `v1.0`）
- Commit 信息用中文 + 约定式前缀：`feat:` `fix:` `docs:` `chore:` `refactor:`，写清版本与内容
  例：`feat(db): V0.1 数据库设计 - 14 张规范化表`

## 课程过程记录（每日必做）

课程 AI 分析软件每晚扫描仓库生成反馈，因此每天 24:00 前必须提交三类记录：

1. **git 提交**：随时提交，commit message 一句话说清改了什么；当天工作当天提交（次日补无效）
2. **提示词记录**：导出当天与 AI Coding 工具的对话，提交到 `prompts/24320220/D{序号}/`（用了几个工具导几份，文本格式、不截图）
3. **每日日报**：提交到 `daily/24320220/D{序号}.md`，固定五节格式：

```markdown
# D{n} 日报
## 昨日遗留问题的回答
（系统每日反馈会留一个问题，次日回答；第 1 天没有这节）
## 今日目标
（早上写：今天打算完成什么）
## 实际进展
（晚上写：完成了什么，可粘贴关键 commit）
## 遇到的问题与解决
（卡在哪、试了什么、谁或什么工具帮到你了）
## 明日计划
（明天的最低目标）
```

写真实发生的具体问题与解决思路，不写空话套话。

- 目录规范：`daily/`、`prompts/`、`docs/` 一律加学号子目录（本成员学号 `24320220`）
- 每日反馈：每天上午在群里收到分析结果、建议，以及「明天最少要往前走哪一步」

## 需求文档与日志同步

- **需求文档更新**：在原有飞书文档中直接修订；收到用户发来的飞书链接后，以该链接为最新需求基线，同步更新 `docs/` 本地文档与 `todos.md`
- **日志同步飞书**：每次项目更新，把本次更新日志同步推送到飞书并留存，保证飞书侧与仓库侧进度一致（工具：`lark-cli docs +update --command append`，追加到开发日志 docx）

## 云服务器同步（每次功能更新必做，收尾项之一）

代码 commit + 双源 push 后，凡改动前端/后端，**必须同步部署上线**，保证线上（公网 http://146.56.247.172，腾讯云）与仓库一致：

1. **打包后端**：`backend/` 下 `mvn package -DskipTests`，产物 `target/backend-0.1.0.jar`
2. **构建前端**：`frontend/` 下 `npm run build`，产物 `dist/`
3. **上传并重建**（服务器 `ubuntu@146.56.247.172`，SSH 密钥 `cjt.pem`——置于仓库外、勿提交；compose 项目在服务器 `~/bookmate/`）：
   - `scp backend/target/backend-0.1.0.jar` → `~/bookmate/backend/backend-0.1.0.jar`
   - 重置并上传 `frontend/dist/*` → `~/bookmate/frontend/dist/`
   - `sudo docker compose build backend frontend && sudo docker compose up -d --remove-orphans backend frontend`
4. **线上冒烟**：公网 `GET /api/auth/subjects` 返回 `code=0`；`POST /api/auth/login`（未注册手机号）应 `code=400`「尚未注册」——验证新代码生效且**勿在线插入脏数据**

> 数据库结构/演示数据变更时才需要：本地 `mysqldump` 出 `bookmate-dump.sql` 上传，`docker exec -i bookmate-db mysql ... < bookmate-dump.sql`（业务数据在 data 卷持久化，一般不动）。

## 里程碑

| 版本 | 时点 | 交付 |
|------|------|------|
| V0.1 | 第 2 天 | 静态原型 + 登录 + 约课最小闭环 |
| V0.5 | 第 4 天 | 四角色完整链路 + 防冲突 + 审批 |
| V1.0 | 第 6 天 | 合同/商品/看板 + Docker 部署答辩 |

## 给助手的提醒

- 不要提交密钥（.env、application-local.yml 已在 .gitignore）
- 修改数据库结构时同步更新 `docs/sql/schema.sql` 并提交
- 网络受限：GitHub raw/release 走 `gh-proxy.com` 镜像或 API base64；npm 走 npmmirror；MySQL 走阿里云镜像
