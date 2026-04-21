<template>
  <el-select class="fc-select" v-bind="$attrs">
    <slot />
  </el-select>
</template>

<script setup lang="ts">
defineOptions({ name: 'FcSelect', inheritAttrs: false })
</script>
