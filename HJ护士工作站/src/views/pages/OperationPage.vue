/**
 * 操作区 — 签到扫码栏 + 患者列表 + 右侧快捷操作
 * 消费者：Desk.vue
 * 已接：SearchPatient / ManualSignIn / Revive / ManualCutIn / ForceSwap / GetAlerts
 * 缺口：全量患者列表、过号/指定诊室/同步挂号/代呼叫等专用接口
 */
<template>
  <div class="nd-page operation-page">
    <div class="page-title">
      <svg class="ln-icon"><use href="#i-monitor" /></svg>
      操作区
    </div>
    <div class="page-subtitle">患者签到 · 队列管理 · 快捷调度</div>

    <div class="operation-row">
      <div class="operation-main">
        <!-- Sign-in scanner bar -->
        <div class="signin-bar">
          <div class="signin-input-wrap">
            <svg class="ln-icon" style="font-size: 24px; flex-shrink: 0"><use href="#i-device" /></svg>
            <input
              ref="scanInputRef"
              v-model="scanCode"
              class="input signin-input"
              placeholder="扫码枪扫描条码/二维码，或手动输入就诊号后按回车..."
              @keydown.enter.prevent="doSignIn"
            />
            <button class="btn btn-primary btn-sm" :disabled="signing" @click="doSignIn">
              {{ signing ? '签到中...' : '确认签到' }}
            </button>
          </div>
          <span class="signin-hint">支持扫码枪自动识别 · 键盘输入后点击确认或按 Enter 签到</span>
        </div>

        <div class="content-card" style="flex: 1; display: flex; flex-direction: column; min-height: 0">
          <div class="card-title">患者列表</div>
          <div class="search-bar">
            <input
              v-model="keyword"
              class="input"
              placeholder="搜索姓名 / 就诊号..."
              @keydown.enter.prevent="searchList"
            />
            <select v-model="filterStatus" class="select">
              <option value="">全部状态</option>
              <option value="waiting">候诊中</option>
              <option value="passed">已过号</option>
              <option value="done">已就诊</option>
            </select>
            <select v-model="filterRoom" class="select">
              <option value="">全部诊室</option>
              <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
            <button class="btn btn-sm btn-outline" :disabled="loading" @click="searchList">
              <svg class="ln-icon"><use href="#i-loop" /></svg>
              刷新列表
            </button>
            <span style="font-size: 12px; color: var(--text-muted); margin-left: auto">
              共 {{ filteredRows.length }} 人 | 候诊 {{ waitingCount }} 人
            </span>
          </div>

          <div style="overflow-x: auto; flex: 1">
            <table class="data-table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>姓名</th>
                  <th>就诊号</th>
                  <th>状态</th>
                  <th>挂号科室</th>
                  <th>诊室</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in filteredRows"
                  :key="row.id"
                  :class="rowClass(row)"
                  :style="{ cursor: 'pointer', outline: selected?.id === row.id ? '1px solid var(--amber)' : 'none' }"
                  @click="selectRow(row)"
                >
                  <td :style="{ color: 'var(--amber)', fontWeight: 700 }">{{ row.showNo || '—' }}</td>
                  <td>{{ row.patientName || '—' }}</td>
                  <td>{{ row.patientCode || '—' }}</td>
                  <td>
                    <span class="status-dot" :class="statusDot(row)" />
                    {{ stateLabel(row.state) }}
                  </td>
                  <td>{{ row.deptID || '—' }}</td>
                  <td>{{ roomName(row.officeRoomId) }}</td>
                  <td @click.stop>
                    <div class="btn-group">
                      <button
                        v-if="row.state === 3"
                        class="btn btn-sm btn-outline"
                        style="color: var(--amber)"
                        @click="actRevive(row)"
                      >
                        召回
                      </button>
                      <template v-else-if="row.state === 0 || row.state === 1">
                        <button class="btn btn-sm btn-outline" style="color: var(--amber)" @click="actCutIn(row)">
                          前移
                        </button>
                        <button class="btn btn-sm btn-outline" @click="openSwap(row)">调号</button>
                      </template>
                      <span v-else style="color: var(--text-muted)">—</span>
                    </div>
                  </td>
                </tr>
                <tr v-if="!filteredRows.length">
                  <td colspan="7">
                    <div class="nd-empty">
                      {{ keyword ? '未找到患者，请调整关键字' : '请输入关键字检索，或从告警列表带入' }}
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Right Quick Ops -->
      <div class="ops-sidebar" :class="{ collapsed: opsCollapsed }">
        <div class="ops-sidebar-header">
          <span class="ops-sidebar-title">快捷操作</span>
          <button class="ops-sidebar-toggle" title="折叠/展开" @click="opsCollapsed = !opsCollapsed">
            {{ opsCollapsed ? '◀' : '▶' }}
          </button>
        </div>
        <div class="ops-sidebar-content">
          <div class="ops-section-label">调度组</div>
          <div class="ops-btn-group">
            <button class="btn btn-sm ops-btn ops-btn-green" @click="needPatient('插队/绿色通道', actCutIn)">
              <svg class="ln-icon"><use href="#i-flash" /></svg> 绿色通道
            </button>
            <button class="btn btn-sm ops-btn" @click="needPatient('前移', actCutIn)">
              <svg class="ln-icon"><use href="#i-up" /></svg> 前移
            </button>
            <button class="btn btn-sm ops-btn" disabled title="后移接口未接">
              <svg class="ln-icon"><use href="#i-down" /></svg> 后移
            </button>
            <button class="btn btn-sm ops-btn ops-btn-coral" disabled title="护士过号接口未接">
              <svg class="ln-icon"><use href="#i-skip" /></svg> 过号
            </button>
            <button class="btn btn-sm ops-btn ops-btn-amber" @click="needPatient('召回', actRevive)">
              <svg class="ln-icon"><use href="#i-loop" /></svg> 召回
            </button>
            <button class="btn btn-sm ops-btn" @click="needPatient('队列转移', openSwap)">
              <svg class="ln-icon"><use href="#i-swap" /></svg> 队列转移
            </button>
          </div>
          <div class="ops-section-label">辅助组</div>
          <div class="ops-btn-group">
            <button class="btn btn-sm ops-btn" disabled title="代呼叫接口未接">
              <svg class="ln-icon"><use href="#i-speaker" /></svg> 代呼叫
            </button>
            <button class="btn btn-sm ops-btn" disabled title="代重呼接口未接">
              <svg class="ln-icon"><use href="#i-loop" /></svg> 代重呼
            </button>
            <button class="btn btn-sm ops-btn ops-btn-coral" disabled title="代过号接口未接">
              <svg class="ln-icon"><use href="#i-skip" /></svg> 代过号
            </button>
          </div>
          <div v-if="selected" style="margin-top: 8px; font-size: 11px; color: var(--text-muted)">
            已选：{{ selected.patientName }} {{ selected.showNo }}
          </div>
        </div>
      </div>
    </div>

    <!-- Sign-in result modal -->
    <div v-if="signinModal" class="nd-modal-overlay" @click.self="signinModal = null">
      <div class="nd-modal modal-signin">
        <div class="modal-title">
          <svg class="ln-icon"><use href="#i-check" /></svg> 签到成功
        </div>
        <div class="modal-body signin-result">
          <div class="signin-patient-name">{{ signinModal.patientName }}</div>
          <div class="signin-patient-meta">
            <span class="signin-meta-item">{{ signinModal.patientCode }}</span>
            <span class="signin-meta-item">{{ signinModal.deptID || '—' }}</span>
            <span class="signin-meta-item">{{ roomName(signinModal.officeRoomId) }}</span>
          </div>
          <div class="signin-status-row">
            <span class="signin-status-badge">{{ stateLabel(signinModal.state) }}</span>
            <span class="signin-queue-num">排队序号：{{ signinModal.showNo || '—' }}</span>
          </div>
        </div>
        <div class="modal-actions signin-actions">
          <button class="btn btn-outline" @click="signinModal = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- Swap dialog -->
    <div v-if="swapTarget" class="nd-modal-overlay" @click.self="swapTarget = null">
      <div class="nd-modal">
        <div class="modal-title">队列转移 / 转诊</div>
        <div class="modal-body">
          <p style="margin-bottom: 12px">
            患者 <strong>{{ swapTarget.patientName }}</strong> → 选择目标科室
          </p>
          <select v-model="swapDeptId" class="select" style="width: 100%">
            <option value="">请选择科室</option>
            <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="swapTarget = null">取消</button>
          <button class="btn btn-primary" :disabled="acting" @click="submitSwap">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  searchPatient,
  manualSignIn,
  revive,
  manualCutIn,
  forceSwap
} from '../../api/nurseDesk'
import { PATIENT_STATE_LABELS } from '../../utils/queueStatus'

