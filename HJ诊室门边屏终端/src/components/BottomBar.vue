<script setup lang="ts">
defineProps<{
  deviceCode: string
}>()
</script>

<template>
  <footer class="bottom-bar">
    <div class="device-code">
      <span class="label">设备编码</span>
      <span class="value">{{ deviceCode || '—' }}</span>
    </div>
    <div class="support-text">技术支持：南京吾爱网络技术有限公司</div>
  </footer>
</template>

<style scoped>
.bottom-bar {
  flex: 0 0 40px;
  height: 40px;
  width: 100%;
  margin: 0;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--color-footer-bg);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  box-sizing: border-box;
}

.device-code {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-code .label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-footer-text-muted);
}

.device-code .value {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-footer-text);
  font-family: var(--font-mono);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.support-text {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-footer-text-muted);
  white-space: nowrap;
}
</style>
