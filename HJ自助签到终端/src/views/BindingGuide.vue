<script setup lang="ts">
/**
 * 自助签到终端设备绑定引导：本机 ID 登记、后台编号录入、等待配置。
 * 消费者：router beforeEach 未绑定/待配置时跳转 /bind。
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DeviceCodeKeyboard from '../components/DeviceCodeKeyboard.vue'
import {
  getShellDeviceId,
  getShellTerminalType,
  getStoredDeviceCode,
  setStoredDeviceCode,
  registerTerminal,
  resolveTerminal,
  TERMINAL_STATUS,
  SHELL_DEVICE_ID_MAX_LEN,
  isValidShellDeviceId,
  normalizeShellDeviceId,
  installAndroidNativeMock
} from '../utils/terminalBinding'
import { IS_DEV } from '../config/env'

const TYPE_LABELS: Record<string, string> = {
  SCREEN: '候诊区大屏',
  CLINIC: '诊室门边屏',
  CHECKIN: '自助签到'
}

const router = useRouter()
const route = useRoute()
const phase = ref<'guide' | 'manual' | 'pending' | 'error' | 'confirmShell'>('guide')
const loading = ref(false)
const terminalType = ref('CHECKIN')
const deviceCode = ref('')
const manualCode = ref('')
const pendingMsg = ref('')
const errorMsg = ref('')
const guideHint = ref('')
const manualError = ref('')
const shellConfirmId = ref('')

const terminalTypeLabel = computed(() => TYPE_LABELS[terminalType.value] || terminalType.value)

const MANUAL_INVALID_MSG = '设备编号无效，请重新确认输入'
const PENDING_POLL_MS = 3000

let pollTimer: ReturnType<typeof setInterval> | null = null
let pollInFlight = false

const stopPoll = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const ensurePendingPoll = () => {
  if (pollTimer) return
  pollTimer = setInterval(() => void recheck({ silent: true }), PENDING_POLL_MS)
}

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible' && phase.value === 'pending') {
    void recheck({ silent: true })
  }
}

const enterBusiness = () => {
  router.replace('/signin')
}

const goGuide = (hint = '') => {
  stopPoll()
  phase.value = 'guide'
  guideHint.value = hint
  errorMsg.value = ''
  manualError.value = ''
  manualCode.value = ''
}

const clearDeviceBinding = () => {
  setStoredDeviceCode('')
  deviceCode.value = ''
}

const persistDeviceBinding = (code: string) => {
  setStoredDeviceCode(code)
  deviceCode.value = code
}

const cancelBinding = () => {
  clearDeviceBinding()
  pendingMsg.value = ''
  goGuide()
}

const tryResolve = async (
  code: string,
  options: { allowRegister?: boolean; manualEntry?: boolean; silent?: boolean } = {}
) => {
  const { allowRegister = false, manualEntry = false, silent = false } = options
  if (!silent) loading.value = true
  if (manualEntry) {
    manualError.value = ''
  } else {
    deviceCode.value = code
  }

  try {
    const result = await resolveTerminal(code, terminalType.value)

    if (result.ok && result.status === TERMINAL_STATUS.READY) {
      stopPoll()
      if (manualEntry) persistDeviceBinding(code)
      enterBusiness()
      return
    }

    if (result.status === TERMINAL_STATUS.PENDING) {
      persistDeviceBinding(code)
      phase.value = 'pending'
      pendingMsg.value =
        result.msg || '请在管理后台完成签到终端诊室绑定并启用'
      ensurePendingPoll()
      return
    }

    if (result.status === TERMINAL_STATUS.NOT_REGISTERED) {
      if (allowRegister) {
        const reg = await registerTerminal(code, terminalType.value)
        if (reg.ok) {
          persistDeviceBinding(code)
          phase.value = 'pending'
          pendingMsg.value = reg.msg || '登记成功，等待管理员配置'
          ensurePendingPoll()
          return
        }
        goGuide(reg.msg || '登记失败，请稍后重试')
        return
      }
      if (manualEntry) {
        clearDeviceBinding()
        manualError.value = MANUAL_INVALID_MSG
        return
      }
      clearDeviceBinding()
      goGuide(result.msg || '本地设备编号已失效，请重新绑定')
      return
    }

    if (result.status === TERMINAL_STATUS.FROZEN) {
      if (manualEntry) {
        clearDeviceBinding()
        manualError.value = result.msg || '设备已冻结'
        return
      }
      phase.value = 'error'
      errorMsg.value = result.msg || '设备已冻结'
      stopPoll()
      return
    }

    const failMsg = result.msg || '绑定失败'
    if (manualEntry) {
      clearDeviceBinding()
      manualError.value = failMsg
      return
    }
    phase.value = 'error'
    errorMsg.value = failMsg
    stopPoll()
  } finally {
    if (!silent) loading.value = false
  }
}

/** 选择「使用本机设备 ID」：编号须 ≤20 且仅含数字/大写字母/-，先确认再登记 */
const useShellId = () => {
  const id = getShellDeviceId()
  if (!id || !isValidShellDeviceId(id)) {
    window.alert(
      `无法获取有效本机设备 ID（须为不超过 ${SHELL_DEVICE_ID_MAX_LEN} 位的数字、大写字母或连字符），请确认安卓 Shell 已注入 AndroidNative`
    )
    return
  }
  shellConfirmId.value = id
  phase.value = 'confirmShell'
}

