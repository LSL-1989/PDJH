<script setup lang="ts">
import { computed } from 'vue'
import type { ClinicDoctorInfo } from '../types/clinic'
import { resolveMediaUrl } from '../utils/resolveMediaUrl'

const props = defineProps<{
  doctor: ClinicDoctorInfo
}>()

const avatarSrc = computed(() => resolveMediaUrl(props.doctor.avatar))
const isOnDuty = computed(() => props.doctor.isOnDuty === 1)
</script>

<template>
  <section class="doctor-single clinic-panel">
    <div class="doctor-top">
      <div class="avatar-wrap">
        <img v-if="avatarSrc" :src="avatarSrc" alt="" class="avatar" />
        <div v-else class="avatar avatar-placeholder">{{ doctor.name?.charAt(0) || '值' }}</div>
      </div>
      <div class="identity">
        <div class="name">{{ doctor.name }}</div>
        <div class="title">{{ doctor.title || '—' }}</div>
      </div>
    </div>
    <div v-if="doctor.remarks" class="doctor-intro">{{ doctor.remarks }}</div>
    <div v-if="doctor.specialty" class="doctor-specialty">
      <span class="label">擅长</span>
      <span class="text">{{ doctor.specialty }}</span>
    </div>
    <div v-if="!isOnDuty" class="duty-mask">
      <span class="duty-mask-text">暂无值班</span>
    </div>
  </section>
</template>

<style scoped>
.doctor-single {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  padding: 14px 16px;
  background:
    linear-gradient(var(--color-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-grid-line) 1px, transparent 1px),
    var(--color-doctor-panel);
  background-size: 24px 24px, 24px 24px, auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.doctor-top {
  display: flex;
  gap: 14px;
  align-items: stretch;
  flex-shrink: 0;
}

.avatar-wrap {
  flex-shrink: 0;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: var(--radius-lg);
  object-fit: cover;
  border: 1px solid var(--color-card-border);
  box-shadow: var(--shadow-glow);
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  color: var(--color-text-inverse);
  font-size: 34px;
  font-weight: 600;
}

.identity {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 88px;
}

.name {
  font-size: 34px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-secondary);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doctor-intro {
  flex: 1;
  min-height: 0;
  font-size: 15px;
  line-height: 1.65;
  color: var(--color-text-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.doctor-specialty {
  flex-shrink: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.doctor-specialty .label {
  color: var(--color-primary);
  font-weight: 700;
  margin-right: 6px;
}

.duty-mask {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: var(--color-mask);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.duty-mask-text {
  font-size: 32px;
  font-weight: 800;
  color: var(--color-text-secondary);
  letter-spacing: 2px;
}
</style>
