/**
 * 工作台页 — 对齐原型 page-dashboard
 * 消费者：Desk.vue；依赖 GetOverview / GetAlerts / GetSession 诊室
 */
<template>
  <div class="nd-page">
    <div class="page-title page-title--row">
      <span>
        <svg class="ln-icon" style="margin-right: 6px"><use href="#i-grid" /></svg>
        工作台
      </span>
      <button class="btn btn-sm btn-outline" :disabled="refreshing" @click="$emit('refresh')">
        <svg class="ln-icon"><use href="#i-refresh" /></svg>
        {{ refreshing ? '刷新中...' : '刷新' }}
      </button>
    </div>
    <div class="page-subtitle">{{ zoneName || '诊区' }} · 诊区概览 — 数据实时刷新</div>

    <!-- Row 1: Core KPI（已接 GetOverview；挂号总/已签到/复诊/完成 等扩展字段待后端） -->
    <div class="card-grid col5">
      <div class="stat-card accent-indigo">
        <div class="card-head">
          <span class="card-label">挂号总人数</span>
          <svg class="card-icon ln-icon"><use href="#i-doc" /></svg>
        </div>
        <div class="card-value">{{ fmt(kpi.totalRegistered) }}</div>
        <div class="card-sub">
          <span class="nd-gap-hint">{{ kpi.totalRegistered == null ? '接口未返回·占位' : '今日队列合计' }}</span>
        </div>
      </div>
      <div class="stat-card accent-amber">
        <div class="card-head">
          <span class="card-label">已签到人数</span>
          <svg class="card-icon ln-icon"><use href="#i-check" /></svg>
        </div>
        <div class="card-value">{{ fmt(kpi.signedIn) }}</div>
        <div class="card-sub">
          签到率
          <strong style="color: var(--amber)">{{ kpi.signRate }}</strong>
        </div>
      </div>
      <div class="stat-card accent-coral">
        <div class="card-head">
          <span class="card-label">过号人数</span>
          <svg class="card-icon ln-icon"><use href="#i-warn" /></svg>
        </div>
        <div class="card-value">{{ fmt(overview.passedCount) }}</div>
        <div class="card-sub"><span class="tag tag-coral">待召回</span></div>
      </div>
      <div class="stat-card accent-magenta">
        <div class="card-head">
          <span class="card-label">复诊人数</span>
          <svg class="card-icon ln-icon"><use href="#i-loop" /></svg>
        </div>
        <div class="card-value">{{ fmt(kpi.revisit) }}</div>
        <div class="card-sub"><span class="tag tag-magenta">检查完返回</span></div>
      </div>
      <div class="stat-card accent-success">
        <div class="card-head">
          <span class="card-label">完成就诊人数</span>
          <svg class="card-icon ln-icon"><use href="#i-hex" /></svg>
        </div>
        <div class="card-value">{{ fmt(kpi.done) }}</div>
        <div class="card-sub">
          候诊
          <strong style="color: var(--success)">{{ fmt(overview.waitingCount) }}</strong>
          · 未签到 {{ fmt(overview.unarrivedCount) }}
        </div>
      </div>
    </div>

    <!-- Row 2: 诊室 / 设备概览 -->
    <div class="card-grid">
      <div class="stat-card accent-success">
        <div class="card-head">
          <span class="card-label">诊室概况</span>
          <svg class="card-icon ln-icon"><use href="#i-room" /></svg>
        </div>
        <div class="card-value">
          {{ fmt(overview.totalRoomCount) }}
          <span style="font-size: 14px; color: var(--text-muted)"> 间</span>
          <span style="font-size: 12px; color: var(--text-muted); font-weight: 400">
            （
            <span style="color: var(--success)">{{ fmt(overview.openRoomCount) }}开放</span>
            ）
          </span>
        </div>
        <div class="card-sub">
          当前候诊合计
          <strong style="color: var(--amber)">{{ fmt(overview.waitingCount) }}人</strong>
        </div>
      </div>
      <div class="stat-card accent-indigo">
        <div class="card-head">
          <span class="card-label">自助签到终端</span>
          <svg class="card-icon ln-icon"><use href="#i-device" /></svg>
        </div>
        <div class="card-value">
          —
          <span style="font-size: 14px; color: var(--text-muted)"> 台</span>
        </div>
        <div class="card-sub"><span class="nd-gap-hint">设备状态接口未接</span></div>
      </div>
      <div class="stat-card accent-amber">
        <div class="card-head">
          <span class="card-label">候诊大屏</span>
          <svg class="card-icon ln-icon"><use href="#i-monitor" /></svg>
        </div>
        <div class="card-value">
          —
          <span style="font-size: 14px; color: var(--text-muted)"> 台</span>
        </div>
        <div class="card-sub"><span class="nd-gap-hint">设备状态接口未接</span></div>
      </div>
      <div class="stat-card accent-magenta">
        <div class="card-head">
          <span class="card-label">门边屏</span>
          <svg class="card-icon ln-icon"><use href="#i-monitor" /></svg>
        </div>
        <div class="card-value">
          —
          <span style="font-size: 14px; color: var(--text-muted)"> 台</span>
        </div>
        <div class="card-sub"><span class="nd-gap-hint">设备状态接口未接</span></div>
      </div>
    </div>

    <!-- Charts -->
    <div class="col-layout" style="margin-bottom: var(--space-4)">
      <div class="content-card">
        <div class="card-title">时间段内签到人数</div>
        <div style="height: 200px; position: relative">
          <canvas ref="chartSignInRef" />
          <div v-if="!chartsReady" class="nd-empty" style="position: absolute; inset: 0">统计接口未接</div>
        </div>
      </div>
      <div class="content-card">
        <div class="card-title">候诊时间趋势</div>
        <div style="height: 200px; position: relative">
          <canvas ref="chartWaitRef" />
          <div v-if="!chartsReady" class="nd-empty" style="position: absolute; inset: 0">统计接口未接</div>
        </div>
      </div>
    </div>

    <!-- Notifications & Rooms as doctor proxy -->
    <div class="card-grid col2">
      <div class="stat-card accent-amber" :class="{ 'warning-glow': alertCount > 0 }">
        <div class="card-head">
          <span class="card-label">通知消息</span>
          <svg class="card-icon ln-icon"><use href="#i-bell" /></svg>
        </div>
        <div class="card-value" style="color: var(--coral)">
          {{ alertCount }}<span style="font-size: 14px"> 条</span>
        </div>
        <div class="card-sub">{{ alertSummary }}</div>
      </div>
      <div class="stat-card accent-indigo">
        <div class="card-head">
          <span class="card-label">诊室状态</span>
          <svg class="card-icon ln-icon"><use href="#i-plus" /></svg>
        </div>
        <div class="doctor-list" style="margin-top: 0">
          <div v-for="r in roomPreview" :key="r.id" class="doctor-item">
            <div class="doc-avatar" :class="r.open ? 'doc-online' : 'doc-offline'">
              {{ (r.name || '?').slice(0, 1) }}
            </div>
            <div class="doc-info">
              <div class="doc-name">{{ r.name }}</div>
              <div class="doc-room">{{ r.open ? '开放中' : '关闭' }} · 等候 {{ r.waitingCount ?? '—' }}</div>
            </div>
            <div class="doc-patients">{{ r.waitingCount ?? 0 }}人</div>
          </div>
          <div v-if="!roomPreview.length" class="nd-empty" style="padding: 12px">暂无诊室</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  overview: { type: Object, default: () => ({}) },
  alerts: { type: Array, default: () => [] },
  rooms: { type: Array, default: () => [] },
  zoneName: { type: String, default: '' },
  refreshing: { type: Boolean, default: false }
})
defineEmits(['refresh'])

