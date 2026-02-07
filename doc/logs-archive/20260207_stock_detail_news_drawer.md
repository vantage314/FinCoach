# 20260207 - 证券详情页 & 新闻抽屉实现

## 完成内容

### 1. 专业级证券详情页 (`StockDetail.vue`)
- **K线周期切换**：顶部工具栏支持 `分时 / 日K / 周K / 月K` 切换
- **F10 档案**：底部 Tabs 展示公司简况、财务摘要、公司公告
- **五档盘口**：右侧实时买卖盘模拟
- **自选功能**：调用 `getWatchlist` / `toggleWatchlist` 真实接口
- **实时时间**：显示北京时间，每秒刷新

### 2. 新闻阅读抽屉 (`Investment/Index.vue`)
- **新闻快讯区**：调用 `getNewsList` 接口获取真实新闻列表
- **抽屉组件**：点击新闻弹出 `el-drawer` 展示详情
- **关联标的**：支持跳转相关股票详情页

## 涉及文件
- `frontend/src/views/market/StockDetail.vue` (全量替换)
- `frontend/src/views/investment/Index.vue` (全量替换)

## 验证步骤
1. 访问 **市场行情** 页面，点击任意股票进入详情
2. 验证 K 线图切换、F10 资料加载、自选功能
3. 访问 **智能投资** 页面，点击新闻查看抽屉弹出
