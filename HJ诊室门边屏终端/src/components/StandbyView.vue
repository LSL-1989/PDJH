<script setup lang="ts">
import { computed } from 'vue'
import type { StandbyReason } from '../types/clinic'
import TopBar from './TopBar.vue'
import BottomBar from './BottomBar.vue'

const props = defineProps<{
  reason: StandbyReason
  message?: string
  deviceCode?: string
  hospitalName: string
  hospitalLogo?: string
}>()

const reasonTitle: Record<StandbyReason, string> = {
  no_device_code: '未配置设备编号',
  not_registered: '设备未注册',
  not_bound: '尚未绑定诊室',
  pending: '等待管理员配置',
  frozen: '设备已停用',
  network_error: '网络连接失败',
  unknown: '加载失败'
}

const reasonDetail: Record<StandbyReason, string> = {
  no_device_code: '请完成设备绑定后再使用本终端',
  not_registered: '请联系管理员在【屏幕管理】登记本设备',
  not_bound: '请在【屏幕管理】将本设备绑定到物理诊室',
  pending: '请在【屏幕管理】完成终端类型与诊室绑定',
  frozen: '本终端已被停用，请联系管理员',
  network_error: '请检查网络连接，系统将自动重试',
  unknown: '请稍后刷新或联系管理员'
}

const roomLabel = computed(() => {
  const map: Record<StandbyReason, string> = {
    no_device_code: '设备未配置',
    not_registered: '设备待注册',
    not_bound: '诊室待绑定',
    pending: '诊室待配置',
    frozen: '设备已停用',
    network_error: '连接异常',
    unknown: '状态异常'
  }
  return map[props.reason]
})

const displayDetail = computed(() => {
  if (props.message && props.reason !== 'unknown') return props.message
  if (props.message && props.reason === 'unknown') return props.message
  return reasonDetail[props.reason]
})

const iconType = computed(() => {
  if (props.reason === 'network_error') return 'network'
  if (props.reason === 'frozen') return 'stop'
  return 'info'
})
</script>

<template>
  <div class="standby-screen">
    <TopBar
      :hospital-name="hospitalName"
      :room-name="roomLabel"
      :hospital-logo="hospitalLogo"
    />
    <main class="standby-main">
      <div class="status-card">
        <div class="status-icon" :class="iconType" aria-hidden="true">
          <svg v-if="iconType === 'network'" viewBox="0 0 24 24" fill="none">
            <path d="M2 8l10-6 10 6-10 6-10-6z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
            <path d="M6 12v4c0 1.1 2.7 2 6 2s6-.9 6-2v-4" stroke="currentColor" stroke-width="1.8" />
          </svg>
          <svg v-else-if="iconType === 'stop'" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8" />
            <path d="M9 9v6M15 9v6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8" />
            <path d="M12 8v5M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
        <h1 class="status-title">{{ reasonTitle[reason] }}</h1>
        <p class="status-detail">{{ displayDetail }}</p>
        <div v-if="deviceCode" class="device-box">
          <span class="device-label">设备编码</span>
          <span class="device-value">{{ deviceCode }}</span>
        </div>
        <p class="patient-hint">
          <svg class="hint-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M12 3a5 5 0 0 1 5 5v2h1a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h1V8a5 5 0 0 1 5-5z" stroke="currentColor" stroke-width="1.6" />
            <path d="M9 10V8a3 3 0 1 1 6 0v2" stroke="currentColor" stroke-width="1.6" />
          </svg>
          请到候诊区留意叫号信息
        </p>
      </div>
    </main>
    <BottomBar
      :device-code="deviceCode || '—'"
    />
  </div>
</template>

<style scoped>
.standby-screen {
  width: 100%;
  height: 100%;
  background: var(--color-background);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.standby-main {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.status-card {
  width: 100%;
  padding: 32px 24px 28px;
  background: var(--color-card-surface);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  text-align: center;
}

.status-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.status-icon.info {
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  box-shadow: var(--shadow-glow);
}

.status-icon.network {
  background: linear-gradient(135deg, #d97706, var(--color-warning));
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.3);
}

.status-icon.stop {
  background: linear-gradient(135deg, #f87171, var(--color-error));
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.25);
}

.status-icon svg {
  width: 36px;
  height: 36px;
}

.status-title {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.status-detail {
  margin: 0 0 20px;
  font-size: 15px;
  line-height: 1.65;
  color: var(--color-text-secondary);
  padding: 0 8px;
}

.device-box {
  margin: 0 0 20px;
  padding: 14px 16px;
  background: var(--color-primary-dim);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
}

.device-label {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.device-value {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: var(--font-mono);
  word-break: break-all;
}

.patient-hint {
  margin: 0;
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-primary-dim);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.hint-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: var(--color-primary);
}
</style>