const props = defineProps({
  rooms: { type: Array, default: () => [] },
  departments: { type: Array, default: () => [] },
  alerts: { type: Array, default: () => [] },
  overview: { type: Object, default: () => ({}) },
  active: { type: Boolean, default: false }
})
const emit = defineEmits(['done'])

const scanInputRef = ref(null)
const scanCode = ref('')
const keyword = ref('')
const filterStatus = ref('')
const filterRoom = ref('')
const rows = ref([])
const selected = ref(null)
const loading = ref(false)
const signing = ref(false)
const acting = ref(false)
const opsCollapsed = ref(false)
const signinModal = ref(null)
const swapTarget = ref(null)
const swapDeptId = ref('')

const waitingCount = computed(() => props.overview.waitingCount ?? '—')

const filteredRows = computed(() => {
  let list = rows.value
  if (filterStatus.value === 'waiting') list = list.filter((r) => r.state === 0 || r.state === 1)
  if (filterStatus.value === 'passed') list = list.filter((r) => r.state === 3)
  if (filterStatus.value === 'done') list = list.filter((r) => r.state === 2)
  if (filterRoom.value) list = list.filter((r) => r.officeRoomId === filterRoom.value)
  return list
})

const roomMap = computed(() => {
  const m = new Map()
  for (const r of props.rooms) m.set(r.id, r.name)
  return m
})

