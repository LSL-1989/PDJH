/**
 * 诊室和设备 — 对齐原型 page-rooms
 * 诊室坐诊信息已接 GetSession.rooms；设备信息接口未接
 */
<template>
  <div class="nd-page">
    <div class="page-title">
      <svg class="ln-icon"><use href="#i-room" /></svg>
      诊室和设备
    </div>
    <div class="page-subtitle">诊室坐诊信息 · 设备运行状态</div>

    <div class="tabs">
      <button type="button" class="tab" :class="{ active: tab === 'info' }" @click="tab = 'info'">
        诊室坐诊信息
      </button>
      <button type="button" class="tab" :class="{ active: tab === 'device' }" @click="tab = 'device'">
        设备信息
      </button>
    </div>

    <div v-show="tab === 'info'">
      <div class="room-grid">
        <div v-for="room in enrichedRooms" :key="room.id" class="room-card">
          <div class="room-header">
            <span class="room-name">{{ room.name }}</span>
            <span
              class="room-status"
              :class="room.suspended ? 'tag-amber' : room.open ? 'tag-success' : 'tag-offline'"
            >
              {{ room.suspended ? '科室停诊' : room.open ? '接诊中' : '离线' }}
            </span>
          </div>
          <div class="room-doctor">
            <svg class="ln-icon"><use href="#i-plus" /></svg>
            {{ room.deptName || '未分组' }}
            <span v-if="room.floor || room.roomNo"> · {{ [room.floor, room.roomNo].filter(Boolean).join(' ') }}</span>
          </div>
          <div class="room-current">
            <div class="current-label">当前就诊</div>
            <div class="current-patient" :style="!room.open ? { color: 'var(--text-muted)' } : {}">—</div>
            <div class="current-wait">
              候诊 <strong>{{ room.waitingCount ?? '—' }}</strong>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!enrichedRooms.length" class="nd-empty">暂无诊室数据</div>
      <p class="nd-gap-hint" style="margin-top: 12px">
        「当前就诊患者 / 医生坐诊姓名」需扩展会话或诊室实时接口
      </p>
    </div>

    <div v-show="tab === 'device'">
      <div class="content-card">
        <div class="card-title">自助签到机</div>
        <div class="nd-empty">设备状态接口未接</div>
      </div>
      <div class="content-card">
        <div class="card-title">候诊大屏</div>
        <div class="nd-empty">设备状态接口未接</div>
      </div>
      <div class="content-card">
        <div class="card-title">诊室门边屏</div>
        <div class="nd-empty">设备状态接口未接</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  rooms: { type: Array, default: () => [] },
  departments: { type: Array, default: () => [] }
})

const tab = ref('info')

const deptMap = computed(() => {
  const m = new Map()
  for (const d of props.departments) m.set(d.id, d)
  return m
})

const enrichedRooms = computed(() =>
  (props.rooms || []).map((r) => {
    const dept = deptMap.value.get(r.deptId)
    return {
      ...r,
      open: r.state === 1,
      suspended: dept?.isSuspended === 1,
      deptName: dept?.name || ''
    }
  })
)
</script>
