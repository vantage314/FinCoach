# 2026-02-04: 修复 Java 编译错误

## 任务内容
修复后端 Java 项目编译时报告的语法错误。

## 完成情况

### 修复的文件

1. **AssetItemServiceImpl.java**
   - 修复第 135 行未闭合的字符串 `.last("LIMIT 1);` → `.last("LIMIT 1");`

2. **InvestmentPlanServiceImpl.java**
   - 移除文件末尾多余的闭合括号 `}`

3. **AssetItemService.java**
   - 添加缺失的 `deleteAssets` 方法声明

4. **InvestmentPlanService.java**
   - 添加缺失的 `import java.math.BigDecimal;`
   - 添加缺失的 `markExecuted` 方法声明

5. **pom.xml**
   - 将 Java 版本从 17 更新为 21（适配系统 JDK 版本）

## 验证结果
- 执行 `mvn compile` 编译成功 ✅
- BUILD SUCCESS

## 相关文档
- Bug 记录：`doc/bug/bug_tracking.md`
- Bugfix 详情：`doc/bugfix/20260204_java_compile_error_fix.md`
