<script setup lang="ts">
import { computed } from 'vue'
import type { ClinicDoctorInfo } from '../types/clinic'
import { resolveMediaUrl } from '../utils/resolveMediaUrl'

const props = defineProps<{
  doctors: ClinicDoctorInfo[]
}>()

const avatarSrc = (doc: ClinicDoctorInfo) => resolveMediaUrl(doc.avatar)
const isOnDuty = (doc: ClinicDoctorInfo) => doc.isOnDuty === 1
const allOffDuty = computed(() => props.doctors.every((d) => d.isOnDuty !== 1))
</script>

<template>
  <section class="doctor-dual clinic-panel">
    <div class="doctor-grid">
      <div v-for="doc in doctors" :key="doc.id" class="doctor-card">
        <div class="avatar-wrap">
          <img v-if="avatarSrc(doc)" :src="avatarSrc(doc)" alt="" class="avatar" />
          <div v-else class="avatar avatar-placeholder">{{ doc.name?.charAt(0) || '值' }}</div>
        </div>
        <div class="info">
          <div class="name">{{ doc.name }}</div>
          <div class="title">{{ doc.title || '—' }}</div>
        </div>
        <div v-if="!isOnDuty(doc)" class="card-mask">
          <span>暂无值班</span>
        </div>
      </div>
    </div>
    <div v-if="allOffDuty" class="duty-mask">
      <span class="duty-mask-text">暂无值班</span>
    </div>
  </section>
</template>

<style scoped>
.doctor-dual {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  padding: 14px 12px;
  background:
    linear-gradient(var(--color-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--color-grid-line) 1px, transparent 1px),
    var(--color-doctor-panel);
  background-size: 24px 24px, 24px 24px, auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.doctor-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  min-height: 0;
}

.doctor-card {
  position: relative;
  background: transparent;
  border-right: 1px solid var(--color-border-accent);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
}

.doctor-card:last-child {
  border-right: none;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-lg);
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--color-card-border);
  box-shadow: var(--shadow-glow);
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  color: var(--color-text-inverse);
  font-size: 28px;
  font-weight: 600;
}

.info {
  min-width: 0;
  width: 100%;
}

.name {
  font-size: 26px;
  font-weight: 800;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-mask {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: var(--color-mask);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-secondary);
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
