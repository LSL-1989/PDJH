<template>
  <header class="desk-topbar">
    <div class="desk-topbar__brand">
      <div class="desk-topbar__logo" aria-hidden="true">
        <el-icon :size="22"><FirstAidKit /></el-icon>
      </div>
      <div>
        <h1 class="desk-topbar__title">
          {{ zoneName || '护士工作站' }}
        </h1>
        <p class="desk-topbar__subtitle">
          <el-icon class="desk-topbar__inline-icon"><User /></el-icon>
          {{ userName }} · {{ role || '护士' }}
        </p>
      </div>
    </div>

    <div class="desk-topbar__clock">
      <span class="desk-topbar__date">{{ dateStr }}</span>
      <span class="desk-topbar__time">{{ timeStr }}</span>
    </div>

    <div class="desk-topbar__actions">
      <el-input
        ref="searchInputRef"
        v-model="searchModel"
        class="desk-topbar__search"
        placeholder="卡号 / 排队号 / 姓名"
        clearable
        :prefix-icon="Search"
        @keyup.enter="$emit('search')"
      />
      <el-badge :value="alertCount" :hidden="!alertCount" :max="99" class="desk-topbar__badge">
        <el-button class="desk-topbar__notify" circle :icon="Bell" @click="$emit('scroll-alerts')" />
      </el-badge>
      <el-button type="primary" size="large" class="desk-topbar__btn-primary" @click="$emit('sign-in')">
        签到兜底
      </el-button>
      <el-button size="large" :loading="refreshing" :icon="Refresh" @click="$emit('refresh')">
        刷新
      </el-button>
      <el-button size="large" text @click="$emit('logout')">退出</el-button>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Bell, FirstAidKit, Refresh, Search, User } from '@element-plus/icons-vue'

const props = defineProps({
  zoneName: { type: String, default: '' },
  userName: { type: String, default: '' },
  role: { type: String, default: '' },
  alertCount: { type: Number, default: 0 },
  refreshing: { type: Boolean, default: false },
  quickSearch: { type: String, default: '' }
})
const emit = defineEmits(['update:quickSearch', 'search', 'sign-in', 'refresh', 'logout', 'scroll-alerts'])

const searchModel = computed({
  get: () => props.quickSearch,
  set: (v) => emit('update:quickSearch', v)
})

const now = ref(new Date())
let timer = null
const dateStr = computed(() => {
  const d = now.value
  const w = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 周${w}`
})
const timeStr = computed(() => {
  const d = now.value
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

function pad(n) {
  return String(n).padStart(2, '0')
}

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const searchInputRef = ref(null)
function focusSearch() {
  searchInputRef.value?.focus?.()
}
defineExpose({ focusSearch })
</script>

<style scoped>
.desk-topbar {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex-wrap: wrap;
  padding: 0.75rem 1.25rem;
  background: linear-gradient(135deg, #1565c0 0%, #1e88e5 100%);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(21, 101, 192, 0.35);
}
.desk-topbar__brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 200px;
}
.desk-topbar__logo {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}
.desk-topbar__title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.desk-topbar__cap-badge {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.95);
  letter-spacing: 0.02em;
}
.desk-topbar__subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.8125rem;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.desk-topbar__inline-icon {
  font-size: 0.875rem;
}
.desk-topbar__clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: auto;
  min-width: 140px;
}
.desk-topbar__date {
  font-size: 0.8125rem;
  opacity: 0.88;
}
.desk-topbar__time {
  font-size: 1.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}
.desk-topbar__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.desk-topbar__search {
  width: 220px;
}
.desk-topbar__search :deep(.el-input__wrapper) {
  border-radius: 8px;
}
.desk-topbar__notify {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
}
.desk-topbar__notify:hover {
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
}
.desk-topbar__btn-primary {
  min-width: 108px;
  font-weight: 600;
}
@media (max-width: 1200px) {
  .desk-topbar__clock {
    display: none;
  }
}
</style>
