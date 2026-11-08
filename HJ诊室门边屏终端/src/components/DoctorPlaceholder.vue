<script setup lang="ts">
/** 无值班人员时的占位：保留值班区结构，遮罩提示未上岗 */
</script>

<template>
  <section class="doctor-placeholder clinic-panel">
    <div class="doctor-top">
      <div class="avatar-skeleton" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="9" r="4" stroke="currentColor" stroke-width="1.6" />
          <path d="M5 20c0-3.3 3.1-6 7-6s7 2.7 7 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </div>
      <div class="info-skeleton">
        <div class="line line-name" />
        <div class="line line-title" />
      </div>
    </div>
    <div class="intro-skeleton" />
    <div class="specialty-skeleton" />
    <div class="duty-mask">
      <span class="duty-mask-text">暂无值班</span>
    </div>
  </section>
</template>

<style scoped>
.doctor-placeholder {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  padding: 14px 16px;
  background:
    linear-gradient(var(--color-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-grid-line) 1px, transparent 1px),
    var(--color-doctor-panel);
  background-size: 24px 24px, 24px 24px, auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.doctor-top {
  display: flex;
  gap: 14px;
  align-items: stretch;
  opacity: 0.4;
}

.avatar-skeleton {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-surface-elevated), var(--color-surface));
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.avatar-skeleton svg {
  width: 40px;
  height: 40px;
}

.info-skeleton {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  height: 88px;
}

.line {
  height: 14px;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-surface-elevated), var(--color-surface));
}

.line-name {
  width: 55%;
  height: 22px;
}

.line-title {
  width: 35%;
  height: 18px;
}

.intro-skeleton,
.specialty-skeleton {
  height: 40px;
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--color-surface-elevated), var(--color-surface));
  opacity: 0.35;
}

.duty-mask {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: var(--color-mask);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.duty-mask-text {
  font-size: 32px;
  font-weight: 800;
  color: var(--color-text-secondary);
  letter-spacing: 2px;
}
</style>