const cancelShellConfirm = () => {
  shellConfirmId.value = ''
  phase.value = 'guide'
}

const confirmShellId = async () => {
  const id = normalizeShellDeviceId(shellConfirmId.value)
  if (!id) {
    cancelShellConfirm()
    return
  }
  setStoredDeviceCode(id)
  shellConfirmId.value = ''
  await tryResolve(id, { allowRegister: true })
}

const openManual = () => {
  clearDeviceBinding()
  manualCode.value = ''
  manualError.value = ''
  phase.value = 'manual'
}

const appendManualChar = (char: string) => {
  if (manualCode.value.length >= SHELL_DEVICE_ID_MAX_LEN) return
  manualCode.value += char
}

const backspaceManual = () => {
  manualCode.value = manualCode.value.slice(0, -1)
}

const clearManual = () => {
  manualCode.value = ''
  manualError.value = ''
}

const submitManual = async () => {
  const code = normalizeShellDeviceId(manualCode.value)
  if (!code) {
    manualError.value = `请输入有效设备编号（不超过${SHELL_DEVICE_ID_MAX_LEN}位，仅数字、大写字母与-）`
    return
  }
  manualCode.value = code
  await tryResolve(code, { allowRegister: false, manualEntry: true })
}

const recheck = async (options: { silent?: boolean } = {}) => {
  const silent = options.silent ?? false
  if (silent && pollInFlight) return
  const code = deviceCode.value || getStoredDeviceCode()
  if (!code) return
  pollInFlight = true
  try {
    await tryResolve(code, { allowRegister: false, silent })
  } finally {
    pollInFlight = false
  }
}

const bootstrapStoredBinding = async () => {
  const code = getStoredDeviceCode()
  if (!code) return
  deviceCode.value = code
  await tryResolve(code, { allowRegister: false })
}

