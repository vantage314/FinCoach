# BugFix: 后端服务拒绝连接 (8080)

## bug描述
前端请求 API 时报错：`[vite] http proxy error ... ECONNREFUSED`，指向 `localhost:8080`。

## bug原因
Spring Boot 后端服务意外停止运行，导致 TCP 端口 8080 无法建立连接。

## 解决方案
手动终止残余的 Java 进程（PID 26500），并执行 `mvn spring-boot:run` 重新启动 `CoreApplication`。

## 总结
在进行压力测试或频繁修改代码热加载后，后端服务可能会崩溃或挂起。开发过程中应关注控制台日志，并确保本地服务存活。