const chartSignInRef = ref(null)
const chartWaitRef = ref(null)
const chartsReady = ref(false)
let chartInstances = []

const alertCount = computed(() => props.alerts?.length || props.overview?.alertCount || 0)

/** 从现有 overview 推导近似 KPI；缺失字段显示 — */
const kpi = computed(() => {
  const w = Number(props.overview.waitingCount) || 0
  const u = Number(props.overview.unarrivedCount) || 0
  const p = Number(props.overview.passedCount) || 0
  const signedIn = w + p
  const total = signedIn + u
  const signRate = total > 0 ? `${Math.round((signedIn / total) * 100)}%` : '—'
  return {
    totalRegistered: total || null,
    signedIn: signedIn || null,
    signRate,
    revisit: null,
    done: null
  }
})

const alertSummary = computed(() => {
  if (!props.alerts?.length) return '暂无待处理告警'
  const types = {}
  for (const a of props.alerts) {
    const t = a.alertType || 'OTHER'
    types[t] = (types[t] || 0) + 1
  }
  return Object.entries(types)
    .map(([k, v]) => `${k} ${v}`)
    .join(' · ')
})

const roomPreview = computed(() =>
  (props.rooms || []).slice(0, 4).map((r) => ({
    ...r,
    open: r.state === 1
  }))
)