onMounted(() => {
  if (IS_DEV) {
    installAndroidNativeMock('CHECKIN')
  }
  terminalType.value = getShellTerminalType('CHECKIN')

  if (getShellTerminalType('CHECKIN') !== 'CHECKIN') {
    phase.value = 'error'
    errorMsg.value = '当前安装包不是自助签到 (CHECKIN) 类型，请安装正确的 Shell'
    return
  }
  if (route.query.frozen === '1') {
    phase.value = 'error'
    errorMsg.value = '设备已停用，请联系管理员'
    deviceCode.value = getStoredDeviceCode()
    return
  }

  void bootstrapStoredBinding()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

watch(phase, (next) => {
  if (next === 'pending') {
    ensurePendingPoll()
  } else {
    stopPoll()
  }
})

onUnmounted(() => {
  stopPoll()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="bind-page" :class="{ 'bind-page--manual': phase === 'manual' }">
    <div v-if="phase === 'manual'" class="manual-panel neu-card">
      <header class="manual-header">
        <p class="manual-title">录入设备编号</p>
        <p class="bind-hint bind-hint--compact">请输入管理后台已添加的签到终端设备编号</p>
      </header>
      <div class="bind-input-display neu-inset" aria-live="polite">{{ manualCode || '—' }}</div>
      <p v-if="manualError" class="manual-error">{{ manualError }}</p>
      <DeviceCodeKeyboard
        class="manual-keyboard"
        @input="appendManualChar"
        @backspace="backspaceManual"
        @clear="clearManual"
        @confirm="submitManual"
      />
      <button class="neu-btn-secondary btn--compact" :disabled="loading" @click="goGuide()">返回</button>
    </div>

    <div v-else class="bind-card neu-card">
      <div class="bind-icon neu-raised" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <rect x="3" y="4" width="18" height="12" rx="2" stroke="currentColor" stroke-width="1.5" />
          <path d="M8 20h8M12 16v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </div>
      <h1>签到终端绑定</h1>
      <p class="bind-type">{{ terminalTypeLabel }}</p>

      <template v-if="phase === 'guide'">
        <p class="bind-hint">首次使用请选择绑定方式</p>
        <p v-if="guideHint" class="guide-error">{{ guideHint }}</p>
        <button class="neu-btn-primary" :disabled="loading" @click="useShellId">使用本机设备 ID</button>
        <button class="neu-btn-secondary" @click="openManual">录入设备编号</button>
      </template>

      <template v-else-if="phase === 'confirmShell'">
        <p class="status-title">确认本机设备</p>
        <p class="bind-hint">将使用以下设备 ID 向系统发起登记与绑定</p>
        <p class="device-code-label">设备 ID</p>
        <p class="device-code neu-inset">{{ shellConfirmId }}</p>
        <button class="neu-btn-primary" :disabled="loading" @click="confirmShellId">
          {{ loading ? '处理中…' : '确认并登记' }}
        </button>
        <button class="neu-btn-secondary" :disabled="loading" @click="cancelShellConfirm">取消</button>
      </template>

      <template v-else-if="phase === 'pending'">
        <p class="status-title">等待管理员配置</p>
        <p class="device-code-label">设备编号</p>
        <p class="device-code neu-inset">{{ deviceCode }}</p>
        <p class="bind-hint">{{ pendingMsg }}</p>
        <button class="neu-btn-primary" :disabled="loading" @click="recheck()">
          {{ loading ? '检测中…' : '重新检测' }}
        </button>
        <button class="neu-btn-secondary" :disabled="loading" @click="cancelBinding">取消绑定</button>
      </template>

      <template v-else>
        <p class="error-msg">{{ errorMsg }}</p>
        <p v-if="deviceCode" class="device-code neu-inset">{{ deviceCode }}</p>
        <button class="neu-btn-secondary" @click="goGuide()">返回绑定</button>
        <button class="neu-btn-primary" :disabled="loading" @click="recheck()">
          {{ loading ? '检测中…' : '重新检测' }}
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.bind-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  padding: 20px 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.bind-page--manual {
  padding: 0;
  align-items: center;
  justify-content: center;
}

.manual-panel {
  width: 410px;
  min-width: 410px;
  max-width: 410px;
  height: auto;
  min-height: 0;
  max-height: none;
  flex: 0 0 auto;
  align-self: center;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px 20px 20px;
  text-align: center;
}

.manual-header {
  flex-shrink: 0;
}

.manual-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--neu-text);
}

.bind-card {
  text-align: center;
  width: 100%;
  max-width: 100%;
  padding: 32px 20px 28px;
}

.bind-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--neu-accent);
}

.bind-icon svg {
  width: 28px;
  height: 28px;
}

h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--neu-text);
}

.bind-type {
  display: inline-block;
  margin: 0 0 24px;
  padding: 4px 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--neu-accent);
  background: rgba(45, 212, 191, 0.12);
  border: 1px solid rgba(45, 212, 191, 0.25);
  border-radius: 999px;
}

.bind-hint {
  color: var(--neu-muted);
  margin-bottom: 20px;
  font-size: 14px;
}

.bind-hint--compact {
  margin-bottom: 0;
  font-size: 13px;
}

.guide-error {
  color: var(--neu-error);
  margin: -8px 0 16px;
  font-size: 14px;
  padding: 10px 14px;
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.28);
  border-radius: 12px;
}

.manual-error {
  color: var(--neu-error);
  margin: 0;
  font-size: 14px;
}

.bind-input-display {
  min-height: 44px;
  padding: 12px 14px;
  font-family: 'Plus Jakarta Sans', monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--neu-highlight);
  letter-spacing: 1px;
  word-break: break-all;
}

.manual-keyboard {
  width: 100%;
}

.neu-btn-primary,
.neu-btn-secondary {
  margin: 10px 0;
}

.btn--compact {
  margin-top: 4px;
}

.device-code-label {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--neu-muted);
}

.device-code {
  font-family: 'Plus Jakarta Sans', monospace;
  color: var(--neu-highlight);
  word-break: break-all;
  margin: 0 0 16px;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.status-title {
  font-weight: 700;
  font-size: 18px;
  color: var(--neu-text);
  margin: 0 0 12px;
}

.error-msg {
  color: var(--neu-error);
  margin-bottom: 16px;
  font-size: 14px;
  padding: 12px 16px;
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.28);
  border-radius: 12px;
  line-height: 1.5;
}
</style>
