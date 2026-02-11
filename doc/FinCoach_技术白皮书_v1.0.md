# FinCoach 项目现状技术白皮书

> **生成日期**: 2026-02-08  
> **版本**: v1.0

---

## 1. 数据库架构 (Database Schema)

### 1.1 核心表清单

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `user` | 用户账户 | id, username, password (BCrypt), email, phone |
| `asset_category` | 资产分类字典 | id, name, icon_slug, description |
| `asset_item` | 资产明细 | id, user_id, category_id, asset_name, current_value, stock_code, quantity, cost_price, market_value |
| `risk_assessment` | 风险测评 | id, user_id, total_score, risk_level, assessment_json |
| `investment_plan` | 投资计划 | id, user_id, plan_type, target_amount, status |
| `ai_log` | AI 对话记录 | id, user_id, prompt_text, response_text |
| `market_security` | 证券行情 | code, name, type, current_price, change_percent, sector, risk_level |
| `financial_news` | 财经新闻 | id, title, source, content, related_code, publish_time |
| `company_profile` | F10 公司资料 | stock_code, name, business_scope, registered_capital |
| `financial_report` | 财务报表 | stock_code, report_year, total_revenue, net_profit |
| `company_notice` | 公司公告 | stock_code, title, content, publish_time |
| `transaction_record` | 交易流水 | id, user_id, amount, type, description |
| `user_watchlist` | 用户自选股 | user_id, stock_code |

### 1.2 DDL 建表语句 (核心表)

```sql
-- 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100),
    `phone` VARCHAR(20),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

-- 资产明细 (Phase 14 升级版)
CREATE TABLE `asset_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` INT NOT NULL,
    `asset_name` VARCHAR(100) NOT NULL,
    `current_value` DECIMAL(18,2) NOT NULL,
    `stock_code` VARCHAR(20),      -- Phase 14 新增
    `quantity` DECIMAL(18,4),      -- Phase 14 新增
    `cost_price` DECIMAL(18,4),    -- Phase 14 新增
    `market_value` DECIMAL(18,2),  -- Phase 14 新增
    `sub_type` VARCHAR(20),
    PRIMARY KEY (`id`)
);

-- 证券行情
CREATE TABLE `market_security` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `type` ENUM('STOCK','FUND','BOND','INDEX') NOT NULL,
    `current_price` DECIMAL(18,4),
    `change_percent` DECIMAL(10,4),
    `sector` VARCHAR(50),
    `risk_level` ENUM('LOW','MEDIUM','HIGH')
);
```

---

## 2. 后端接口清单 (Backend APIs)

### 2.1 接口总览

| 模块 | 基础路径 | 接口数 | 实现状态 |
|------|----------|--------|----------|
| 身份认证 | `/api/auth` | 3 | ✅ 完整实现 |
| 资产管理 | `/api/asset` | 5 | ✅ 完整实现 |
| 市场中心 | `/api/market` | 1 | ✅ 完整实现 |
| AI 咨询 | `/api/ai` | 1 | ✅ 完整实现 (RAG) |
| 投资计划 | `/api/plan` | 4 | ✅ 完整实现 |
| 用户管理 | `/api/user` | 3 | ✅ 完整实现 |
| 风险测评 | `/api/risk` | 2 | ✅ 完整实现 |
| 智能投资 | `/api/invest` | 7 | ✅ 完整实现 |
| 健康体检 | `/api/health` | 1 | ✅ 完整实现 |
| 交易流水 | `/api/transactions` | 1 | ✅ 完整实现 |

### 2.2 详细接口列表

#### 2.2.1 身份认证 (`AuthController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 (BCrypt 加密) | ✅ |
| POST | `/api/auth/login` | 用户登录 (返回 JWT) | ✅ |
| GET | `/api/auth/test` | 健康检测 | ✅ |

#### 2.2.2 资产管理 (`AssetItemController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/asset/add` | 录入新资产 | ✅ |
| GET | `/api/asset/list` | 获取资产列表 | ✅ |
| GET | `/api/asset/summary` | 获取组合统计 | ✅ |
| DELETE | `/api/asset` | 批量删除 | ✅ |
| GET | `/api/asset/analysis` | 资产全景分析 + 健康体检 | ✅ Phase 14 |

