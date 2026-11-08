<template>
  <div class="alert-inbox">
    <div class="alert-inbox__header">
      <div>
        <h2 class="nd-panel-title">待处理事项</h2>
        <p class="alert-inbox__sub">点击条目在下方处置，核心操作 ≤2 次点击</p>
      </div>
      <el-badge :value="alerts.length" :hidden="!alerts.length" type="danger">
        <span class="alert-inbox__count-label">告警</span>
      </el-badge>
    </div>
    <div ref="listRef" class="alert-inbox__list" :class="{ 'alert-inbox__list--compact': compact }">
      <button
        v-for="(item, idx) in alerts"
        :key="item.callListId || item.deptId || idx"
        type="button"
        class="alert-item nd-card"
        :class="{ 'alert-item--active': selectedKey === alertKey(item) }"
        @click="$emit('select', item)"
      >
        <div class="alert-item__row">
          <span
            class="alert-item__type"
            :style="{
              color: typeMeta(item).color,
              background: typeMeta(item).bg
            }"
          >
            <el-icon :size="14"><component :is="typeMeta(item).icon" /></el-icon>
            {{ typeLabel(item.alertType) }}
          </span>
          <span v-if="item.showNo" class="alert-item__no">{{ item.showNo }}</span>
        </div>
        <p class="alert-item__msg">{{ item.message }}</p>
        <p v-if="item.patientName" class="alert-item__patient">
          <el-icon :size="14"><User /></el-icon>
          {{ item.patientName }}
        </p>
        <span
          class="alert-item__severity"
          :style="{ color: severityMeta(item).color }"
        >
          <el-icon :size="12"><component :is="severityMeta(item).icon" /></el-icon>
          优先级 {{ item.severity ?? 1 }}
        </span>
      </button>
      <el-empty v-if="!alerts.length" description="暂无待处理告警" class="alert-inbox__empty" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { User } from '@element-plus/icons-vue'
import { ALERT_TYPE_LABELS, alertSeverityMeta, alertTypeMeta } from '../utils/queueStatus'

defineProps({
  alerts: { type: Array, default: () => [] },
  selectedKey: { type: String, default: '' },
  /** 未选患者时压缩高度，为能力矩阵留出空间 */
  compact: { type: Boolean, default: false }
})
defineEmits(['select'])

const listRef = ref(null)
defineExpose({ scrollToTop: () => listRef.value?.scrollTo({ top: 0, behavior: 'smooth' }) })

function typeLabel(t) {
  return ALERT_TYPE_LABELS[t] || t
}

function typeMeta(item) {
  return alertTypeMeta(item.alertType)
}

function severityMeta(item) {
  return alertSeverityMeta(item.severity)
}

function alertKey(item) {
  return item.callListId ? `c-${item.callListId}` : `d-${item.deptId}`
}
</script>

<style scoped>
.alert-inbox {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  border-bottom: 1px solid var(--nd-border);
}
.alert-inbox__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1rem 1rem 0.5rem;
  flex-shrink: 0;
}
.alert-inbox__sub {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  color: var(--nd-text-secondary);
}
.alert-inbox__count-label {
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
  padding-right: 0.25rem;
}
.alert-inbox__list {
  flex: 1;
  overflow-y: auto;
  padding: 0 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-height: 120px;
  max-height: 42vh;
}
.alert-inbox__list--compact {
  max-height: 28vh;
}
.alert-item {
  width: 100%;
  text-align: left;
  padding: 0.875rem 1rem;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.1s;
}
.alert-item:hover {
  border-color: #90caf9;
  transform: translateX(2px);
}
.alert-item--active {
  border-color: var(--nd-primary);
  box-shadow: 0 0 0 1px var(--nd-primary), var(--nd-shadow);
}
.alert-item__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}
.alert-item__type {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
}
.alert-item__no {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--nd-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}
.alert-item__msg {
  margin: 0.5rem 0 0;
  font-size: var(--nd-font-body);
  color: var(--nd-text);
  line-height: 1.45;
}
.alert-item__patient {
  margin: 0.35rem 0 0;
  font-size: var(--nd-font-body);
  font-weight: 600;
  color: var(--nd-text);
  display: flex;
  align-items: center;
  gap: 4px;
}
.alert-item__severity {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 0.35rem;
  font-size: 0.75rem;
  font-weight: 500;
}
.alert-inbox__empty {
  padding: 1rem 0;
}
</style>
