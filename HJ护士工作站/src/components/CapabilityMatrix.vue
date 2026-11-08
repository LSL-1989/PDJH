<template>
  <div class="cap-matrix" :class="`cap-matrix--${mode}`">
    <div class="cap-matrix__head">
      <div class="cap-matrix__head-left">
        <span class="cap-matrix__badge">{{ total }} 项核心能力</span>
        <h3 v-if="mode === 'desk'" class="cap-matrix__title">工作站能力矩阵</h3>
        <h2 v-else class="cap-matrix__title cap-matrix__title--hero">智慧分诊 · 全能处置</h2>
        <p class="cap-matrix__sub">
          <template v-if="mode === 'desk'">
            选患者后解锁处置按钮；带
            <el-icon class="cap-matrix__spark"><Lightning /></el-icon>
            可一键直达
          </template>
          <template v-else>
            告警驱动 · 签到兜底 · 队列干预 · 全程审计留痕
          </template>
        </p>
      </div>
      <div v-if="mode === 'desk' && alertCount > 0" class="cap-matrix__pulse">
        <span class="cap-matrix__pulse-dot" />
        {{ alertCount }} 条待办
      </div>
    </div>

    <div class="cap-matrix__groups">
      <section
        v-for="(group, gi) in CAPABILITY_GROUPS"
        :key="group.id"
        class="cap-group"
        :style="{ '--g-color': group.color, '--g-bg': group.bg, '--delay': `${gi * 80}ms` }"
      >
        <div class="cap-group__bar">
          <span class="cap-group__dot" />
          <span class="cap-group__name">{{ group.name }}</span>
          <span class="cap-group__count">{{ group.items.length }} 项</span>
        </div>
        <div class="cap-group__grid">
          <component
            :is="item.action && mode === 'desk' ? 'button' : 'div'"
            v-for="item in group.items"
            :key="item.id"
            type="button"
            class="cap-tile"
            :class="{
              'cap-tile--action': item.action && mode === 'desk',
              'cap-tile--patient': mode === 'desk' && patientRequired(item.id)
            }"
            @click="onTileClick(item)"
          >
            <div class="cap-tile__icon" :style="{ background: group.bg, color: group.color }">
              <el-icon :size="mode === 'login' ? 22 : 18"><component :is="item.icon" /></el-icon>
              <el-icon v-if="item.action && mode === 'desk'" class="cap-tile__spark" :size="12">
                <Lightning />
              </el-icon>
            </div>
            <div class="cap-tile__text">
              <span class="cap-tile__name">{{ item.name }}</span>
              <span class="cap-tile__desc">{{ item.desc }}</span>
            </div>
            <span
              v-if="mode === 'desk' && patientRequired(item.id)"
              class="cap-tile__tag"
            >需选患者</span>
          </component>
        </div>
      </section>
    </div>

    <div v-if="mode === 'desk'" class="cap-matrix__flow nd-card">
      <span class="cap-matrix__flow-label">推荐流程</span>
      <div class="cap-matrix__flow-steps">
        <span>① 看告警</span>
        <el-icon><ArrowRight /></el-icon>
        <span>② 选患者</span>
        <el-icon><ArrowRight /></el-icon>
        <span>③ 选原因</span>
        <el-icon><ArrowRight /></el-icon>
        <span>④ 执行处置</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { ArrowRight, Lightning } from '@element-plus/icons-vue'
import {
  CAPABILITY_GROUPS,
  TOTAL_CAPABILITIES,
  PATIENT_REQUIRED_IDS
} from '../constants/deskCapabilities'

defineProps({
  mode: { type: String, default: 'desk' },
  alertCount: { type: Number, default: 0 }
})
const emit = defineEmits(['action'])

const total = TOTAL_CAPABILITIES

function patientRequired(id) {
  return PATIENT_REQUIRED_IDS.has(id)
}