#### 2.2.3 市场中心 (`MarketController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| GET | `/api/market/securities` | 证券列表 (分页+搜索) | ✅ |

#### 2.2.4 AI 咨询 (`AiController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/ai/chat` | RAG 智能对话 (DeepSeek) | ✅ Phase 13.2 |

#### 2.2.5 投资计划 (`InvestmentPlanController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/plan/generate` | 生成调仓计划 | ✅ |
| POST | `/api/plan/save` | 保存计划 | ✅ |
| GET | `/api/plan/list` | 查询历史 | ✅ |
| POST | `/api/plan/execute` | 执行计划 | ✅ |

#### 2.2.6 用户管理 (`UserController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/user/update` | 更新资料 | ✅ |
| POST | `/api/user/change-password` | 修改密码 | ✅ |
| POST | `/api/user/reset-data` | 重置演示数据 | ✅ |

#### 2.2.7 风险测评 (`RiskAssessmentController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/risk/assess` | 提交测评 | ✅ |
| GET | `/api/risk/latest` | 获取最新结果 | ✅ |

#### 2.2.8 智能投资 (`InvestmentController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| GET | `/api/invest/news/list` | 财经新闻列表 | ✅ |
| GET | `/api/invest/news/{id}` | 新闻详情 | ✅ |
| GET | `/api/invest/profile/{code}` | F10 公司资料 | ✅ |
| GET | `/api/invest/watchlist` | 自选股列表 | ✅ |
| POST | `/api/invest/watchlist/toggle` | 添加/移除自选 | ✅ |
| GET | `/api/invest/finance/{code}` | 财务报表 | ✅ |
| GET | `/api/invest/notice/{code}` | 公司公告 | ✅ |

#### 2.2.9 健康体检 (`HealthCheckController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| GET | `/api/health/check` | 四维健康度评估 | ✅ |

#### 2.2.10 交易流水 (`TransactionController`)
| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| GET | `/api/transactions` | 分页查询流水 | ✅ |

---

## 3. 前端路由与页面 (Frontend Structure)

### 3.1 路由清单

| 路径 | 组件 | 功能 | API 对接状态 |
|------|------|------|--------------|
| `/login` | `Login.vue` | 登录页 | ✅ 已对接 |
| `/register` | `Register.vue` | 注册页 | ✅ 已对接 |
| `/dashboard` | `dashboard/Index.vue` | 资产总览 | ✅ 已对接 |
| `/market` | `market/Index.vue` | 市场中心 | ✅ 已对接 |
| `/market/detail/:code` | `market/StockDetail.vue` | 证券详情 | ✅ 已对接 |
| `/diagnosis` | `diagnosis/Index.vue` | 智能诊断 | ✅ 已对接 |
| `/plan` | `plan/Index.vue` | 投资计划 | ✅ 已对接 |
| `/investment` | `investment/Index.vue` | 智能投资驾驶舱 | ✅ 已对接 |
| `/chat` | `chat/Index.vue` | AI 咨询 | ✅ 已对接 (RAG) |
| `/asset/analysis` | `asset/Analysis.vue` | 资产分析 + 体检 | ✅ Phase 14 |
| `/asset/manage` | `asset/Index.vue` | 资产明细管理 | ✅ Phase 14.5 |
| `/user/profile` | `user/Profile.vue` | 个人中心 | ✅ 已对接 |
| `/risk/assessment` | `risk/AssessmentWizard.vue` | 风险测评向导 | ✅ 已对接 |
| `/risk/result` | `risk/AssessmentResult.vue` | 测评结果 | ✅ 已对接 |

### 3.2 视图目录结构

```
views/
├── asset/          # 资产管理 (Analysis.vue, Index.vue)
├── chat/           # AI 咨询 (Index.vue)
├── dashboard/      # 资产总览仪表盘
├── diagnosis/      # 智能诊断
├── investment/     # 智能投资驾驶舱
├── market/         # 市场中心 (Index, StockDetail, SecurityDetail)
├── plan/           # 投资计划
├── portfolio/      # 组合管理
├── risk/           # 风险测评 (AssessmentWizard, AssessmentResult)
└── user/           # 个人中心
```

