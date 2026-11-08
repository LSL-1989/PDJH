/**
 * 顶栏：HIS/语音状态、通知、时钟、用户
 * 消费者：Desk.vue；对齐原型 topbar
 */
<template>
  <header class="topbar">
    <div class="topbar-system">
      <div class="sys-dot" :class="{ offline: !hisOk }" />
      <span class="sys-label">HIS连接</span>
      <span class="sys-value" :class="{ bad: !hisOk }">{{ hisOk ? '正常' : '异常' }}</span>
    </div>
    <div class="topbar-sep" />
    <div class="topbar-system">
      <div class="sys-dot" :class="{ offline: !voiceOk }" />
      <span class="sys-label">语音系统</span>
      <span class="sys-value" :class="{ bad: !voiceOk }">{{ voiceOk ? '正常' : '未知' }}</span>
    </div>

    <div class="topbar-spacer" />

    <button type="button" class="topbar-notify" title="系统通知" @click="$emit('notify')">
      <svg class="bell ln-icon" style="font-size: 20px; position: relative">
        <use href="#i-bell" />
      </svg>
      <span v-if="alertCount > 0" class="bell-dot" />
      <span style="font-size: 12px; color: var(--text-secondary)">
        {{ alertCount > 0 ? `${alertCount}条新通知` : '无新通知' }}
      </span>
    </button>

    <div class="topbar-time">
      <div class="clock">{{ timeStr }}</div>
      <div class="date">{{ dateStr }}</div>
    </div>

    <div class="topbar-user">
      <div class="avatar">{{ avatarChar }}</div>
      <div class="user-info">
        <div class="user-name">{{ userName || '护士' }}</div>
        <div class="user-role">{{ zoneName || '诊区' }} · 分诊台</div>
      </div>
      <button
        v-if="canSwitchZone"
        type="button"
        class="topbar-switch"
        title="切换当班诊区"
        @click="$emit('switch-zone')"
      >
        切换诊区
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  userName: { type: String, default: '' },
  zoneName: { type: String, default: '' },
  alertCount: { type: Number, default: 0 },
  canSwitchZone: { type: Boolean, default: false },
  hisOk: { type: Boolean, default: true },
  voiceOk: { type: Boolean, default: true }
})
defineEmits(['notify', 'switch-zone'])

const now = ref(new Date())
let timer = null

const avatarChar = computed(() => (props.userName || '护').slice(0, 1))
const dateStr = computed(() => {
  const d = now.value
  const w = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 星期${w}`
})
const timeStr = computed(() => {
  const d = now.value
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

function pad(n) {
  return String(n).padStart(2, '0')
}

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.topbar {
  height: var(--topbar-h);
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  padding: 0 var(--space-6);
  gap: var(--space-4);
  flex-shrink: 0;
}
.topbar-system {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.sys-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  animation: pulse-dot 2s infinite;
}
.sys-dot.offline {
  background: var(--coral);
  animation: none;
}
@keyframes pulse-dot {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(38, 166, 154, 0.4);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(38, 166, 154, 0);
  }
}
.sys-label {
  font-size: 12px;
  color: var(--text-secondary);
}
.sys-value {
  font-size: 12px;
  color: var(--success);
  font-weight: 600;
}
.sys-value.bad {
  color: var(--coral);
}
.topbar-sep {
  width: 1px;
  height: 24px;
  background: var(--glass-border);
}
.topbar-notify {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  transition: background var(--fast);
  background: none;
  border: none;
  color: inherit;
  font-family: inherit;
  position: relative;
}
.topbar-notify:hover {
  background: var(--glass-hover);
}
.bell-dot {
  position: absolute;
  top: 2px;
  left: 18px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--coral);
}
.topbar-spacer {
  flex: 1;
}
.topbar-time {
  text-align: right;
}
.topbar-time .clock {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
}
.topbar-time .date {
  font-size: 11px;
  color: var(--text-muted);
}
.topbar-user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background: var(--glass-hover);
  border-radius: var(--radius-lg);
  border: 1px solid var(--glass-border);
}
.topbar-user .avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--indigo), var(--magenta));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}
.topbar-user .user-info {
  line-height: 1.3;
}
.topbar-user .user-name {
  font-size: 13px;
  font-weight: 600;
}
.topbar-user .user-role {
  font-size: 11px;
  color: var(--text-muted);
}
.topbar-switch {
  margin-left: 4px;
  border: 1px solid var(--glass-border);
  background: transparent;
  color: inherit;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: inherit;
}
.topbar-switch:hover {
  background: var(--glass-hover);
}
@media (max-width: 640px) {
  .topbar-user .user-info {
    display: none;
  }
}
</style>
