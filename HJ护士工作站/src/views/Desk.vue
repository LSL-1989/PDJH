/**
 * 分诊护士工作站主壳 — 侧栏 + 顶栏 + 多页面
 * 布局对齐「分诊护士工作站_原型.html」
 */
<template>
  <div class="nd-app">
    <IconSprite />
    <AppSidebar
      :current="page"
      :waiting-badge="overview.waitingCount"
      :alert-badge="alerts.length || undefined"
      @navigate="page = $event"
      @logout="handleLogout"
    />
    <div class="nd-main">
      <AppTopBar
        :user-name="session.userName"
        :zone-name="session.zoneName"
        :can-switch-zone="session.canSwitchZone"
        :alert-count="alerts.length"
        @notify="page = 'dashboard'"
        @switch-zone="openSwitchZone"
      />

      <DashboardPage
        v-show="page === 'dashboard'"
        :overview="overview"
        :alerts="alerts"
        :rooms="session.rooms"
        :zone-name="session.zoneName"
        :refreshing="refreshing"
        @refresh="refreshAll"
      />
      <OperationPage
        v-show="page === 'operation'"
        :active="page === 'operation'"
        :rooms="session.rooms"
        :departments="session.departments"
        :alerts="alerts"
        :overview="overview"
        @done="refreshAll"
      />
      <CallAssistPage
        v-show="page === 'call'"
        :active="page === 'call'"
        :rooms="session.rooms"
      />
      <RoomsDevicesPage
        v-show="page === 'rooms'"
        :rooms="session.rooms"
        :departments="session.departments"
      />
      <AnnouncePage v-show="page === 'announce'" />
      <SystemPage v-show="page === 'system'" :active="page === 'system'" />
    </div>

    <div v-if="switchVisible" class="nd-modal-mask" @click.self="switchVisible = false">
      <div class="nd-modal">
        <div class="nd-modal-title">切换当班诊区</div>
        <p class="nd-modal-hint">仅列出授权诊区；切换会写入今日排班并留痕。</p>
        <select v-model="switchZoneId" class="select" style="width:100%;margin:12px 0;">
          <option v-for="z in session.authorizedZones" :key="z.id" :value="z.id">
            {{ z.name }}{{ z.source === 'RESIDENT' ? '（常驻）' : '' }}
          </option>
        </select>
        <div class="nd-modal-actions">
          <button type="button" class="btn btn-sm btn-outline" @click="switchVisible = false">取消</button>
          <button type="button" class="btn btn-sm" :disabled="switching" @click="confirmSwitchZone">确认切换</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../stores/session'
import { getSession, getOverview, getAlerts, switchZone } from '../api/nurseDesk'
import { wsService } from '../utils/websocket'
import IconSprite from '../components/layout/IconSprite.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopBar from '../components/layout/AppTopBar.vue'
import DashboardPage from './pages/DashboardPage.vue'
import OperationPage from './pages/OperationPage.vue'
import CallAssistPage from './pages/CallAssistPage.vue'
import RoomsDevicesPage from './pages/RoomsDevicesPage.vue'
import AnnouncePage from './pages/AnnouncePage.vue'
import SystemPage from './pages/SystemPage.vue'

const router = useRouter()
const session = useSessionStore()

const page = ref('dashboard')
const overview = ref({})
const alerts = ref([])
const refreshing = ref(false)
const switchVisible = ref(false)
const switchZoneId = ref('')
const switching = ref(false)

async function refreshAll() {
  refreshing.value = true
  try {
    const [sess, ov, al] = await Promise.all([getSession(), getOverview(), getAlerts()])
    if (sess.success) session.setSession(sess.data)
    else if (sess.msg) ElMessage.warning(sess.msg)
    if (ov.success) overview.value = ov.data || {}
    if (al.success) alerts.value = al.data || []
  } catch (_) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function openSwitchZone() {
  switchZoneId.value = session.zoneId
  switchVisible.value = true
}

async function confirmSwitchZone() {
  if (!switchZoneId.value || switchZoneId.value === session.zoneId) {
    switchVisible.value = false
    return
  }
  switching.value = true
  try {
    const res = await switchZone({ zoneId: switchZoneId.value })
    if (res?.success) {
      session.setSession(res.data)
      switchVisible.value = false
      ElMessage.success('已切换诊区')
      await refreshAll()
    } else {
      ElMessage.error(res?.msg || '切换失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '切换失败')
  } finally {
    switching.value = false
  }
}

function handleLogout() {
  session.logout()
  wsService.disconnect()
  router.push({ name: 'Login' })
}

onMounted(() => {
  refreshAll()
  wsService.connect((msg) => {
    if (msg?.type === 'STATE_UPDATE') refreshAll()
  })
})

onUnmounted(() => wsService.disconnect())
</script>

<style scoped>
.nd-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(8, 20, 36, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
}
.nd-modal {
  width: min(420px, calc(100vw - 32px));
  background: var(--glass-bg, #fff);
  border: 1px solid var(--glass-border, #e0e0e0);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
}
.nd-modal-title {
  font-size: 16px;
  font-weight: 700;
}
.nd-modal-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-muted, #80868b);
}
.nd-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