---

## 4. 核心业务逻辑 (Core Services)

### 4.1 AssetItemServiceImpl (资产服务)

| 方法 | 功能 | 实现状态 |
|------|------|----------|
| `addAsset()` | 录入资产，自动生成交易流水 | ✅ |
| `getUserAssets()` | 按条件查询用户资产 | ✅ |
| `getPortfolioSummary()` | 计算组合统计 (总额/占比/红线) | ✅ |
| `deleteAssets()` | 越权防御删除 | ✅ |
| `analyze()` | **Phase 14**: 实时估值 + 健康体检 | ✅ |
| `performHealthCheck()` | 四维健康评分算法 | ✅ |

### 4.2 AiService (AI 智能投顾)

| 核心功能 | 描述 |
|----------|------|
| **RAG 架构** | 检索增强生成，优先使用本地数据库上下文 |
| **证券匹配** | 自动识别用户消息中的股票代码/名称 |
| **新闻注入** | 检索关联新闻作为上下文 |
| **DeepSeek API** | 调用 `OpenAiClient.callChat()` |

### 4.3 MarketServiceImpl (行情服务)

| 方法 | 功能 |
|------|------|
| `getSecurities()` | 分页查询证券，支持类型过滤和关键词搜索 |

### 4.4 定时任务与异步

| 类型 | 状态 |
|------|------|
| `@Scheduled` 定时任务 | ⚠️ 未发现 |
| 异步任务 `@Async` | ⚠️ 未发现 |

---

## 5. 待办与技术债务 (TODOs)

### 5.1 代码中的 TODO

> 扫描结果：**未发现显式 `// TODO` 注释**

### 5.2 已识别的技术债务

| 问题 | 位置 | 风险等级 | 建议 |
|------|------|----------|------|
| 硬编码 `UserContext.getCurrentUserId()` | 多个 Controller | 🟡 中 | 统一通过拦截器注入 |
| 行情数据为静态种子数据 | `market_security` | 🟡 中 | 接入实时行情 API |
| DeepSeek API Key 配置 | `application.yml` | 🔴 高 | 迁移到环境变量 |
| 无定时任务刷新行情 | 全局 | 🟡 中 | 添加 `@Scheduled` 行情同步 |

### 5.3 功能完整性评估

| 模块 | 完整度 | 备注 |
|------|--------|------|
| 身份认证 | 100% | JWT + BCrypt |
| 资产管理 | 100% | CRUD + 分析 + 体检 |
| 市场中心 | 90% | 缺实时行情 |
| AI 咨询 | 100% | RAG 已实现 |
| 风险测评 | 100% | 一票否决 + 分值映射 |
| 投资计划 | 100% | 生成 + 执行 |
| 智能投资 | 100% | 新闻+F10+自选股 |

---

## 6. SQL 脚本清单

| 脚本 | 用途 |
|------|------|
| `init/01_initial_tables.sql` | 初始化核心表结构 |
| `migration/20260203_*` | 资产分类升级 |
| `migration/20260204_risk_assessment.sql` | 风险测评表 |
| `migration/20260205_*` | 投资计划 + 市场证券 |
| `migration/20260206_*` | 行情注入 + 交易流水 |
| `migration/20260207_full_data_init.sql` | 全量演示数据 |
| `migration/20260208_asset_upgrade.sql` | Phase 14 资产字段升级 |
| `migration/20260208_fix_encoding.sql` | 乱码修复 |

---

## 7. 技术栈汇总

| 层 | 技术 |
|----|------|
| **后端框架** | Spring Boot 3 + MyBatis-Plus |
| **认证** | JWT (io.jsonwebtoken) + BCrypt |
| **数据库** | MySQL 8 (utf8mb4) |
| **前端框架** | Vue 3 + Element Plus |
| **构建工具** | Maven (后端) + Vite/Webpack (前端) |
| **AI 集成** | DeepSeek API (OpenAI 兼容格式) |
| **图表** | ECharts |

---

*文档由 AI 自动生成于 2026-02-08*
