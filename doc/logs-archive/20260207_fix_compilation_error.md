# 任务日志 - 2026-02-07 修复编译错误

## 问题描述
用户反馈 `com.fincoach.core.common.Result` 类中找不到 `fail(String)` 方法，导致 `InvestmentController.java` 编译失败。

## 原因分析
检查 `Result.java` 源码发现，该类仅定义了：
- `success(T data)`
- `success(T data, String message)`
- `error(Integer code, String message)`

并不存在 `fail` 方法。

## 解决方案
修改 `backend/src/main/java/com/fincoach/core/controller/InvestmentController.java`，将 `Result.fail("...")` 替换为 `Result.error(400, "...")`。

```java
// 修改前
return Result.fail("股票代码不能为空");

// 修改后
return Result.error(400, "股票代码不能为空");
```

## 验证结论
代码已修正，应能通过编译。
