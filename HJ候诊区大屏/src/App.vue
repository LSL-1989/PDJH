<template>
  <div style="width: 100%; height: 100%" allowfullscreen="true">
    <router-view style="z-index: 1" />
    <div id="a" style="z-index: 99; width: 100%; height: 100%" />
    <div v-if="previewMode" class="screen-preview-badge">预览观察 · 不参与绑定与叫号</div>
  </div>
</template>

<script>
import {
  getShellTerminalType,
  getStoredDeviceCode,
  resolveTerminal,
  TERMINAL_STATUS,
  isTransientResolveFailure,
  installAndroidNativeMock
} from '@/utils/terminalBinding'
import { isScreenPreview } from '@/utils/screenPreview'

/** 业务页生命周期轮询间隔：及时感知后台冻结/解冻 */
const LIFECYCLE_POLL_MS = 3000

export default {
  name: 'App',
  data() {
    return {
      fullscreen: false,
      previewMode: false,
      _lifecycleTimer: null,
      _lifecycleInFlight: false
    }
  },
  methods: {
    getSystemConfig() {
      this.$axios
        .get('/api/SystemConfig/GetClientManifest')
        .then((res) => {
          const body = res.data || {}
          if (body.success && body.code === 200) {
            window.$sysConfig = body.data
            document.title = window.$sysConfig.SystemTitle || '排队叫号系统'
          }
        })
        .catch((err) => {
          console.error('加载系统配置失败:', err)
          window.$sysConfig = {}
        })
    },
    isBindOrMetaRoute(path) {
      return path === '/bind' || path === '/404' || path === '/wrong-terminal'
    },
    async bootstrapDeviceRoute() {
      if (this.isBindOrMetaRoute(this.$route.path)) {
        return
      }

      if (import.meta.env.DEV) {
        installAndroidNativeMock('SCREEN')
      }

      const shellType = getShellTerminalType('SCREEN')
      if (shellType !== 'SCREEN') {
        this.$router.replace('/wrong-terminal')
        return
      }

      /** 仅用户确认过的本地编号参与自动解析；本机 Shell ID 不自动绑定 */
      const deviceCode = getStoredDeviceCode()
      if (!deviceCode) {
        this.$router.replace('/bind')
        return
      }

      try {
        const result = await resolveTerminal(deviceCode, 'SCREEN')
        if (result.ok && result.status === TERMINAL_STATUS.READY) {
          const data = result.data || {}
          if (data.terminalType === 'CLINIC') {
            this.$router.replace({ path: '/bind', query: { wrongShell: '1' } })
            return
          }
          const routePath = data.templateRoute || '/default'
          const screenId = data.screenId || data.recordId
          window.$screenAssignedId = screenId
          const target = `${routePath}?screenId=${screenId}`
          if (this.$route.fullPath !== target) {
            this.$router.replace(target)
          }
          return
        }
        if (result.status === TERMINAL_STATUS.FROZEN) {
          this.$router.replace({ path: '/bind', query: { frozen: '1' } })
          return
        }
        // 瞬时网络失败：进入绑定页网络待机并自动重试，服务恢复后回到业务页
        if (isTransientResolveFailure(result)) {
          this.$router.replace({ path: '/bind', query: { network: '1' } })
          return
        }
        this.$router.replace('/bind')
      } catch (err) {
        console.error('设备认证连接服务器失败', err)
        this.$router.replace({ path: '/bind', query: { network: '1' } })
      }
    },
    /**
     * 业务页周期 Resolve：后台冻结后尽快离开业务模板；
     * 绑定页自身轮询解冻/网络恢复，此处跳过以免抢路由。
     * 瞬时网络失败不离开业务页，由 Main 轮询与 STOMP 重连自行恢复。
     */
    async checkLifecycle() {
      if (this._lifecycleInFlight) return
      if (this.isBindOrMetaRoute(this.$route.path)) return
      const deviceCode = getStoredDeviceCode()
      if (!deviceCode) return

      this._lifecycleInFlight = true
      try {
        const result = await resolveTerminal(deviceCode, 'SCREEN')
        if (isTransientResolveFailure(result)) {
          return
        }
        if (result.status === TERMINAL_STATUS.FROZEN) {
          this.$router.replace({ path: '/bind', query: { frozen: '1' } })
          return
        }
        if (!result.ok || result.status !== TERMINAL_STATUS.READY) {
          this.$router.replace('/bind')
        }
      } catch (err) {
        console.warn('[Screen] lifecycle poll failed', err)
      } finally {
        this._lifecycleInFlight = false
      }
    },
    startLifecyclePoll() {
      if (this._lifecycleTimer) return
      this._lifecycleTimer = setInterval(() => void this.checkLifecycle(), LIFECYCLE_POLL_MS)
    },
    stopLifecyclePoll() {
      if (this._lifecycleTimer) {
        clearInterval(this._lifecycleTimer)
        this._lifecycleTimer = null
      }
    },
    onVisibilityChange() {
      if (document.visibilityState === 'visible') {
        void this.checkLifecycle()
      }
    }
  },
  created() {
    this.previewMode = isScreenPreview(this.$route)
  },
  mounted() {
    this.getSystemConfig()
    if (this.previewMode) {
      const query = this.$route.query || {}
      const screenId = query.screenId || query.ScreensID || query.screensID
      if (screenId) window.$screenAssignedId = screenId
      return
    }
    this.bootstrapDeviceRoute()
    this.startLifecyclePoll()
    document.addEventListener('visibilitychange', this.onVisibilityChange)
  },
  beforeUnmount() {
    this.stopLifecyclePoll()
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
  }
}
</script>

<style>
@import './common/fonts/font.css';
#a {
  position: absolute;
  top: 0;
  left: 0;
  background: transparent;
  /* 全屏透明层仅作叠层容器，禁止拦截下方绑定页/大屏点击 */
  pointer-events: none;
}
.screen-preview-badge {
  position: fixed;
  top: 10px;
  left: 220px;
  z-index: 200;
  pointer-events: none;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.92);
  color: #fff;
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: 0.02em;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}
</style>
