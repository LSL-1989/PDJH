<template>
  <div class="room-grid">
    <div class="room-grid__header">
      <h2 class="nd-panel-title">诊区诊室队列概览</h2>
      <span class="room-grid__hint">共 {{ rooms.length }} 间诊室</span>
    </div>

    <template v-for="group in groupedRooms" :key="group.deptId">
      <div class="room-grid__dept">
        <span class="room-grid__dept-name">{{ group.deptName }}</span>
        <el-tag
          v-if="group.suspended"
          size="small"
          effect="dark"
          class="room-grid__dept-tag"
          :style="{ background: '#FB8C00', border: 'none' }"
        >
          <el-icon class="room-grid__tag-icon"><Warning /></el-icon>
          停诊
        </el-tag>
      </div>
      <div class="room-grid__matrix">
        <article
          v-for="room in group.rooms"
          :key="room.id"
          class="room-card nd-card"
          :class="{ 'room-card--danger': room.congested, 'room-card--muted': room.state !== 1 }"
        >
          <div class="room-card__top">
            <div class="room-card__status" :style="{ background: statusOf(room, group.suspended).bg }">
              <el-icon :size="20" :style="{ color: statusOf(room, group.suspended).color }">
                <component :is="statusOf(room, group.suspended).icon" />
              </el-icon>
            </div>
            <div class="room-card__meta">
              <h3 class="room-card__name">{{ room.name }}</h3>
              <p v-if="room.floor || room.roomNo" class="room-card__loc">
                {{ [room.floor, room.roomNo].filter(Boolean).join(' · ') }}
              </p>
            </div>
          </div>
          <div class="room-card__footer">
            <span
              class="room-card__badge"
              :style="{
                color: statusOf(room, group.suspended).color,
                background: statusOf(room, group.suspended).bg
              }"
            >
              <el-icon :size="14"><component :is="statusOf(room, group.suspended).icon" /></el-icon>
              {{ statusOf(room, group.suspended).label }}
            </span>
            <div class="room-card__wait">
              <span class="room-card__wait-label">等候</span>
              <span class="room-card__wait-num">{{ room.waitingCount ?? '—' }}</span>
            </div>
          </div>
        </article>
      </div>
    </template>

    <el-empty v-if="!rooms.length" description="暂无诊室数据" class="room-grid__empty" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import { roomStatusMeta } from '../utils/queueStatus'

const props = defineProps({
  rooms: { type: Array, default: () => [] },
  departments: { type: Array, default: () => [] }
})

const deptMap = computed(() => {
  const m = new Map()
  for (const d of props.departments) {
    m.set(d.id, d)
  }
  return m
})

const groupedRooms = computed(() => {
  const groups = new Map()
  for (const room of props.rooms) {
    const deptId = room.deptId || '_unknown'
    if (!groups.has(deptId)) {
      const dept = deptMap.value.get(deptId)
      groups.set(deptId, {
        deptId,
        deptName: dept?.name || '未分组科室',
        suspended: dept?.isSuspended === 1,
        rooms: []
      })
    }
    groups.get(deptId).rooms.push(room)
  }
  return [...groups.values()]
})

function statusOf(room, deptSuspended) {
  return roomStatusMeta(room, deptSuspended)
}
</script>

<style scoped>
.room-grid {
  flex: 1;
  overflow-y: auto;
  padding: 0 1rem 1rem;
}
.room-grid__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0.75rem 0 0.5rem;
  position: sticky;
  top: 0;
  background: var(--nd-bg);
  z-index: 2;
}
.room-grid__hint {
  font-size: var(--nd-font-caption);
  color: var(--nd-text-secondary);
}
.room-grid__dept {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 1rem 0 0.5rem;
}
.room-grid__dept-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--nd-text);
}
.room-grid__dept-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.room-grid__tag-icon {
  margin-right: 2px;
}
.room-grid__matrix {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.75rem;
}
.room-card {
  padding: 1rem;
  cursor: default;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}
.room-card--danger {
  border-color: #ffcdd2;
}
.room-card--muted {
  opacity: 0.85;
}
.room-card__top {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}
.room-card__status {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.room-card__name {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--nd-text);
  line-height: 1.3;
}
.room-card__loc {
  margin: 0.25rem 0 0;
  font-size: var(--nd-font-caption);
  color: var(--nd-text-secondary);
}
.room-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--nd-border);
}
.room-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
}
.room-card__wait {
  text-align: right;
}
.room-card__wait-label {
  display: block;
  font-size: 0.75rem;
  color: var(--nd-text-secondary);
}
.room-card__wait-num {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--nd-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.room-card--danger .room-card__wait-num {
  color: var(--nd-danger);
}
.room-grid__empty {
  margin-top: 3rem;
}
</style>
