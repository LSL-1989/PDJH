<template>
  <div class="overview-stats">
    <div
      v-for="item in items"
      :key="item.key"
      class="overview-stats__card nd-card"
      :style="{ '--accent': item.color }"
    >
      <div class="overview-stats__icon" :style="{ background: item.bg, color: item.color }">
        <el-icon :size="22"><component :is="item.icon" /></el-icon>
      </div>
      <div class="overview-stats__body">
        <span class="overview-stats__label">{{ item.label }}</span>
        <span class="overview-stats__value nd-stat-value" :style="{ color: item.color }">
          {{ item.value }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Clock, Bell, Warning, OfficeBuilding, RefreshLeft } from '@element-plus/icons-vue'

const props = defineProps({
  overview: { type: Object, default: () => ({}) }
})

const items = computed(() => [
  {
    key: 'waiting',
    label: '候诊人数',
    value: fmt(props.overview.waitingCount),
    color: '#1E88E5',
    bg: '#E3F2FD',
    icon: Clock
  },
  {
    key: 'unarrived',
    label: '未签到',
    value: fmt(props.overview.unarrivedCount),
    color: '#FB8C00',
    bg: '#FFF3E0',
    icon: Warning
  },
  {
    key: 'passed',
    label: '过号',
    value: fmt(props.overview.passedCount),
    color: '#E53935',
    bg: '#FFEBEE',
    icon: RefreshLeft
  },
  {
    key: 'alerts',
    label: '待处理告警',
    value: fmt(props.overview.alertCount),
    color: '#E53935',
    bg: '#FFEBEE',
    icon: Bell
  },
  {
    key: 'rooms',
    label: '开放诊室',
    value: `${fmt(props.overview.openRoomCount)}/${fmt(props.overview.totalRoomCount)}`,
    color: '#43A047',
    bg: '#E8F5E9',
    icon: OfficeBuilding
  }
])

function fmt(v) {
  return v == null || v === '' ? '—' : v
}
</script>

<style scoped>
.overview-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  padding: 1rem 1rem 0;
}
.overview-stats__card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.overview-stats__card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}
.overview-stats__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.overview-stats__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.overview-stats__label {
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
}
.overview-stats__value {
  font-size: 1.75rem;
}
</style>
