<template>
  <div class="cap-ribbon nd-card">
    <span class="cap-ribbon__total">
      <el-icon><Grid /></el-icon>
      全站 <b>{{ total }}</b> 项能力
    </span>
    <div class="cap-ribbon__chips">
      <span
        v-for="g in CAPABILITY_GROUPS"
        :key="g.id"
        class="cap-ribbon__chip"
        :style="{ '--c': g.color, background: g.bg }"
      >
        {{ g.name }}
        <em>{{ g.items.length }}</em>
      </span>
    </div>
    <button type="button" class="cap-ribbon__link" @click="$emit('show-hub')">
      查看矩阵
      <el-icon><ArrowRight /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { ArrowRight, Grid } from '@element-plus/icons-vue'
import { CAPABILITY_GROUPS, TOTAL_CAPABILITIES } from '../constants/deskCapabilities'

defineEmits(['show-hub'])
const total = TOTAL_CAPABILITIES
</script>

<style scoped>
.cap-ribbon {
  margin: 0 1rem 0.75rem;
  padding: 0.65rem 1rem;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  background: linear-gradient(90deg, #fff 0%, #f0f7ff 100%);
}
.cap-ribbon__total {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.8125rem;
  color: var(--nd-text);
  white-space: nowrap;
}
.cap-ribbon__total b {
  color: var(--nd-primary);
  font-size: 1.125rem;
}
.cap-ribbon__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  flex: 1;
}
.cap-ribbon__chip {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  color: var(--c);
}
.cap-ribbon__chip em {
  font-style: normal;
  font-weight: 800;
  margin-left: 2px;
}
.cap-ribbon__link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: none;
  background: none;
  color: var(--nd-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  white-space: nowrap;
}
.cap-ribbon__link:hover {
  text-decoration: underline;
}
</style>
