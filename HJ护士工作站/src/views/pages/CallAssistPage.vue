/**
 * 辅助呼叫页 — 对齐原型 page-call
 * 消费者：Desk.vue。呼叫/重呼/过号/完成复用叫号器 DoctorCall，不另建接口。
 */
<template>
  <div class="nd-page">
    <div class="page-title">
      <svg class="ln-icon"><use href="#i-speaker" /></svg>
      辅助呼叫
    </div>
    <div class="page-subtitle">叫号器故障时，护士替代呼叫、重呼、过号操作</div>

    <div class="col-layout">
      <div>
        <div class="content-card">
          <div class="card-title">当前叫号</div>
          <div class="call-panel">
            <div style="font-size: 14px; color: var(--text-muted)">
              {{ currentRoomLabel }}
            </div>
            <div class="call-number">{{ current?.showNo || '—' }}</div>
            <div class="call-info">
              <strong>{{ current?.patientName || '未选择患者' }}</strong>
              · {{ current?.patientCode || '—' }}
            </div>
            <div class="call-info">请从下方选择叫号对象后操作</div>
            <div class="btn-group" style="margin-top: var(--space-3)">
              <button class="btn btn-primary btn-lg" :disabled="busy" @click="callSelected">
                <svg class="ln-icon"><use href="#i-speaker" /></svg> 呼叫
              </button>
              <button class="btn btn-outline btn-lg" :disabled="busy" @click="recall">
                <svg class="ln-icon"><use href="#i-loop" /></svg> 重呼
              </button>
              <button class="btn btn-coral btn-lg" :disabled="busy" @click="passCurrent">
                <svg class="ln-icon"><use href="#i-skip" /></svg> 过号
              </button>
              <button class="btn btn-outline btn-lg" :disabled="busy" @click="finishCurrent">
                完成
              </button>
            </div>
          </div>
        </div>

        <div class="content-card">
          <div class="card-title">选择叫号对象</div>
          <div style="display: flex; gap: var(--space-3); margin-bottom: var(--space-4)">
            <select v-model="roomId" class="select" style="flex: 1">
              <option value="">选择诊室...</option>
              <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
            <select v-model="patientId" class="select" style="flex: 1">
              <option value="">选择患者...</option>
              <option v-for="p in patients" :key="p.id" :value="p.id">
                {{ p.showNo }} {{ p.patientName }}
              </option>
            </select>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="confirmSwitch">确认切换</button>
          </div>
        </div>
      </div>

      <div class="content-card">
        <div class="card-title">呼叫记录</div>
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>操作人</th>
              <th>操作类型</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(log, i) in logs" :key="i">
              <td>{{ formatTime(log.createTime || log.createdAt) }}</td>
              <td>{{ log.operatorName || log.operatorUserNo || '—' }}</td>
              <td><span class="tag tag-indigo">{{ log.actionType || log.action || '—' }}</span></td>
              <td>{{ log.detail || log.remark || log.message || '—' }}</td>
            </tr>
            <tr v-if="!logs.length">
              <td colspan="4"><div class="nd-empty">暂无记录</div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getActionLogs, getRoomQueue, editCallState, docCallMsg } from '../../api/nurseDesk'
import { ElMessage } from 'element-plus'

const props = defineProps({
  rooms: { type: Array, default: () => [] },
  active: { type: Boolean, default: false }
})

const roomId = ref('')
const patientId = ref('')
const patients = ref([])
const calling = ref(null)
const current = ref(null)
const logs = ref([])
const busy = ref(false)

const currentRoomLabel = computed(() => {
  const r = props.rooms.find((x) => x.id === roomId.value)
  return r ? r.name : '请选择诊室'
})

function mapPatient(row) {
  return {
    id: row.id ?? row.Id,
    showNo: row.showNo || row.ShowNo || '—',
    patientName: row.patientName || row.PatientName || '—',
    patientCode: row.patientCode || row.PatientCode || '—'
  }
}

function selectedRoom() {
  return props.rooms.find((x) => x.id === roomId.value) || null
}

async function loadPatients() {
  patients.value = []
  calling.value = null
  if (!roomId.value) return
  const room = selectedRoom()
  if (!room?.deptId) {
    ElMessage.warning('该诊室未关联科室，无法拉取队列')
    return
  }
  try {
    const [waitRes, callRes] = await Promise.all([
      getRoomQueue(room.deptId, room.id, 0),
      getRoomQueue(room.deptId, room.id, 1)
    ])
    patients.value = (waitRes.data || []).map(mapPatient)
    const callingList = (callRes.data || []).map(mapPatient)
    calling.value = callingList[0] || null
    current.value = calling.value
  } catch (_) {
    patients.value = []
    calling.value = null
    current.value = null
  }
}

async function loadLogs() {
  try {
    const res = await getActionLogs(1, 20)
    const data = res.data || {}
    logs.value = data.items || data.list || []
  } catch (_) {
    logs.value = []
  }
}

function confirmSwitch() {
  const p = patients.value.find((x) => String(x.id) === String(patientId.value))
  if (!p) {
    ElMessage.warning('请选择等候患者')
    return
  }
  current.value = p
  ElMessage.success('已选中 ' + p.patientName)
}

async function voice(patient, room) {
  await docCallMsg(`${patient.id}|${room?.name || ''}`, '')
}

async function callSelected() {
  const room = selectedRoom()
  if (!room) {
    ElMessage.warning('请选择诊室')
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    if (calling.value) {
      if (patientId.value && String(patientId.value) !== String(calling.value.id)) {
        ElMessage.warning('请先完成或过号当前患者，再呼叫下一位')
        return
      }
      await voice(calling.value, room)
      ElMessage.success('正在呼叫 ' + calling.value.patientName)
      return
    }
    const picked = patients.value.find((x) => String(x.id) === String(patientId.value)) || patients.value[0]
    if (!picked) {
      ElMessage.warning('当前诊室没有等候患者')
      return
    }
    await editCallState(picked.id, 1, room.id)
    await voice(picked, room)
    ElMessage.success('正在呼叫 ' + picked.patientName)
    await loadPatients()
    await loadLogs()
  } finally {
    busy.value = false
  }
}

async function recall() {
  const room = selectedRoom()
  const patient = calling.value
  if (!patient) {
    ElMessage.warning('当前没有正在呼叫的患者')
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    await voice(patient, room)
    ElMessage.success('正在重呼 ' + patient.patientName)
  } finally {
    busy.value = false
  }
}

async function passCurrent() {
  await changeCallingState(6, '已过号')
}

async function finishCurrent() {
  await changeCallingState(4, '已完成')
}

async function changeCallingState(state, okText) {
  const patient = calling.value
  if (!patient) {
    ElMessage.warning('当前没有正在呼叫的患者')
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    await editCallState(patient.id, state, roomId.value)
    ElMessage.success(okText)
    current.value = null
    patientId.value = ''
    await loadPatients()
    await loadLogs()
  } finally {
    busy.value = false
  }
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

watch(roomId, loadPatients)
watch(
  () => props.active,
  (v) => {
    if (v) loadLogs()
  }
)
onMounted(() => {
  if (props.active) loadLogs()
})
</script>
