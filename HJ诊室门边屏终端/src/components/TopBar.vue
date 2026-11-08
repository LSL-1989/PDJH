<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { resolveMediaUrl } from '../utils/resolveMediaUrl'

const props = defineProps<{
  hospitalName: string
  roomName: string
  hospitalLogo?: string
}>()

const dateText = ref('')
const timeText = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const logoSrc = computed(() => resolveMediaUrl(props.hospitalLogo))

const pad = (n: number) => String(n).padStart(2, '0')

const tick = () => {
  const d = new Date()
  dateText.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  timeText.value = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <header class="top-bar">
    <div class="top-bar-brand">
      <div class="logo-wrap" aria-label="医院Logo">
        <img v-if="logoSrc" :src="logoSrc" alt="" class="logo-img" />
        <svg v-else viewBox="0 0 24 24" fill="none" class="logo-svg">
          <path d="M12 6v12M6 12h12" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
        </svg>
      </div>
      <div class="brand-text">
        <div class="hospital">{{ hospitalName }}</div>
        <div class="room">{{ roomName }}</div>
      </div>
    </div>
    <div class="time-block">
      <span class="time-value">{{ timeText }}</span>
      <span class="date-value">{{ dateText }}</span>
    </div>
  </header>
</template>

<style scoped>
.top-bar {
  flex: 0 0 auto;
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--color-header-bg);
  color: var(--color-header-text);
  border-bottom: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: var(--shadow-header);
}

.top-bar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.logo-wrap {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.logo-svg {
  width: 28px;
  height: 28px;
  color: var(--color-header-text);
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-text {
  min-width: 0;
}

.hospital {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.room {
  font-size: 15px;
  font-weight: 600;
  margin-top: 2px;
  opacity: 0.92;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-block {
  flex-shrink: 0;
  width: 118px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-left: 10px;
  text-align: center;
}

.time-value {
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  letter-spacing: 0.5px;
}

.date-value {
  font-size: 12px;
  opacity: 0.88;
  font-variant-numeric: tabular-nums;
}
</style>
