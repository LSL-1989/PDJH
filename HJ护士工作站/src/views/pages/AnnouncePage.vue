/**
 * 发布公告页 — 对齐原型 page-announce
 * 公告与屏幕控制后端接口尚未提供，UI 按原型还原
 */
<template>
  <div class="nd-page">
    <div class="page-title">
      <svg class="ln-icon"><use href="#i-doc" /></svg>
      发布公告
    </div>
    <div class="page-subtitle">管理候诊大屏公告、紧急通知与屏幕控制</div>

    <div class="col-layout">
      <div class="content-card">
        <div class="card-title">发布新公告</div>
        <div style="margin-bottom: var(--space-3)">
          <label class="field-label">公告类型</label>
          <div style="display: flex; gap: var(--space-2)">
            <button
              type="button"
              class="btn btn-sm"
              :class="annType === 'normal' ? 'btn-primary' : 'btn-outline'"
              :style="annType === 'normal' ? {} : { borderColor: 'var(--indigo)', color: 'var(--indigo)' }"
              @click="annType = 'normal'"
            >
              普通公告
            </button>
            <button
              type="button"
              class="btn btn-sm"
              :class="annType === 'urgent' ? 'btn-coral' : 'btn-outline'"
              @click="annType = 'urgent'"
            >
              紧急通知
            </button>
          </div>
        </div>
        <div style="margin-bottom: var(--space-3)">
          <label class="field-label">目标屏幕</label>
          <div style="display: flex; gap: var(--space-2)">
            <button
              v-for="t in targets"
              :key="t.id"
              type="button"
              class="btn btn-sm"
              :class="target === t.id ? 'btn-primary' : 'btn-outline'"
              @click="target = t.id"
            >
              {{ t.label }}
            </button>
          </div>
        </div>
        <div style="margin-bottom: var(--space-3)">
          <label class="field-label">公告标题</label>
          <input v-model="title" class="input" placeholder="输入公告标题..." />
        </div>
        <div style="margin-bottom: var(--space-4)">
          <label class="field-label">公告内容</label>
          <textarea v-model="body" class="textarea" placeholder="输入公告内容..." rows="4" />
        </div>
        <div class="btn-group">
          <button class="btn btn-primary btn-lg" disabled title="公告发布接口未接">
            <svg class="ln-icon"><use href="#i-skip" /></svg> 发布公告
          </button>
          <button class="btn btn-outline" disabled title="草稿接口未接">
            <svg class="ln-icon"><use href="#i-doc" /></svg> 保存草稿
          </button>
        </div>
        <p class="nd-gap-hint" style="margin-top: 12px">公告发布 / 草稿 / 屏幕内容管理接口未对接</p>
      </div>

      <div class="content-card">
        <div class="card-title">已发布公告</div>
        <div class="announce-list">
          <div class="nd-empty">暂无公告数据（列表接口未接）</div>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="card-title">屏幕控制</div>
      <div class="card-grid col3">
        <div class="stat-card accent-success" style="text-align: center; cursor: default">
          <div style="margin-bottom: var(--space-2); display: flex; justify-content: center">
            <svg class="ln-icon" style="width: 36px; height: 36px"><use href="#i-monitor" /></svg>
          </div>
          <div style="font-weight: 600; margin-bottom: 4px">综合显示大屏</div>
          <div style="font-size: 12px; color: var(--text-muted)">候诊大厅 · 状态未知</div>
          <button class="btn btn-sm btn-outline" style="margin-top: var(--space-3)" disabled>
            <svg class="ln-icon"><use href="#i-wrench" /></svg> 管理内容
          </button>
        </div>
        <div class="stat-card accent-indigo" style="text-align: center; cursor: default">
          <div style="margin-bottom: var(--space-2); display: flex; justify-content: center">
            <svg class="ln-icon" style="width: 36px; height: 36px"><use href="#i-monitor" /></svg>
          </div>
          <div style="font-weight: 600; margin-bottom: 4px">诊室门口屏</div>
          <div style="font-size: 12px; color: var(--text-muted)">设备状态接口未接</div>
          <button class="btn btn-sm btn-outline" style="margin-top: var(--space-3)" disabled>
            <svg class="ln-icon"><use href="#i-wrench" /></svg> 统一管理
          </button>
        </div>
        <div class="stat-card accent-amber" style="text-align: center; cursor: default">
          <div style="margin-bottom: var(--space-2); display: flex; justify-content: center">
            <svg class="ln-icon" style="width: 36px; height: 36px"><use href="#i-monitor" /></svg>
          </div>
          <div style="font-weight: 600; margin-bottom: 4px">窗口叫号屏</div>
          <div style="font-size: 12px; color: var(--text-muted)">设备状态接口未接</div>
          <button class="btn btn-sm btn-outline" style="margin-top: var(--space-3)" disabled>
            <svg class="ln-icon"><use href="#i-wrench" /></svg> 统一管理
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const annType = ref('normal')
const target = ref('hall')
const title = ref('')
const body = ref('')
const targets = [
  { id: 'hall', label: '综合大屏' },
  { id: 'door', label: '诊室门口屏' },
  { id: 'all', label: '全部屏幕' }
]
</script>

<style scoped>
.field-label {
  font-size: 12px;
  color: var(--text-muted);
  display: block;
  margin-bottom: 4px;
}
</style>
