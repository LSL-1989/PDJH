<script setup lang="ts">
import { computed } from 'vue'
import type { ClinicDoctorInfo, ClinicPatientRow } from '../types/clinic'
import { formatPatientLine } from '../utils/formatPatientLine'
import PatientMarquee from './PatientMarquee.vue'

const props = defineProps<{
  patients: ClinicPatientRow[]
  doctors: ClinicDoctorInfo[]
}>()

const doctorNameMap = computed(() => {
  const map = new Map<string, string>()
  for (const d of props.doctors) {
    map.set(d.id, d.name)
  }
  return map
})

/** 仅展示排在第一的候诊人 */
const displayPatient = computed(() => props.patients[0] ?? null)

const displayText = computed(() => {
  if (!displayPatient.value?.patientName) return ''
  return formatPatientLine(displayPatient.value)
})

const doctorTag = computed(() => {
  const p = displayPatient.value
  if (!p?.doctorId || props.doctors.length <= 1) return ''
  return doctorNameMap.value.get(p.doctorId) ?? ''
})
</script>

<template>
  <section class="waiting-list clinic-panel">
    <div class="section-head">
      <span class="section-title">等待就诊</span>
      <span v-if="doctorTag" class="doctor-tag">{{ doctorTag }}</span>
    </div>
    <div v-if="displayText" class="patient-line">
      <PatientMarquee :text="displayText" />
    </div>
  </section>
</template>

<style scoped>
.waiting-list {
  flex: 0 0 auto;
  min-height: 132px;
  padding: 14px 16px 18px;
  background: var(--color-waiting-panel);
  border-top: 1px solid var(--color-border-accent);
}

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.5px;
}

.doctor-tag {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.patient-line {
  min-height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: var(--color-primary-dim);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-md);
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text-primary);
  letter-spacing: 1px;
}
</style>
