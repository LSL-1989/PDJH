/**
 * 分诊护士台侧栏导航
 * 消费者：Desk.vue；对齐原型 sidebar
 */
<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <div class="icon">➕</div>
      <div class="brand">分诊护士台<span>急诊分诊管理系统</span></div>
    </div>
    <nav class="sidebar-nav">
      <div class="nav-section">核心功能</div>
      <button
        v-for="item in coreNav"
        :key="item.id"
        type="button"
        class="nav-item"
        :class="{ active: current === item.id }"
        @click="$emit('navigate', item.id)"
      >
        <svg class="nav-icon ln-icon"><use :href="`#${item.icon}`" /></svg>
        <span class="nav-text">{{ item.label }}</span>
        <span v-if="item.badge" class="badge">{{ item.badge }}</span>
      </button>
      <div class="nav-section">信息与管理</div>
      <button
        v-for="item in manageNav"
        :key="item.id"
        type="button"
        class="nav-item"
        :class="{ active: current === item.id }"
        @click="$emit('navigate', item.id)"
      >
        <svg class="nav-icon ln-icon"><use :href="`#${item.icon}`" /></svg>
        <span class="nav-text">{{ item.label }}</span>
        <span v-if="item.badge" class="badge">{{ item.badge }}</span>
      </button>
    </nav>
    <div class="sidebar-footer">
      <button type="button" class="nav-item" style="font-size: 13px" @click="$emit('logout')">
        <svg class="nav-icon ln-icon"><use href="#i-power" /></svg>
        <span class="nav-text">退出登录</span>
      </button>
      <button
        type="button"
        class="sidebar-toggle"
        title="折叠/展开菜单"
        @click="collapsed = !collapsed"
      >
        {{ collapsed ? '▶' : '◀' }}
      </button>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  current: { type: String, default: 'dashboard' },
  waitingBadge: { type: [String, Number], default: '' },
  alertBadge: { type: [String, Number], default: '' }
})
defineEmits(['navigate', 'logout'])

const collapsed = ref(false)

const coreNav = computed(() => [
  { id: 'dashboard', label: '工作台', icon: 'i-grid' },
  {
    id: 'operation',
    label: '操作区',
    icon: 'i-monitor',
    badge: props.waitingBadge ? `${props.waitingBadge}待` : ''
  },
  { id: 'call', label: '辅助呼叫', icon: 'i-speaker' }
])

const manageNav = computed(() => [
  { id: 'rooms', label: '诊室和设备', icon: 'i-room' },
  { id: 'announce', label: '发布公告', icon: 'i-doc' },
  {
    id: 'system',
    label: '系统管理',
    icon: 'i-gear',
    badge: props.alertBadge || ''
  }
])
</script>

<style scoped>
.sidebar {
  width: var(--side-w);
  min-width: var(--side-w);
  background: linear-gradient(180deg, var(--navy-800) 0%, var(--navy-900) 100%);
  border-right: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  transition: width var(--normal);
  z-index: 100;
}
.sidebar-logo {
  padding: var(--space-5) var(--space-6);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  border-bottom: 1px solid var(--glass-border);
}
.sidebar-logo .icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--coral), var(--amber));
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.sidebar-logo .brand {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.sidebar-logo .brand span {
  color: var(--amber);
  font-size: 12px;
  display: block;
  font-weight: 400;
  margin-top: 2px;
}
.sidebar-nav {
  flex: 1;
  padding: var(--space-3);
  overflow-y: auto;
  overflow-x: hidden;
}
.nav-section {
  color: var(--text-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  padding: var(--space-3) var(--space-3) var(--space-2);
  white-space: nowrap;
  overflow: hidden;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--fast);
  margin-bottom: 2px;
  border: 1px solid transparent;
  white-space: nowrap;
  width: 100%;
  background: none;
  font-family: inherit;
  text-align: left;
}
.nav-item .nav-icon {
  width: 20px;
  text-align: center;
  font-size: 16px;
  flex-shrink: 0;
}
.nav-item .badge {
  margin-left: auto;
  background: var(--coral);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
  flex-shrink: 0;
}
.nav-item:hover {
  background: var(--glass-hover);
  color: var(--text-primary);
}
.nav-item.active {
  background: linear-gradient(135deg, var(--navy-500), var(--navy-600));
  color: var(--text-primary);
  border-color: var(--glass-border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}
.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sidebar-footer .nav-item {
  font-size: 13px;
  flex: 1;
  margin-bottom: 0;
}
.sidebar-toggle {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-muted);
  font-size: 14px;
  flex-shrink: 0;
  transition: all var(--fast);
  border: 1px solid transparent;
  margin-left: var(--space-2);
  background: none;
}
.sidebar-toggle:hover {
  color: var(--amber);
  background: var(--glass-hover);
  border-color: var(--glass-border);
}
.sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}
.sidebar.collapsed .brand {
  display: none;
}
.sidebar.collapsed .sidebar-logo {
  justify-content: center;
  padding: var(--space-4) var(--space-2);
}
.sidebar.collapsed .sidebar-logo .icon {
  margin: 0;
}
.sidebar.collapsed .nav-section {
  display: none;
}
.sidebar.collapsed .nav-item {
  gap: 0;
  justify-content: center;
  padding: var(--space-3) var(--space-2);
}
.sidebar.collapsed .nav-item .nav-text,
.sidebar.collapsed .nav-item .badge {
  display: none;
}
.sidebar.collapsed .sidebar-footer {
  flex-direction: column;
  gap: var(--space-2);
}
.sidebar.collapsed .sidebar-footer .nav-item {
  flex: 0;
}
.sidebar.collapsed .sidebar-toggle {
  margin-left: 0;
  margin-top: var(--space-1);
}
</style>
