# 20260207 - 数据基础设施全面升级

## 完成内容

### Step 1: 数据库全量重构 (SQL)
创建 `sql/migration/20260207_full_data_init.sql`：
- 清空旧数据，修复乱码
- 新建 `financial_report` (财务报表) 和 `company_notice` (公司公告) 表
- 插入 10 条真实头部公司数据 (宁德时代、贵州茅台、腾讯控股等)
- 存储过程批量生成 90 条仿真数据，共计 100 条记录

### Step 2: 后端逻辑升级 (Java)
**新建实体类：**
- `FinancialReport.java` - 财务报表实体
- `CompanyNotice.java` - 公司公告实体

**新建 Mapper：**
- `FinancialReportMapper.java`
- `CompanyNoticeMapper.java`

**更新 Service 层：**
- `InvestmentService` 接口增加 `getFinancialReports` 和 `getCompanyNotices`
- `InvestmentServiceImpl` 实现查询逻辑

**更新 Controller：**
- `GET /api/invest/finance/{code}` - 获取财务报表
- `GET /api/invest/notice/{code}` - 获取公司公告

### Step 3: 前端交互完善 (Vue)
**更新 API 层：**
- `api/invest.ts` 增加 `getFinancialReports` 和 `getCompanyNotices`

**更新 StockDetail.vue：**
- 财务摘要 Tab：使用 `el-table` 展示 `reportList`
- 公司公告 Tab：点击打开公告 (模拟 PDF)
- 懒加载：Tab 切换时才请求对应数据

## 涉及文件
| 类型 | 文件路径 |
|------|---------|
| SQL | `sql/migration/20260207_full_data_init.sql` |
| Entity | `repository/entity/FinancialReport.java` |
| Entity | `repository/entity/CompanyNotice.java` |
| Mapper | `repository/mapper/FinancialReportMapper.java` |
| Mapper | `repository/mapper/CompanyNoticeMapper.java` |
| Service | `service/InvestmentService.java` |
| Service | `service/impl/InvestmentServiceImpl.java` |
| Controller | `controller/InvestmentController.java` |
| API | `frontend/src/api/invest.ts` |
| View | `frontend/src/views/market/StockDetail.vue` |

## 验证步骤
1. 在 MySQL 客户端执行 `20260207_full_data_init.sql` 脚本
2. 重启后端服务
3. 访问 **市场行情** → 点击股票进入详情页
4. 验证 F10、财务摘要、公司公告 Tab 数据加载