function onTileClick(item) {
  if (!item.action) return
  if (item.action === 'search' || item.action === 'sign-in' || item.action === 'alerts') {
    emit('action', item.action)
    return
  }
  ElMessage.info('请先在上方告警列表或搜索中选择患者')
}
</script>

<style scoped>
.cap-matrix--login {
  color: #fff;
}
.cap-matrix--login .cap-matrix__sub,
.cap-matrix--login .cap-group__name,
.cap-matrix--login .cap-tile__desc {
  color: rgba(255, 255, 255, 0.78);
}
.cap-matrix--login .cap-tile {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
}
.cap-matrix--login .cap-tile:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-3px);
}
.cap-matrix--login .cap-group__count {
  color: rgba(255, 255, 255, 0.55);
}
.cap-matrix--login .cap-matrix__badge {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.cap-matrix__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.25rem;
}
.cap-matrix__badge {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  background: #e3f2fd;
  color: var(--nd-primary);
  margin-bottom: 0.5rem;
  letter-spacing: 0.04em;
}
.cap-matrix__title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--nd-text);
}
.cap-matrix__title--hero {
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 800;
  line-height: 1.25;
  background: linear-gradient(120deg, #fff 0%, #90caf9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.cap-matrix__sub {
  margin: 0.35rem 0 0;
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}
.cap-matrix__spark {
  color: #ffd54f;
}
.cap-matrix__pulse {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: #ffebee;
  color: #e53935;
  font-size: 0.8125rem;
  font-weight: 700;
  flex-shrink: 0;
}
.cap-matrix__pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e53935;
  animation: cap-pulse 1.2s ease-in-out infinite;
}
@keyframes cap-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.cap-matrix__groups {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.cap-group {
  animation: cap-fade-up 0.5s ease backwards;
  animation-delay: var(--delay);
}
@keyframes cap-fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.cap-group__bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.cap-group__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--g-color);
  box-shadow: 0 0 8px var(--g-color);
}
.cap-group__name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--nd-text);
}
.cap-matrix--login .cap-group__name {
  color: #fff;
}
.cap-group__count {
  font-size: 0.75rem;
  color: var(--nd-text-secondary);
  margin-left: auto;
}
.cap-group__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 0.5rem;
}
.cap-matrix--login .cap-group__grid {
  grid-template-columns: repeat(2, 1fr);
}
.cap-matrix--desk .cap-group__grid {
  grid-template-columns: 1fr;
}
@media (min-width: 1280px) {
  .cap-matrix--desk .cap-group__grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.cap-tile {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.65rem 0.75rem;
  border-radius: 10px;
  border: 1px solid var(--nd-border);
  background: var(--nd-surface);
  text-align: left;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
  position: relative;
  width: 100%;
  font: inherit;
  color: inherit;
}
.cap-tile--action {
  cursor: pointer;
}
.cap-tile--action:hover {
  border-color: var(--g-color, var(--nd-primary));
  box-shadow: 0 4px 14px rgba(30, 136, 229, 0.15);
  transform: translateY(-2px);
}
.cap-tile--patient {
  border-style: dashed;
}
.cap-tile__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}
.cap-tile__spark {
  position: absolute;
  top: -2px;
  right: -2px;
  color: #fb8c00;
}
.cap-tile__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.cap-tile__name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--nd-text);
}
.cap-matrix--login .cap-tile__name {
  color: #fff;
}
.cap-tile__desc {
  font-size: 0.7rem;
  color: var(--nd-text-secondary);
  line-height: 1.35;
  margin-top: 2px;
}
.cap-tile__tag {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 0.625rem;
  padding: 1px 5px;
  border-radius: 4px;
  background: #f5f5f5;
  color: #9e9e9e;
}

.cap-matrix__flow {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 0.75rem;
}
.cap-matrix__flow-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--nd-primary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.cap-matrix__flow-steps {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35rem;
  font-size: 0.8125rem;
  color: var(--nd-text-secondary);
}
</style>
