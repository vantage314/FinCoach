<template>
  <el-card class="fc-card" shadow="never" v-bind="$attrs">
    <template v-if="$slots.header" #header>
      <div class="fc-card-header">
        <slot name="header" />
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'FcCard', inheritAttrs: false })
</script>