function fmt(v) {
  return v == null || v === '' ? '—' : v
}

const chartText = { color: '#94A3B8', font: { size: 11 } }
const chartGrid = { color: 'rgba(255,255,255,0.06)' }

function destroyCharts() {
  chartInstances.forEach((c) => {
    try {
      c.destroy()
    } catch (_) {}
  })
  chartInstances = []
}

function initCharts() {
  destroyCharts()
  // 无分时段接口时绘空坐标系，保持布局 1:1
  const emptyHours = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00']
  const zeros = emptyHours.map(() => 0)

  if (chartSignInRef.value) {
    chartInstances.push(
      new Chart(chartSignInRef.value, {
        type: 'bar',
        data: {
          labels: emptyHours,
          datasets: [
            {
              label: '签到人数',
              data: zeros,
              backgroundColor: 'rgba(92,107,192,0.35)',
              borderRadius: 4,
              borderSkipped: false
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { grid: chartGrid, ticks: chartText },
            y: {
              grid: chartGrid,
              ticks: { ...chartText, callback: (v) => v + '人' },
              beginAtZero: true
            }
          }
        }
      })
    )
  }
  if (chartWaitRef.value) {
    chartInstances.push(
      new Chart(chartWaitRef.value, {
        type: 'line',
        data: {
          labels: emptyHours,
          datasets: [
            {
              label: '平均候诊(分)',
              data: zeros,
              borderColor: '#FFB347',
              backgroundColor: 'rgba(255,179,71,0.1)',
              fill: true,
              tension: 0.4,
              pointRadius: 3,
              pointBackgroundColor: '#FFB347',
              borderWidth: 2
            },
            {
              label: '最长候诊(分)',
              data: zeros,
              borderColor: '#FF6B6B',
              backgroundColor: 'rgba(255,107,107,0.05)',
              fill: true,
              tension: 0.4,
              pointRadius: 3,
              pointBackgroundColor: '#FF6B6B',
              borderWidth: 2,
              borderDash: [4, 2]
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              labels: {
                color: '#94A3B8',
                boxWidth: 12,
                boxHeight: 2,
                padding: 16,
                font: { size: 11 }
              }
            }
          },
          scales: {
            x: { grid: chartGrid, ticks: chartText },
            y: {
              grid: chartGrid,
              ticks: { ...chartText, callback: (v) => v + '分' },
              beginAtZero: true
            }
          }
        }
      })
    )
  }
  chartsReady.value = true
}

onMounted(async () => {
  await nextTick()
  initCharts()
})

watch(
  () => props.refreshing,
  (v) => {
    if (!v) nextTick(() => initCharts())
  }
)

onUnmounted(() => destroyCharts())
</script>
