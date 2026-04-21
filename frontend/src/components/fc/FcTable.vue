<template>
  <el-table class="fc-table" v-bind="$attrs">
    <slot />
  </el-table>
</template>

<script setup lang="ts">
defineOptions({ name: 'FcTable', inheritAttrs: false })
</script>

<style scoped>
.fc-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
}
</style>
