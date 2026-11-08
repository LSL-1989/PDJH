<template>
  <div class="app-root">
    <ScreenFrame>
      <router-view />
    </ScreenFrame>
    <div v-if="!bootReady" class="boot-layer" aria-live="polite">
      <div class="boot-card neu-raised">
        <div class="boot-spinner" aria-hidden="true" />
        <p class="boot-text">正在加载终端…</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import router from './router'
import ScreenFrame from './components/ScreenFrame.vue'

const bootReady = ref(false)

onMounted(() => {
  void router.isReady().finally(() => {
    bootReady.value = true
  })
})
</script>

<style>
html,
body,
#app {
  width: 100%;
  height: 100%;
  min-height: var(--viewport-height, 100vh);
  margin: 0;
  padding: 0;
  background-color: var(--neu-bg, #0b1520);
  overflow: hidden;
}

.app-root {
  width: 100%;
  height: 100%;
  min-height: var(--viewport-height, 100vh);
  position: relative;
}

.boot-layer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
  display: -webkit-flex;
  display: flex;
  -webkit-align-items: center;
  align-items: center;
  -webkit-justify-content: center;
  justify-content: center;
  background: rgba(11, 21, 32, 0.88);
}

@supports ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
  .boot-layer {
    -webkit-backdrop-filter: blur(8px);
    backdrop-filter: blur(8px);
  }
}

.boot-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 40px;
  border-radius: 24px;
}

.boot-spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid rgba(45, 212, 191, 0.2);
  border-top-color: var(--neu-accent, #2dd4bf);
  animation: boot-spin 0.8s linear infinite;
}

.boot-text {
  margin: 16px 0 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--neu-muted, rgba(226, 232, 240, 0.72));
}

@keyframes boot-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
