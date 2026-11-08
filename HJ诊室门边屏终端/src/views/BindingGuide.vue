<script setup lang="ts">
/**
 * 诊室门边屏终端设备绑定引导：本机 ID 登记、后台编号录入、等待配置。
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

const TYPE_LABELS: Record<string, string> = {
  SCREEN: '候诊区大屏',
  CLINIC: '诊室门边屏',
  CHECKIN: '自助签到'
}

const router = useRouter()
const route = useRoute()
const phase = ref<'guide' | 'manual' | 'pending' | 'error' | 'confirmShell'>('guide')
const loading = ref(false)
const terminalType = ref('CLINIC')
const deviceCode = ref('')
const manualCode = ref('')
const pendingMsg = ref('')
const errorMsg = ref('')
const guideHint = ref('')
const manualError = ref('')
const shellConfirmId = ref('')

const terminalTypeLabel = computed(() => TYPE_LABELS[terminalType.value] || terminalType.value)

/** 录入编号查不到后台门边屏时的固定提示 */
const MANUAL_INVALID_MSG = '设备编号无效，请重新确认输入'

/** 等待配置页轮询间隔（毫秒） */
const PENDING_POLL_MS = 3000

let pollTimer: ReturnType<typeof setInterval> | null = null
let pollInFlight = false

const stopPoll = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 启动等待配置页后台轮询（不重复创建定时器） */
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
  router.replace('/')
}

const goGuide = (hint = '') => {
  stopPoll()
  phase.value = 'guide'
  guideHint.value = hint
  errorMsg.value = ''
  manualError.value = ''
  manualCode.value = ''
}

/** 清除本地已绑定的设备编号（取消绑定 / 录入无效时） */
const clearDeviceBinding = () => {
  setStoredDeviceCode('')
  try {
    localStorage.removeItem('pdjh_clinic_device_code')
  } catch {
    /* 忽略 */
  }
  deviceCode.value = ''
}

/** 录入编号校验通过后持久化 */
const persistDeviceBinding = (code: string) => {
  setStoredDeviceCode(code)
  deviceCode.value = code
}

/** 取消绑定：清除本地编号并回到绑定方式选择页 */
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
      if (result.data?.terminalType === 'SCREEN') {
        if (manualEntry) {
          clearDeviceBinding()
          manualError.value = MANUAL_INVALID_MSG
          return
        }
        phase.value = 'error'
        errorMsg.value = '该设备登记为候诊区大屏 (SCREEN)，请使用 SCREEN 壳访问 /screen/'
        stopPoll()
        return
      }
      stopPoll()
      if (manualEntry) persistDeviceBinding(code)
      enterBusiness()
      return
    }

    if (result.status === TERMINAL_STATUS.PENDING) {
      persistDeviceBinding(code)
      phase.value = 'pending'
      pendingMsg.value =
        result.msg || '请在【屏幕管理】将终端类型设为 CLINIC 并绑定 1 个物理诊室后启用'
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
      phase.value = 'error'
      errorMsg.value = result.msg || '设备未在管理后台登记，请联系管理员添加门边屏信息'
      stopPoll()
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

/** 用户确认本机 ID 及登记意图后发起解析；未登记则自动登记 */
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
  manualCode.value += char
}

const backspaceManual = () => {
  manualCode.value = manualCode.value.slice(0, -1)
}

const clearManual = () => {
  manualCode.value = ''
  manualError.value = ''
}

