<script setup lang="ts">
import ScreenFrame from './components/ScreenFrame.vue'
import { isClinicPreview } from './utils/clinicPreview'

const previewMode = isClinicPreview()
</script>

<template>
  <ScreenFrame>
    <router-view />
  </ScreenFrame>
  <div v-if="previewMode" class="clinic-preview-badge">预览观察 · 不参与绑定与叫号</div>
</template>

<style>
html,
body,
#app {
  width: 100%;
  height: 100%;
  min-height: var(--viewport-height, 100vh);
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: var(--color-frame-outer);
}

.clinic-preview-badge {
  position: fixed;
  top: 12px;
  left: 12px;
  z-index: 300;
  pointer-events: none;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  box-shadow: 0 4px 16px rgba(12, 74, 110, 0.25);
}
</style>
