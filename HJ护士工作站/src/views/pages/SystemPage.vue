/**
 * 系统管理 — 操作日志（GetActionLogs）
 * 消费者：Desk.vue；对齐原型 page-system
 */
<template>
  <div class="nd-page">
    <div class="page-title">
      <svg class="ln-icon"><use href="#i-gear" /></svg>
      系统管理
    </div>
    <div class="page-subtitle">操作日志 · 操作记录追溯</div>

    <div class="search-bar">
      <input
        v-model="keyword"
        class="input"
        placeholder="搜索操作人 / 操作类型..."
        style="max-width: 240px"
        @keydown.enter.prevent="load"
      />
      <select v-model="actionType" class="select">
        <option value="">全部操作类型</option>
        <option value="SIGN_IN">签到</option>
        <option value="REVIVE">召回</option>
        <option value="CUT_IN">插队</option>
        <option value="SWAP">队列转移</option>
        <option value="FREEZE">冻结</option>
        <option value="SWITCH_ZONE">切换诊区</option>
      </select>
      <button class="btn btn-sm btn-outline" :disabled="loading" @click="load">查询</button>
      <span style="font-size: 12px; color: var(--text-muted)">共 {{ total }} 条记录</span>
    </div>

    <div style="overflow-x: auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>操作类型</th>
            <th>患者</th>
            <th>详情</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(log, i) in displayItems" :key="log.id || i">
            <td>{{ formatTime(log.createTime || log.createdAt || log.operateTime) }}</td>
            <td>{{ log.operatorName || log.operatorUserNo || '—' }}</td>
            <td>
              <span class="tag" :class="typeTag(log.actionType || log.action)">
                {{ log.actionType || log.action || '—' }}
              </span>
            </td>
            <td>{{ patientCell(log) }}</td>
            <td>{{ log.detail || log.remark || log.message || '—' }}</td>
            <td>{{ log.clientIp || log.ip || '—' }}</td>
          </tr>
          <tr v-if="!displayItems.length">
            <td colspan="6"><div class="nd-empty">{{ loading ? '加载中...' : '暂无操作记录' }}</div></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getActionLogs } from '../../api/nurseDesk'

const props = defineProps({
  active: { type: Boolean, default: false }
})

const keyword = ref('')
const actionType = ref('')
const items = ref([])
const total = ref(0)
const loading = ref(false)

const displayItems = computed(() => {
  let list = items.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((l) => {
      const blob = `${l.operatorName || ''}${l.operatorUserNo || ''}${l.actionType || ''}${l.action || ''}${l.detail || ''}${l.remark || ''}`.toLowerCase()
      return blob.includes(kw)
    })
  }
  if (actionType.value) {
    list = list.filter((l) => String(l.actionType || l.action || '').includes(actionType.value))
  }
  return list
})

async function load() {
  loading.value = true
  try {
    const res = await getActionLogs(1, 50)
    const data = res.data || {}
    items.value = data.items || data.list || []
    total.value = data.total ?? items.value.length
  } catch (_) {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function patientCell(log) {
  if (log.patientName || log.showNo) {
    return [log.showNo, log.patientName].filter(Boolean).join(' ')
  }
  return log.callListId || '—'
}

function typeTag(type) {
  const t = String(type || '')
  if (/REVIVE|召回/.test(t)) return 'tag-amber'
  if (/CUT|插|GREEN|绿色/.test(t)) return 'tag-success'
  if (/SWAP|转移|转诊/.test(t)) return 'tag-magenta'
  if (/SIGN|签到/.test(t)) return 'tag-indigo'
  if (/PASS|过号/.test(t)) return 'tag-coral'
  return 'tag-indigo'
}

watch(
  () => props.active,
  (v) => {
    if (v) load()
  }
)

onMounted(() => {
  if (props.active) load()
})
</script>