/** 录入编号：仅查询后台已添加的门边屏；成功后再持久化，无效编号不保存 */
const submitManual = async () => {
  const code = manualCode.value.trim().toUpperCase()
  if (!code) {
    manualError.value = '请输入设备编号'
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

/** 启动时若用户已确认过本地绑定编号，按状态进入对应页面 */
const bootstrapStoredBinding = async () => {
  const code = getStoredDeviceCode()
  if (!code) return
  deviceCode.value = code
  await tryResolve(code, { allowRegister: false })
}

onMounted(() => {
  if (import.meta.env.DEV) {
    installAndroidNativeMock('CLINIC')
  }
  terminalType.value = getShellTerminalType('CLINIC')

  if (route.query.wrongShell === '1') {
    phase.value = 'error'
    errorMsg.value = '该设备为候诊区大屏 (SCREEN)，当前为门边屏壳。请改用 SCREEN 壳访问 /screen/'
    return
  }
  if (getShellTerminalType('CLINIC') !== 'CLINIC') {
    phase.value = 'error'
    errorMsg.value = '当前安装包不是门边屏 (CLINIC) 类型，请安装正确的 Shell'
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
    <!-- 录入页：独立固定比例面板，随 ScreenFrame 450×800 等比缩放 -->
    <div v-if="phase === 'manual'" class="manual-panel">
      <header class="manual-header">
        <p class="manual-title">录入设备编号</p>
        <p class="bind-hint bind-hint--compact">请输入管理后台已添加的门边屏设备编号</p>
      </header>
      <div class="bind-input-display" aria-live="polite">{{ manualCode || '—' }}</div>
      <p v-if="manualError" class="manual-error">{{ manualError }}</p>
      <DeviceCodeKeyboard
        class="manual-keyboard"
        @input="appendManualChar"
        @backspace="backspaceManual"
        @clear="clearManual"
        @confirm="submitManual"
      />
      <button class="btn secondary btn--compact" :disabled="loading" @click="goGuide()">返回</button>
    </div>

    <div v-else class="bind-card">
      <div class="bind-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <rect x="4" y="2" width="16" height="20" rx="2" stroke="currentColor" stroke-width="1.8" />
          <path d="M8 7h8M8 11h8M8 15h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </div>
      <h1>终端设备绑定</h1>
      <p class="bind-type">{{ terminalTypeLabel }}</p>

      <template v-if="phase === 'guide'">
        <p class="bind-hint">首次使用请选择绑定方式</p>
        <p v-if="guideHint" class="guide-error">{{ guideHint }}</p>
        <div class="btn-group">
          <button class="btn primary" :disabled="loading" @click="useShellId">使用本机设备 ID</button>
          <button class="btn secondary" @click="openManual">录入设备编号</button>
        </div>
      </template>

      <template v-else-if="phase === 'confirmShell'">
        <p class="status-title">确认本机设备</p>
        <p class="bind-hint">将使用以下设备 ID 向系统发起登记与绑定</p>
        <p class="device-code-label">设备 ID</p>
        <p class="device-code">{{ shellConfirmId }}</p>
        <div class="btn-group">
          <button class="btn primary" :disabled="loading" @click="confirmShellId">
            {{ loading ? '处理中…' : '确认并登记' }}
          </button>
          <button class="btn secondary" :disabled="loading" @click="cancelShellConfirm">取消</button>
        </div>
      </template>

      <template v-else-if="phase === 'pending'">
        <p class="status-title">等待管理员配置</p>
        <p class="device-code-label">设备编号</p>
        <p class="device-code">{{ deviceCode }}</p>
        <p class="bind-hint">{{ pendingMsg }}</p>
        <div class="btn-group">
          <button class="btn primary" :disabled="loading" @click="recheck()">
            {{ loading ? '检测中…' : '重新检测' }}
          </button>
          <button class="btn secondary" :disabled="loading" @click="cancelBinding">取消绑定</button>
        </div>
      </template>

      <template v-else>
        <p class="error-msg">{{ errorMsg }}</p>
        <p v-if="deviceCode" class="device-code">{{ deviceCode }}</p>
        <div class="btn-group">
          <button class="btn secondary" @click="goGuide()">返回绑定</button>
          <button class="btn primary" :disabled="loading" @click="recheck()">
            {{ loading ? '检测中…' : '重新检测' }}
          </button>
        </div>
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
  padding: 20px 16px;
  box-sizing: border-box;
  background: var(--color-background);
  overflow: hidden;
}

.bind-page--manual {
  padding: 0;
  align-items: center;
  justify-content: center;
}

/* 录入页白底卡片：固定宽度 + 内容撑开高度，不随画布纵向拉伸 */
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
  align-items: stretch;
  gap: 14px;
  padding: 24px 20px 20px;
  box-sizing: border-box;
  text-align: center;
  background: var(--color-card-surface);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
}

.manual-header {
  flex-shrink: 0;
  margin-bottom: 0;
}

.manual-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.manual-panel .bind-hint--compact {
  margin-bottom: 0;
}

.manual-panel .bind-input-display {
  flex-shrink: 0;
  height: 52px;
  min-height: 52px;
  margin-bottom: 0;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.manual-panel .manual-error {
  flex-shrink: 0;
  margin: 0;
}

.manual-panel .manual-error:empty {
  display: none;
}

.manual-keyboard {
  flex: 0 0 auto;
  width: 100%;
  height: auto;
  margin-bottom: 0;
}

.btn--compact {
  padding: 14px 20px;
  font-size: 16px;
}

.bind-card {
  text-align: center;
  width: 100%;
  max-width: 100%;
  padding: 32px 20px 28px;
  background: var(--color-card-surface);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  box-sizing: border-box;
}

.bind-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-glow);
}

.bind-icon svg {
  width: 32px;
  height: 32px;
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 0.5px;
}

.bind-type {
  display: inline-block;
  margin: 0 0 28px;
  padding: 4px 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-dim);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-full);
}

.bind-hint {
  color: var(--color-text-secondary);
  margin: 0 0 24px;
  font-size: 15px;
  line-height: 1.5;
}

.bind-hint--compact {
  margin-bottom: 12px;
  font-size: 13px;
}

.guide-error {
  color: var(--color-error);
  margin: -12px 0 20px;
  font-size: 14px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-lg);
}

.manual-error {
  color: var(--color-error);
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.4;
}

.bind-input-display {
  min-height: 44px;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 2px solid var(--color-card-border);
  border-radius: var(--radius-lg);
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 1px;
  word-break: break-all;
  background: var(--color-primary-dim);
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.btn {
  display: block;
  width: 100%;
  padding: 16px 20px;
  border-radius: 14px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.btn:active:not(:disabled) {
  transform: scale(0.98);
}

.btn.primary {
  background: linear-gradient(135deg, var(--color-primary-deep), var(--color-primary));
  color: var(--color-text-inverse);
  border: none;
  box-shadow: var(--shadow-glow);
}

.btn.secondary {
  background: var(--color-surface-elevated);
  color: var(--color-text-primary);
  border: 2px solid var(--color-border);
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.device-code-label {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.device-code {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  word-break: break-all;
  margin: 0 0 16px;
  padding: 12px 16px;
  background: var(--color-primary-dim);
  border: 1px solid var(--color-card-border);
  border-radius: var(--radius-lg);
}

.status-title {
  margin: 0 0 16px;
  font-weight: 700;
  font-size: 20px;
  color: var(--color-text-primary);
}

.error-msg {
  color: var(--color-error);
  margin: 0 0 16px;
  font-size: 15px;
  line-height: 1.5;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-lg);
}
</style>
