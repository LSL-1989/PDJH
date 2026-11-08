<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import StandbyView from '../components/StandbyView.vue'
import TopBar from '../components/TopBar.vue'
import BottomBar from '../components/BottomBar.vue'
import DoctorSingle from '../components/DoctorSingle.vue'
import DoctorDual from '../components/DoctorDual.vue'
import DoctorPlaceholder from '../components/DoctorPlaceholder.vue'
import CurrentPatient from '../components/CurrentPatient.vue'
import WaitingList from '../components/WaitingList.vue'
import BulletinBar from '../components/BulletinBar.vue'
import { useQueuePoll } from '../composables/useQueuePoll'
import { useHallWebSocket } from '../composables/useHallWebSocket'
import { resolveDeviceCodeFromRoute } from '../api/clinicScreen'
import type { ClinicDisplayData, StandbyReason } from '../types/clinic'

const route = useRoute()
const envHospitalName = (import.meta.env.VITE_HOSPITAL_NAME as string) || '某某医院'
const envHospitalLogo = (import.meta.env.VITE_HOSPITAL_LOGO as string) || ''

const displayData = ref<ClinicDisplayData | null>(null)
const standbyReason = ref<StandbyReason | null>(null)
const standbyMessage = ref('')
/** 上次成功拉取的院名/Logo；失败(含断网)不回退清空，避免显示环境默认名 */
const cachedHospitalName = ref('')
const cachedHospitalLogo = ref('')

const deviceCode = computed(() => resolveDeviceCodeFromRoute(route.query.deviceCode))

const isStandby = computed(() => standbyReason.value !== null)

const hospitalName = computed(
  () => cachedHospitalName.value.trim() || envHospitalName
)

const hospitalLogo = computed(
  () => cachedHospitalLogo.value.trim() || envHospitalLogo
)

/** 仅在成功响应里更新院牌子；空字符串不覆盖已有缓存 */
const rememberHospitalBranding = (data: ClinicDisplayData) => {
  const name = data.hospitalName?.trim()
  if (name) cachedHospitalName.value = name
  const logo = data.hospitalLogo?.trim()
  if (logo) cachedHospitalLogo.value = logo
}

const onData = (data: ClinicDisplayData) => {
  standbyReason.value = null
  standbyMessage.value = ''
  displayData.value = data
  rememberHospitalBranding(data)
}

const onStandby = (reason: StandbyReason, message: string) => {
  standbyReason.value = reason
  standbyMessage.value = message
  displayData.value = null
  // 不改动 cachedHospitalName / cachedHospitalLogo
}

const { refresh } = useQueuePoll(onData, onStandby)

const matchHallMessage = (msg: { type?: string; code?: string }) => {
  const data = displayData.value
  if (!data) return false
  const code = msg.code
  if (!code) return true
  return code === data.room.deptId || code === data.room.id || code === data.screen.callCode
}

useHallWebSocket(matchHallMessage, () => {
  void refresh()
})

const doctors = computed(() => displayData.value?.doctors ?? [])
</script>

<template>
  <StandbyView
    v-if="isStandby && standbyReason"
    :reason="standbyReason"
    :message="standbyMessage"
    :device-code="deviceCode || undefined"
    :hospital-name="hospitalName"
    :hospital-logo="hospitalLogo || undefined"
  />
  <div v-else-if="displayData" class="clinic-screen">
    <TopBar
      :hospital-name="hospitalName"
      :room-name="displayData.room.name"
      :hospital-logo="hospitalLogo || undefined"
    />
    <main class="clinic-main">
      <DoctorSingle v-if="doctors.length === 1" :doctor="doctors[0]" />
      <DoctorDual v-else-if="doctors.length === 2" :doctors="doctors" />
      <DoctorPlaceholder v-else />
      <CurrentPatient
        :patients="displayData.currentPatients"
        :doctors="doctors"
      />
      <WaitingList
        v-if="displayData.screen.showWaitList !== false"
        :patients="displayData.waitingPatients"
        :doctors="doctors"
      />
      <BulletinBar :bulletin="displayData.screen.bulletin" />
    </main>
    <BottomBar :device-code="deviceCode" />
  </div>
  <div v-else class="clinic-screen loading-screen">
    <TopBar
      :hospital-name="hospitalName"
      room-name="加载中…"
      :hospital-logo="hospitalLogo || undefined"
    />
    <div class="loading-body">
      <div class="loading-spinner" aria-hidden="true" />
      <p class="loading-text">正在加载诊室信息</p>
    </div>
    <BottomBar :device-code="deviceCode" />
  </div>
</template>

<style scoped>
.clinic-screen {
  width: 100%;
  height: 100%;
  background: var(--color-background);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.clinic-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.clinic-main :deep(.clinic-panel) {
  margin: 0;
  border-radius: 0;
  position: relative;
}

/* 坐诊医生 → 正在就诊、正在就诊 → 等待就诊、等待就诊 → 公告：连接处向下阴影 */
.clinic-main :deep(.doctor-single.clinic-panel),
.clinic-main :deep(.doctor-dual.clinic-panel),
.clinic-main :deep(.doctor-placeholder.clinic-panel) {
  z-index: 4;
  box-shadow: var(--shadow-panel);
}

.clinic-main :deep(.current-patient.clinic-panel) {
  z-index: 3;
  box-shadow: var(--shadow-glow);
}

.clinic-main :deep(.waiting-list.clinic-panel) {
  z-index: 2;
  box-shadow: var(--shadow-panel);
}

.loading-screen {
  width: 100%;
  height: 100%;
  background: var(--color-background);
  display: flex;
  flex-direction: column;
}

.loading-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--color-primary-dim);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  box-shadow: 0 0 16px var(--color-primary-glow);
}

.loading-text {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
