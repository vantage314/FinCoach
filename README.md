# 💰 白领人士个人资产诊断与投资建议系统（FinCoach）

> **FinCoach** 是一款面向白领人士的全栈智能个人资产诊断与投资建议平台，集 **资产管理、财务健康诊断、投资计划、实时行情、AI 对话咨询** 于一体，帮助用户科学管理个人财务、优化资产配置。

---

## 📋 目录

- [项目简介](#-项目简介)
- [功能模块](#-功能模块)
- [技术栈](#-技术栈)
- [目录结构](#-目录结构)
- [环境准备](#-环境准备)
- [快速启动](#-快速启动)
- [环境变量配置](#-环境变量配置)
- [API 文档](#-api-文档)
- [数据库管理](#-数据库管理)
- [Python 行情服务](#-python-行情服务)

---

## 🏠 项目简介

本系统采用前后端分离架构，包含三大子系统：

| 子系统 | 说明 |
|--------|------|
| **用户端** (`/app`) | 面向普通用户的财务管理界面 |
| **管理后台** (`/admin`) | 面向管理员的运维与数据管理 |
| **行情同步服务** (Python) | 实时抓取行情数据、财经新闻、F10 公司资料 |

系统实现了 **JWT 无状态认证** 和 **RBAC 角色权限控制**（USER / ADMIN），管理后台与用户端路由隔离。

---

## ✨ 功能模块

### 用户端

| 模块 | 路由 | 说明 |
|------|------|------|
| 📊 资产体检 | `/app/diagnosis` | 多维度财务健康评分与诊断建议 |
| 🏦 资产管理 | `/app/dashboard` | 资产总览仪表盘 |
| 📈 市场中心 | `/app/market` | A 股实时行情、K 线图、证券详情 |
| 📋 投资计划 | `/app/plan` | 智能生成投资计划并一键执行 |
| 🚀 投资驾驶舱 | `/app/investment` | 自选股、板块热力图、智能推荐 |
| 🤖 AI 咨询 | `/app/chat` | 基于 DeepSeek 的智能财务问答 |
| 📉 资产分析 | `/app/asset/analysis` | 资产趋势可视化与归因分析 |
| 💳 债务管理 | `/app/debt` | 借贷及还款追踪 |
| 💰 现金流管理 | `/app/cashflow` | 收入支出流水记录与分析 |
| ⚠️ 风险测评 | `/risk/assessment` | 用户投资风险偏好问卷评估 |
| 👤 个人中心 | `/app/user/profile` | 用户信息与密码管理 |

### 管理后台

| 模块 | 路由 | 说明 |
|------|------|------|
| 仪表盘 | `/admin/dashboard` | 系统运营数据概览 |
| 预警管理 | `/admin/alerts` | 用户财务预警规则与记录 |
| 用户管理 | `/admin/users` | 用户列表、角色编辑 |
| 证券映射 | `/admin/securities/mapping` | 股票代码与资产项映射管理 |
| 行情快照 | `/admin/securities/snapshots` | 实时行情数据查看 |
| 数据质量 | `/admin/securities/quality` | 数据完整性与准确性检查 |
| 建议规则集 | `/admin/advice/rulesets` | 诊断建议配置化管理 |
| 数据源管理 | `/admin/data-source` | 外部数据源与抓取状态 |
| 调试快照 | `/admin/debug` | 系统调试信息 |

---

## 🛠 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2.2 | Web 框架 |
| MyBatis-Plus | 3.5.5 | ORM / 持久层 |
| PostgreSQL | 14+ | 数据库（默认，也支持 MySQL 回切） |
| Flyway | 9.22.3 | 数据库版本管理 |
| JWT (jjwt) | 0.12.3 | 无状态认证 |
| Spring Security Crypto | — | 密码加密 (BCrypt) |
| SpringDoc OpenAPI | 2.3.0 | Swagger API 文档 |
| Lombok | 1.18.30 | 简化 Java 代码 |
| Maven | — | 构建工具 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Vite | 5.4.x | 构建与开发服务器 |
| Element Plus | 2.13.x | UI 组件库 |
| ECharts | 6.0 | 数据可视化图表 |
| Vue ECharts | 8.0 | ECharts Vue 封装 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| Axios | 1.13 | HTTP 请求 |
| Sass | 1.97 | CSS 预处理器 |
| Playwright | — | E2E 端到端测试 |

### Python 行情服务

| 技术 | 用途 |
|------|------|
| SQLAlchemy + psycopg2 | 数据库连接 |
| Requests | HTTP 请求（新浪行情 API） |
| Schedule | 定时任务调度 |

---

## 📁 目录结构

```
FinCoach/
├── backend/                    # 后端 Spring Boot 项目
│   ├── pom.xml                 # Maven 配置
│   ├── .env.example            # 环境变量模板
│   └── src/main/java/com/fincoach/core/
│       ├── CoreApplication.java
│       ├── controller/         # 控制器层 (11 个 Controller)
│       ├── service/            # 业务逻辑层
│       ├── repository/         # 数据访问层
│       ├── healthv2/           # 财务健康诊断 V2 引擎
│       ├── rbac/               # 角色权限控制
│       ├── security/           # 安全配置
│       ├── interceptor/        # JWT 拦截器
│       ├── config/             # 全局配置
│       ├── common/             # 通用类（异常处理、常量）
│       ├── ticker/             # 证券代码映射
│       └── utils/              # 工具类
├── frontend/                   # 前端 Vue 3 项目
│   ├── package.json
│   ├── vite.config.mts         # Vite 配置
│   ├── .env.example            # 环境变量模板
│   └── src/
│       ├── api/                # API 调用封装 (20 个模块)
│       ├── views/              # 页面组件 (14 个功能视图)
│       ├── components/         # 可复用组件
│       ├── store/              # Pinia 状态管理
│       ├── router/             # 路由配置
│       ├── theme/              # 语义化主题
│       ├── layout/             # 布局组件
│       └── utils/              # 工具函数
├── sql/                        # 数据库脚本
│   ├── init/                   # 全量初始化脚本
│   └── migration/              # 增量迁移脚本 (23 个)
├── scripts/                    # 部署与开发脚本
│   ├── dev.ps1                 # Windows 一键启动脚本
│   └── qa/                     # QA 测试脚本
├── doc/                        # 项目文档
│   ├── design/                 # 设计文档
│   ├── bugfix/                 # Bug 修复记录
│   └── logs-archive/           # 开发日志归档
├── main.py                     # Python 行情同步服务入口
└── README.md                   # 本文件
```

---

## ⚙️ 环境准备

### 必需环境

| 环境 | 最低版本 | 说明 |
|------|----------|------|
| **JDK** | 17+ | 后端编译运行（推荐 OpenJDK 21） |
| **Maven** | 3.8+ | 后端构建 |
| **Node.js** | 18+ | 前端构建（推荐 LTS 版本） |
| **npm** | 9+ | 前端包管理（或使用 pnpm） |
| **PostgreSQL** | 14+ | 数据库（默认） |
| **Python** | 3.8+ | 行情同步服务（可选） |

### 推荐工具

- **IDE**: IntelliJ IDEA（后端）/ VS Code（前端）
- **数据库管理**: Navicat / DBeaver / DataGrip

---

## 🚀 快速启动

### 1. 克隆项目

```bash
git clone https://github.com/vantage314/FinCoach.git
cd FinCoach
```

### 2. 初始化数据库

```bash
# 创建 PostgreSQL 数据库
psql -U postgres -c "CREATE DATABASE fincoach ENCODING 'UTF8';"

# 无需手动执行 SQL —— Flyway 会在应用启动时自动迁移
```

> 如需使用 MySQL，将 `application.yml` 中 `spring.profiles.active` 改为 `dev-mysql` 即可。

### 3. 启动后端

```bash
cd backend

# 复制环境变量模板并修改
cp .env.example .env
# 编辑 .env，填入数据库连接信息和 DeepSeek API Key

# 编译并运行
mvn clean package -DskipTests
java -jar target/core-0.0.1-SNAPSHOT.jar

# 或使用 Maven 直接运行
mvn spring-boot:run
```

后端默认启动在 **http://localhost:8080**

### 4. 启动前端

```bash
cd frontend

# 复制环境变量模板
cp .env.example .env

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认启动在 **http://localhost:5173**

### 5.（可选）启动 Python 行情同步服务

```bash
# 安装 Python 依赖
pip install requests sqlalchemy psycopg2-binary schedule -i https://pypi.tuna.tsinghua.edu.cn/simple

# 启动行情服务
python main.py
```

该服务提供：
- ⏱ 每 3 秒更新 A 股实时行情（新浪行情 API）
- 📰 每 60 秒抓取财经新闻
- 📊 启动时自动补全 F10 公司资料

### 一键启动（Windows PowerShell）

```powershell
# 使用内置的开发脚本
.\scripts\dev.ps1
```

---

## 🔧 环境变量配置

### 后端 (`backend/.env`)

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `DB_URL` | ⬜ | `jdbc:postgresql://localhost:5432/fincoach` | JDBC 数据库连接 URL |
| `DB_USERNAME` | ⬜ | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | ✅ | — | 数据库密码 |
| `DEEPSEEK_API_KEY` | ⬜ | — | DeepSeek API Key（AI 咨询功能需要） |
| `SERVER_PORT` | ⬜ | `8080` | 服务端口 |

### 前端 (`frontend/.env`)

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `VITE_API_BASE_URL` | ✅ | `http://localhost:8080` | 后端 API 基础地址 |

---

## 📖 API 文档

后端集成了 **SpringDoc OpenAPI (Swagger)**，启动后可访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 主要 API 模块

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证 | `/api/auth` | 登录、注册、Token 刷新 |
| 用户 | `/api/user` | 用户信息、密码修改 |
| 资产 | `/api/assets` | 资产 CRUD、分类管理 |
| 健康诊断 | `/api/health` | 财务健康评分与建议 |
| 投资计划 | `/api/plans` | 投资计划生成与执行 |
| 行情 | `/api/market` | 证券列表、K 线、行情快照 |
| 投资 | `/api/investment` | 自选股、推荐 |
| 风险测评 | `/api/risk` | 风险偏好评估 |
| AI 咨询 | `/api/ai` | 智能对话 |
| 交易 | `/api/transactions` | 交易记录查询 |

---

## 🗄 数据库管理

项目使用 **Flyway** 进行数据库版本管理，支持 **PostgreSQL（默认）** 和 **MySQL** 双数据库：

```
backend/src/main/resources/db/migration/
├── postgresql/     # PostgreSQL 迁移脚本（默认，25+ 个文件）
│   ├── V20260215_01__base_tables.sql      # 基础表 (16 张)
│   ├── V20260216_02__create_fc_*.sql      # 业务表
│   └── ...
└── mysql/          # MySQL 迁移脚本（回切用）
```

- **首次启动**：Flyway 自动创建所有表结构，无需手动执行 SQL
- **切换数据库**：修改 `application.yml` 中的 `spring.profiles.active`
  - `dev-pg` → PostgreSQL（默认）
  - `dev-mysql` → MySQL
- 详细迁移指南见 [`doc/db-migration/mysql-to-pg-runbook.md`](doc/db-migration/mysql-to-pg-runbook.md)

---

## 🐍 Python 行情服务

`main.py` 是独立的行情数据同步服务，提供三个并行模块：

| 模块 | 周期 | 数据源 | 说明 |
|------|------|--------|------|
| 实时行情 | 3 秒 | 新浪行情 API | 更新 `market_security` 表 |
| 财经新闻 | 60 秒 | 新浪滚动新闻 | 写入 `financial_news` 表 |
| F10 资料 | 启动时 | 内置数据 + 爬取 | 补全 `company_profile` 表 |

> ⚠️ **注意**：行情同步服务依赖新浪 API，请确保网络环境可访问。详细依赖安装请参考 [依赖安装指南](依赖安装指南.md)。

---

## 📄 许可证

本项目仅用于学术研究与学习用途。

---

## 🤝 贡献

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m '新增 xxx 功能'`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 发起 Pull Request
