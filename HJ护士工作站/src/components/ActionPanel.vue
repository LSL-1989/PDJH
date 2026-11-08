<template>
  <div class="action-panel">
    <template v-if="patient">
      <div class="action-panel__hero nd-card">
        <div class="action-panel__hero-no">{{ patient.showNo || '—' }}</div>
        <div class="action-panel__hero-info">
          <h3 class="action-panel__name">{{ patient.patientName || '未知患者' }}</h3>
          <p class="action-panel__code">{{ patient.patientCode }}</p>
        </div>
        <span
          class="action-panel__state"
          :style="patientStateStyle(patient)"
        >
          <el-icon :size="14"><component :is="patientStateIcon(patient)" /></el-icon>
          {{ stateLabel(patient.state) }}
        </span>
      </div>

      <el-descriptions :column="2" border size="default" class="action-panel__desc">
        <el-descriptions-item label="分诊状态">{{ patient.triageState || '—' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ patient.deptID || '—' }}</el-descriptions-item>
        <el-descriptions-item label="诊室">{{ patient.officeRoomId || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="预约">
          <span v-if="patient.isAppointment === 1">
            {{ patient.appointmentStartTime }}–{{ patient.appointmentEndTime }}
          </span>
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>

      <ReasonSelect v-model:reason-code="reasonCode" v-model:remark="remark" class="action-panel__reason" />

      <section class="action-panel__section">
        <h4 class="action-panel__section-title">签到与绑定</h4>
        <div class="action-panel__btns">
          <el-button type="primary" size="large" :loading="loading" @click="doAction('signIn')">签到兜底</el-button>
          <el-button size="large" :loading="loading" @click="doAction('signInOverride')">Override 签到</el-button>
          <el-button size="large" :loading="loading" @click="showBindRoom = true">补绑诊室</el-button>
        </div>
      </section>

      <section class="action-panel__section">
        <h4 class="action-panel__section-title">队列调整</h4>
        <div class="action-panel__btns">
          <el-button type="warning" size="large" :loading="loading" @click="doAction('revive')">过号复活</el-button>
          <el-button type="warning" size="large" :loading="loading" @click="doAction('cutIn')">插队</el-button>
          <el-button size="large" :loading="loading" @click="openSwap(false)">转诊</el-button>
          <el-button size="large" :loading="loading" @click="openSwap(true)">会诊转诊</el-button>
        </div>
      </section>

      <section class="action-panel__section">
        <h4 class="action-panel__section-title">冻结与退号</h4>
        <div class="action-panel__btns">
          <el-button size="large" :loading="loading" @click="doAction('freeze')">冻结</el-button>
          <el-button size="large" :loading="loading" @click="doAction('unfreeze')">解冻</el-button>
          <el-button type="danger" size="large" :loading="loading" @click="confirmHisRefund">HIS 已退</el-button>
        </div>
      </section>
    </template>

    <template v-else-if="deptAlert">
      <div class="action-panel__hero nd-card action-panel__hero--warn">
        <el-icon :size="28" color="#FB8C00"><Warning /></el-icon>
        <div>
          <h3 class="action-panel__name">科室停诊处置</h3>
          <p class="action-panel__code">{{ deptAlert.message }}</p>
        </div>
      </div>
      <ReasonSelect v-model:reason-code="reasonCode" v-model:remark="remark" />
      <div class="action-panel__btns action-panel__btns--single">
        <el-button type="danger" size="large" :loading="loading" @click="doDeptAction('suspend')">确认停诊</el-button>
        <el-button type="success" size="large" :loading="loading" @click="doDeptAction('resume')">恢复出诊</el-button>
      </div>
    </template>

    <template v-else>
      <div class="action-panel__empty nd-card">
        <el-icon :size="36" color="#90A4AE"><Search /></el-icon>
        <p class="action-panel__empty-title">请先检索并选择患者</p>
        <p class="action-panel__empty-desc">选中患者后可进行签到兜底、队列调整、冻结退号等操作；全部操作自动留痕。</p>
        <div class="action-panel__btns">
          <el-button type="primary" size="large" @click="emit('navigate', 'search')">患者检索</el-button>
          <el-button size="large" @click="emit('navigate', 'alerts')">
            告警{{ alertCount > 0 ? ` (${alertCount})` : '' }}
          </el-button>
        </div>
      </div>
    </template>

    <el-dialog v-model="showBindRoom" title="补绑诊室" width="400px" destroy-on-close>
      <el-select v-model="bindRoomId" placeholder="选择诊室" class="w-full">
        <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="showBindRoom = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitBindRoom">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSwap" :title="swapIsConsult ? '会诊转诊' : '转诊'" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="目标科室">
          <el-select v-model="swapDeptId" placeholder="选择科室" class="w-full">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSwap = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitSwap">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning, CircleCheck, CircleClose, Clock, Search } from '@element-plus/icons-vue'
import ReasonSelect from './ReasonSelect.vue'
import { PATIENT_STATE_LABELS } from '../utils/queueStatus'
import {
  manualSignIn, bindOfficeRoom, revive, manualCutIn, forceSwap,
  setFrozen, markHisRefunded, suspendDept, resumeDept
} from '../api/nurseDesk'

const props = defineProps({
  patient: { type: Object, default: null },
  deptAlert: { type: Object, default: null },
  rooms: { type: Array, default: () => [] },
  departments: { type: Array, default: () => [] },
  alertCount: { type: Number, default: 0 }
})
const emit = defineEmits(['done', 'navigate'])

const reasonCode = ref('')
const remark = ref('')
const loading = ref(false)
const showBindRoom = ref(false)
const bindRoomId = ref('')
const showSwap = ref(false)
const swapDeptId = ref('')
const swapIsConsult = ref(false)

watch(() => props.patient, () => {
  reasonCode.value = ''
  remark.value = ''
})

function basePayload() {
  return { reasonCode: reasonCode.value, remark: remark.value }
}

function validateReason() {
  if (!reasonCode.value) {
    ElMessage.warning('请选择操作原因')
    return false
  }
  if (reasonCode.value === 'OTHER' && !remark.value.trim()) {
    ElMessage.warning('选择「其他」时必须填写备注')
    return false
  }
  return true
}

function stateLabel(s) {
  return PATIENT_STATE_LABELS[s] || String(s ?? '—')
}

function patientStateStyle(patient) {
  const s = patient.state
  if (s === 3) return { color: '#E53935', background: '#FFEBEE' }
  if (s === 1) return { color: '#1E88E5', background: '#E3F2FD' }
  if (s === 2) return { color: '#43A047', background: '#E8F5E9' }
  return { color: '#FB8C00', background: '#FFF3E0' }
}

function patientStateIcon(patient) {
  const s = patient.state
  if (s === 3) return CircleClose
  if (s === 2) return CircleCheck
  return Clock
}

async function doAction(type) {
  if (!props.patient?.id || !validateReason()) return
  loading.value = true
  try {
    const id = props.patient.id
    const p = { callListId: id, ...basePayload() }
    let res
    if (type === 'signIn') {
      res = await manualSignIn({ ...p, skipSecondScan: true })
    } else if (type === 'signInOverride') {
      res = await manualSignIn({ ...p, skipApptCheck: true, skipSecondScan: true })
    } else if (type === 'revive') {
      res = await revive(p)
    } else if (type === 'cutIn') {
      res = await manualCutIn({ ...p, targetXh: 0 })
    } else if (type === 'freeze') {
      res = await setFrozen({ ...p, frozen: true })
    } else if (type === 'unfreeze') {
      res = await setFrozen({ ...p, frozen: false })
    }
    if (res?.success) {
      ElMessage.success(res.msg || '操作成功')
      emit('done')
    } else {
      ElMessage.error(res?.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}

function openSwap(consult) {
  swapIsConsult.value = consult
  swapDeptId.value = ''
  showSwap.value = true
}

async function submitBindRoom() {
  if (!bindRoomId.value || !validateReason()) return
  loading.value = true
  try {
    const res = await bindOfficeRoom({
      callListId: props.patient.id,
      officeRoomId: bindRoomId.value,
      ...basePayload(),
      reasonCode: reasonCode.value || 'ROOM_FIX'
    })
    if (res?.success) {
      ElMessage.success('绑定成功')
      showBindRoom.value = false
      emit('done')
    }
  } finally {
    loading.value = false
  }
}

async function submitSwap() {
  if (!swapDeptId.value || !validateReason()) return
  loading.value = true
  try {
    const payload = {
      callListId: props.patient.id,
      newDeptId: swapDeptId.value,
      ...basePayload()
    }
    if (swapIsConsult.value) {
      payload.transferType = 'CONSULT'
      if (!payload.reasonCode) payload.reasonCode = 'CONSULT_TRANSFER'
    }
    const res = await forceSwap(payload)
    if (res?.success) {
      ElMessage.success('转诊成功')
      showSwap.value = false
      emit('done')
    }
  } finally {
    loading.value = false
  }
}

async function confirmHisRefund() {
  if (!validateReason()) return
  try {
    await ElMessageBox.confirm('确认该患者已在收费窗口退号？', 'HIS 已退', { type: 'warning' })
    loading.value = true
    const res = await markHisRefunded({
      callListId: props.patient.id,
      reasonCode: reasonCode.value || 'HIS_REFUNDED',
      remark: remark.value
    })
    if (res?.success) {
      ElMessage.success('已同步作废队列')
      emit('done')
    }
  } catch (_) { /* cancel */ } finally {
    loading.value = false
  }
}

async function doDeptAction(type) {
  if (!props.deptAlert?.deptId || !validateReason()) return
  loading.value = true
  try {
    const p = { deptId: props.deptAlert.deptId, ...basePayload() }
    const res = type === 'suspend' ? await suspendDept(p) : await resumeDept(p)
    if (res?.success) {
      ElMessage.success(res.msg)
      emit('done')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.action-panel {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 1rem 1.25rem;
  min-height: 0;
  background: var(--nd-bg);
}
.action-panel__hero {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 0.75rem;
}
.action-panel__hero--warn {
  border-left: 4px solid #fb8c00;
}
.action-panel__hero-no {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--nd-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  min-width: 4ch;
}
.action-panel__hero-info {
  flex: 1;
  min-width: 0;
}
.action-panel__name {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--nd-text);
}
.action-panel__code {
  margin: 0.25rem 0 0;
  font-size: var(--nd-font-caption);
  color: var(--nd-text-secondary);
}
.action-panel__state {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  flex-shrink: 0;
}
.action-panel__desc {
  margin-bottom: 0.75rem;
}
.action-panel__reason {
  margin-bottom: 0.5rem;
}
.action-panel__section {
  margin-top: 0.75rem;
}
.action-panel__section-title {
  margin: 0 0 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--nd-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.action-panel__btns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.action-panel__btns .el-button {
  min-height: 44px;
  min-width: 120px;
}
.action-panel__btns--single {
  margin-top: 1rem;
}
.action-panel__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem 1.5rem;
  gap: 0.5rem;
}
.action-panel__empty-title {
  margin: 0.75rem 0 0;
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--nd-text);
}
.action-panel__empty-desc {
  margin: 0 0 1rem;
  font-size: 0.875rem;
  color: var(--nd-text-secondary);
  line-height: 1.5;
  max-width: 28rem;
}
.w-full { width: 100%; }
</style>