function roomName(id) {
  if (!id) return '未绑定'
  return roomMap.value.get(id) || id
}

function stateLabel(state) {
  return PATIENT_STATE_LABELS[state] ?? '—'
}

function rowClass(row) {
  if (row.state === 3) return 'row-passed'
  if (row.state === 2) return 'row-done'
  if (row.state === 0 || row.state === 1) return 'row-arrived'
  return 'row-waiting'
}

function statusDot(row) {
  if (row.state === 3) return 'status-passed'
  if (row.state === 2) return 'status-done'
  return 'status-arrived'
}

function selectRow(row) {
  selected.value = row
}

function seedFromAlerts() {
  const fromAlerts = (props.alerts || [])
    .filter((a) => a.callListId || a.id)
    .map((a) => ({
      id: a.callListId || a.id,
      callListId: a.callListId || a.id,
      showNo: a.showNo,
      patientName: a.patientName,
      patientCode: a.patientCode,
      triageState: a.triageState,
      state: a.state,
      deptID: a.deptID,
      officeRoomId: a.officeRoomId,
      message: a.message
    }))
  if (!keyword.value && fromAlerts.length) {
    rows.value = fromAlerts
  }
}

async function searchList() {
  if (!keyword.value.trim()) {
    seedFromAlerts()
    return
  }
  loading.value = true
  try {
    const res = await searchPatient(keyword.value.trim())
    rows.value = res.data || []
  } catch (_) {
    ElMessage.error('检索失败')
  } finally {
    loading.value = false
  }
}

async function doSignIn() {
  const code = scanCode.value.trim()
  if (!code) return
  signing.value = true
  try {
    const res = await searchPatient(code)
    const list = res.data || []
    if (!list.length) {
      ElMessage.warning('未找到患者')
      return
    }
    const p = list[0]
    await manualSignIn({
      callListId: p.id,
      reasonCode: 'SCAN_DESK',
      remark: '操作区扫码签到'
    })
    signinModal.value = p
    scanCode.value = ''
    emit('done')
    keyword.value = code
    await searchList()
  } catch (e) {
    ElMessage.error(e?.message || '签到失败')
  } finally {
    signing.value = false
    scanInputRef.value?.focus?.()
  }
}

function needPatient(name, fn) {
  if (!selected.value) {
    ElMessage.warning(`请先在列表中选择患者，再执行「${name}」`)
    return
  }
  fn(selected.value)
}

async function actRevive(row) {
  acting.value = true
  try {
    await revive({ callListId: row.id || row.callListId, reasonCode: 'NURSE_REVIVE', remark: '操作区召回' })
    ElMessage.success('已召回')
    emit('done')
    await searchList()
  } catch (e) {
    ElMessage.error(e?.message || '召回失败')
  } finally {
    acting.value = false
  }
}

async function actCutIn(row) {
  acting.value = true
  try {
    await manualCutIn({
      callListId: row.id || row.callListId,
      reasonCode: 'NURSE_CUTIN',
      remark: '操作区前移/绿色通道'
    })
    ElMessage.success('已调整排队')
    emit('done')
    await searchList()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function openSwap(row) {
  swapTarget.value = row
  swapDeptId.value = ''
}

async function submitSwap() {
  if (!swapDeptId.value) {
    ElMessage.warning('请选择目标科室')
    return
  }
  acting.value = true
  try {
    await forceSwap({
      callListId: swapTarget.value.id || swapTarget.value.callListId,
      targetDeptId: swapDeptId.value,
      reasonCode: 'NURSE_SWAP',
      remark: '操作区队列转移'
    })
    ElMessage.success('已转移')
    swapTarget.value = null
    emit('done')
    await searchList()
  } catch (e) {
    ElMessage.error(e?.message || '转移失败')
  } finally {
    acting.value = false
  }
}

watch(
  () => props.active,
  (v) => {
    if (v) {
      seedFromAlerts()
      setTimeout(() => scanInputRef.value?.focus?.(), 200)
    }
  }
)

watch(
  () => props.alerts,
  () => {
    if (!keyword.value) seedFromAlerts()
  },
  { deep: true }
)

onMounted(() => seedFromAlerts())
</script>

<style scoped>
.operation-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
}
.operation-row {
  display: flex;
  gap: var(--space-4);
  flex: 1;
  min-height: 0;
}
.operation-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
</style>
