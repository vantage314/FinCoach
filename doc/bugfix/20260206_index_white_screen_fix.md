# BugFix: 前端 Index.vue 白屏问题

## bug描述
打开“我的投资计划”页面时出现白屏，控制台报错：`Uncaught ReferenceError: ref is not defined at Index.vue:84`。

## bug原因
在 `frontend/src/views/plan/Index.vue` 中使用了 `ref` 响应式变量，但在 `<script setup>` 顶部没有从 `vue` 中引入 `ref`。

## 解决方案
在 `Index.vue` 的 `<script setup>` 顶部添加 `import { ref, reactive, onMounted, onActivated } from 'vue';`，并顺便引入了缺失的业务组件 `CreatePlanDialog` 和 `PlanDetailDialog`。

## 总结
在编写 Vue3 组合式 API 时，必须确保所有使用的 `ref`, `reactive`, `computed` 等都已正确导入。后续开发中需加强 ESlint 检查以避免此类低级错误。
