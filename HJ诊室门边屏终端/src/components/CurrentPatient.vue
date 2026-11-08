<script setup lang="ts">
import { computed } from 'vue'
import type { ClinicDoctorInfo, ClinicPatientRow } from '../types/clinic'
import { formatPatientLine } from '../utils/formatPatientLine'
import PatientMarquee from './PatientMarquee.vue'

const props = defineProps<{
  patients: ClinicPatientRow[]
  doctors: ClinicDoctorInfo[]
}>()

/** 单医生取首位；双医生按医生顺序取各 1 位，展示时仅取第一个有人的记录 */
const displayPatient = computed((): ClinicPatientRow | null => {
  if (props.patients.length === 0) return null
  if (props.doctors.length <= 1) {
    return props.patients[0] ?? null
  }
  const byDoctor = new Map<string, ClinicPatientRow>()
  for (const p of props.patients) {
    if (p.doctorId && !byDoctor.has(p.doctorId)) {
      byDoctor.set(p.doctorId, p)
    }
  }
  for (const d of props.doctors.slice(0, 2)) {
    const p = byDoctor.get(d.id)
    if (p?.patientName) return p
  }
  return props.patients[0] ?? null
})

const displayText = computed(() => {
  if (!displayPatient.value?.patientName) return ''
  return formatPatientLine(displayPatient.value)
})

const doctorTag = computed(() => {
  if (props.doctors.length <= 1 || !displayPatient.value?.doctorId) return ''
  const doc = props.doctors.find((d) => d.id === displayPatient.value?.doctorId)
  return doc?.name ?? ''
})
</script>

<template>
  <section class="current-patient clinic-panel">
    <div class="section-head">
      <span class="section-title">正在就诊</span>
      <span v-if="doctorTag" class="doctor-tag">{{ doctorTag }}</span>
    </div>
    <div v-if="displayText" class="patient-line">
      <PatientMarquee :text="displayText" />
    </div>
  </section>
</template>

<style scoped>
.current-patient {
  flex: 0 0 auto;
  min-height: 132px;
  padding: 14px 16px 18px;
  background: var(--color-current-panel);
  border-top: 1px solid var(--color-border-accent);
  border-bottom: 1px solid var(--color-border-accent);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-inverse);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.doctor-tag {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.patient-line {
  min-height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: var(--color-patient-highlight);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-md);
  font-size: 30px;
  font-weight: 800;
  color: var(--color-text-primary);
  letter-spacing: 1px;
  box-shadow: inset 0 0 16px rgba(14, 165, 233, 0.06);
}
</style>
