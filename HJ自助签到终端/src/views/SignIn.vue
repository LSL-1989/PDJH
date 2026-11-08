<template>
  <div class="signin-screen">
      <!-- 顶部栏 -->
      <header class="top-bar neu-raised">
        <div class="top-bar__inner">
          <div class="top-bar__brand">
            <div class="logo-circle" aria-label="医院Logo">
              <img v-if="logoSrc" :src="logoSrc" alt="" class="logo-img" />
              <svg v-else viewBox="0 0 24 24" fill="none">
                <path d="M12 6v12M6 12h12" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
              </svg>
            </div>
            <div class="brand-text">
              <span class="hospital-name">{{ hospitalName }}</span>
              <span class="hospital-tagline">科技赋能 · 智慧医疗</span>
            </div>
          </div>
          <div class="time-block">
            <svg class="time-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" />
              <path d="M12 7v5l3 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <div class="time-text">
              <span class="current-clock">{{ currentClock }}</span>
              <span class="current-date">{{ currentDate }}</span>
            </div>
          </div>
        </div>
      </header>

      <!-- 主内容 -->
      <main class="main-body" :class="{ 'input-locked': !canKeypadInput }">
        <div class="page-header">
          <div class="slogan-row">
            <span class="slogan-deco" />
            <h2 class="slogan">{{ pageSlogan }}</h2>
            <span class="slogan-deco" />
          </div>
          <p class="slogan-sub">{{ pageSloganSub }}</p>
          <p v-if="deviceError" class="area-hint area-hint--error">{{ deviceError }}</p>
        </div>

        <!-- 核心交互卡片 -->
        <section class="input-card neu-raised">
          <div class="input-box input-box--top" :class="{ focused: inputFocused }">
            <!-- 只读展示，避免弹软键盘；真实键入由隐藏框 + 全局 keydown 写入 manualCode -->
            <input
              v-model="manualCode"
              class="code-input"
              type="text"
              readonly
              tabindex="-1"
              placeholder="请扫码或输入签到码"
              maxlength="32"
            />
          </div>

          <div class="card-body">
            <!-- 左侧：扫码取景 -->
            <div class="left-panel">
              <div class="scan-viewfinder">
                <div class="viewfinder-frame">
                  <div class="viewfinder-grid" aria-hidden="true" />
                  <span class="corner corner--tl" />
                  <span class="corner corner--tr" />
                  <span class="corner corner--bl" />
                  <span class="corner corner--br" />
                  <div class="scan-line" />
                </div>
                <p class="viewfinder-hint">请将签到码放入框内自动扫描</p>
              </div>
            </div>

            <!-- 右侧：数字键盘 -->
            <div class="right-panel">
              <div class="keypad">
                <button
                  v-for="key in digitKeys"
                  :key="'d-' + key"
                  type="button"
                  class="key-btn"
                  :disabled="!canKeypadInput"
                  @click="onKeyPress(String(key))"
                >
                  {{ key }}
                </button>
                <button
                  type="button"
                  class="key-btn key-btn--icon"
                  :disabled="!canKeypadInput"
                  aria-label="删除"
                  @click="onKeyPress('del')"
                >
                  <svg viewBox="0 0 24 24" fill="none">
                    <path d="M9 6H20a1 1 0 011 1v10a1 1 0 01-1 1H9l-4-4V10l4-4z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
                    <path d="M12 10h6M12 14h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                </button>
                <button
                  type="button"
                  class="key-btn"
                  :disabled="!canKeypadInput"
                  @click="onKeyPress('0')"
                >
                  0
                </button>
                <button
                  type="button"
                  class="key-btn key-btn--icon"
                  :disabled="!canKeypadInput"
                  aria-label="清空"
                  @click="onKeyPress('clear')"
                >
                  <svg viewBox="0 0 24 24" fill="none">
                    <path d="M4 7h16M6 7v12a1 1 0 001 1h10a1 1 0 001-1V7M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M10 11v5M14 11v5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <button
            type="button"
            class="confirm-btn"
            :disabled="!canKeypadInput"
            @click="onConfirm"
          >
            确认签到
          </button>
        </section>

        <!-- 步骤指引 -->
        <section class="tips-area">
          <div v-for="(tip, i) in operationTips" :key="i" class="tip-card">
            <div class="tip-icon" :class="'tip-icon--' + (i + 1)">
              <component :is="tip.icon" />
            </div>
            <div class="tip-content">
              <p class="tip-title">{{ tip.title }}</p>
              <p class="tip-desc">{{ tip.desc }}</p>
            </div>
          </div>
        </section>
      </main>

      <!-- 底部状态栏 -->
      <footer class="status-bar">
        <span class="status-item">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <rect x="3" y="4" width="18" height="12" rx="2" stroke="currentColor" stroke-width="1.5" />
            <path d="M8 20h8M12 16v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          设备编号：{{ deviceId }}
        </span>
        <span class="status-item status-item--muted">技术支持：南京吾爱网络技术有限公司</span>
      </footer>

      <!-- 常驻焦点：扫码枪/实体键盘写入此处；失焦后自动抢回，页面任意处键入也经全局拦截汇总到同一值 -->
      <input
        ref="scannerInputRef"
        v-model="manualCode"
        class="scanner-hidden"
        autocomplete="off"
        autocorrect="off"
        spellcheck="false"
        maxlength="32"
        :disabled="!canScanInput"
        @focus="inputFocused = true"
        @blur="onScannerBlur"
        @keydown.enter.prevent="onScannerEnter"
      />

      <div v-if="isProcessing" class="overlay overlay-waiting">
        <div class="waiting-card">
          <div class="waiting-spinner" aria-hidden="true" />
          <p class="waiting-text">正在签到，请稍候...</p>
        </div>
      </div>

      <transition name="modal">
        <div v-if="resultVisible" class="overlay overlay-result" @click.self="closeResultOverlay">
          <div
            class="result-modal"
            :class="resultSuccess ? 'result-modal--success' : 'result-modal--fail'"
          >
            <template v-if="resultSuccess && resultDetail">
              <p class="result-title result-title--success">
                <svg class="result-title-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" />
                  <path d="M8 12.5l2.5 2.5L16 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                {{ resultDetail.alreadyCheckedIn ? '您已签到' : '签到成功' }}
              </p>
              <p class="result-no-label">签到序号</p>
              <p class="result-no" :class="showNoSizeClass">{{ resultDetail.showNo }}</p>
              <p class="result-line result-line--queue">
                <span>当前队列人数：<em>{{ resultDetail.queueCount }}</em></span>
                <span>排队位置：<em>{{ resultDetail.queuePosition }}</em></span>
              </p>
              <p class="result-line">{{ resultDetail.patientLine }}</p>
              <p class="result-line result-line--place">
                <span>签到诊室：{{ resultDetail.room }}</span>
                <span>签到时间：{{ resultDetail.checkInTime }}</span>
              </p>
              <p class="result-notice">{{ resultDetail.notice }}</p>
            </template>
            <template v-else>
              <div class="result-icon-wrap">
                <svg class="result-icon" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" />
                  <path d="M9 9l6 6M15 9l-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                </svg>
              </div>
              <p class="result-title">签到失败</p>
              <p class="result-message">{{ resultMessage }}</p>
            </template>
            <div class="result-countdown-bar">
              <div
                class="result-countdown-fill"
                :class="resultSuccess ? 'fill--success' : 'fill--fail'"
                :style="{ width: (resultCountdown / 5) * 100 + '%' }"
              />
            </div>
            <p class="result-countdown">{{ resultCountdown }} 秒后自动关闭</p>
          </div>
        </div>
      </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, h } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { getStoredDeviceCode, setStoredDeviceCode } from '../utils/terminalBinding'
import { resolveMediaUrl } from '../utils/resolveMediaUrl'
import { HOSPITAL_NAME, HOSPITAL_LOGO, DEVICE_ID, DEV_DEFAULT_PATIENT_CODE } from '../config/env'

const route = useRoute()

/** 与门边屏一致：环境变量兜底（默认「某某医院」） */
const envHospitalName = HOSPITAL_NAME || '某某医院'
const envHospitalLogo = HOSPITAL_LOGO || ''
/** 与门边屏一致：优先 GetDeviceConfig（sys_config.HospitalName / HospitalLogo） */
const apiHospitalName = ref('')
const apiHospitalLogo = ref('')
const hospitalName = computed(() => apiHospitalName.value.trim() || envHospitalName)
const hospitalLogo = computed(() => apiHospitalLogo.value.trim() || envHospitalLogo)
const logoSrc = computed(() => resolveMediaUrl(hospitalLogo.value))

const resolveDeviceCode = (): string => {
  const fromQuery = (route.query.deviceId || route.query.deviceCode) as string | undefined
  if (fromQuery?.trim()) {
    setStoredDeviceCode(fromQuery.trim())
    return fromQuery.trim()
  }
  const fromStorage = getStoredDeviceCode()
  if (fromStorage) return fromStorage
  const legacy = localStorage.getItem('pdjh_sign_device_id')?.trim()
  if (legacy) {
    setStoredDeviceCode(legacy)
    return legacy
  }
  const fromEnv = DEVICE_ID || ''
  return fromEnv.trim()
}

const deviceId = ref(resolveDeviceCode() || '未配置')
const deviceReady = ref(false)
const deviceError = ref('')

/** 后端在线窗口 5 分钟；心跳须短于该窗口，复用 GetDeviceConfig 刷 last_seen_at */
const HEARTBEAT_MS = 2 * 60 * 1000
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let heartbeatInFlight = false

type DeviceConfigBody = {
  success?: boolean
  data?: {
    title?: string
    showText?: string
    scene?: string
    hospitalName?: string
    hospitalLogo?: string
  }
  msg?: string
}

const pageSlogan = ref('自助签到')
const pageSloganSub = ref('请扫码或输入签到码完成签到')
const deviceScene = ref<'CLINIC' | 'PHARMACY' | 'LAB'>('CLINIC')

const applyDeviceConfig = (body: DeviceConfigBody): boolean => {
  if (!body?.success || !body.data) {
    deviceError.value = body?.msg || '设备未注册或已停用'
    deviceReady.value = false
    // 失败不改动已成功缓存的医院名/Logo
    return false
  }
  const name = (body.data.hospitalName || '').trim()
  const logo = (body.data.hospitalLogo || '').trim()
  if (name) apiHospitalName.value = name
  if (logo) apiHospitalLogo.value = logo
  const scene = (body.data.scene || '').trim().toUpperCase()
  if (scene === 'PHARMACY') deviceScene.value = 'PHARMACY'
  else if (scene === 'LAB') deviceScene.value = 'LAB'
  else deviceScene.value = 'CLINIC'
  const title = (body.data.title || '').trim()
  const showText = (body.data.showText || '').trim()
  if (title) {
    pageSlogan.value = title
  } else if (deviceScene.value === 'PHARMACY') {
    pageSlogan.value = '取药签到'
  } else if (deviceScene.value === 'LAB') {
    pageSlogan.value = '检验签到'
  } else {
    pageSlogan.value = '自助签到'
  }
  if (showText) {
    pageSloganSub.value = showText
  } else if (deviceScene.value === 'PHARMACY') {
    pageSloganSub.value = '请扫码，系统将通知药房配药'
  } else if (deviceScene.value === 'LAB') {
    pageSloganSub.value = '请扫码，系统将分配采血窗口'
  } else {
    pageSloganSub.value = '请扫码或输入签到码完成签到'
  }
  deviceReady.value = true
  deviceError.value = ''
  return true
}

const loadDeviceConfig = async () => {
  const code = deviceId.value
  if (!code || code === '未配置') {
    deviceError.value = '未配置设备编号，请在 URL 添加 ?deviceId= 或在本地设置 VITE_DEVICE_ID'
    deviceReady.value = false
    return
  }
  try {
    const res = await axios.get('/api/SignDevice/GetDeviceConfig', { params: { deviceCode: code } })
    applyDeviceConfig(res.data as DeviceConfigBody)
  } catch (err: unknown) {
    const e = err as { response?: { data?: { msg?: string } }; message?: string }
    deviceError.value = e.response?.data?.msg || e.message || '无法加载终端配置'
    deviceReady.value = false
  }
}

/**
 * 定时心跳：调用 GetDeviceConfig 刷新 last_seen_at，保持后台「已连接」。
 * 瞬时网络失败不打翻就绪态；业务失败（停用/未绑定）与首次加载一致。
 */
const sendPresenceHeartbeat = async () => {
  if (heartbeatInFlight) return
  const code = deviceId.value
  if (!code || code === '未配置') return
  heartbeatInFlight = true
  try {
    const res = await axios.get('/api/SignDevice/GetDeviceConfig', { params: { deviceCode: code } })
    applyDeviceConfig(res.data as DeviceConfigBody)
  } catch (err: unknown) {
    const e = err as { response?: { status?: number; data?: DeviceConfigBody } }
    const status = e.response?.status
    const body = e.response?.data
    if (status === 403 || status === 404 || (body && body.success === false)) {
      applyDeviceConfig(body || { success: false, msg: '设备未注册或已停用' })
    }
  } finally {
    heartbeatInFlight = false
  }
}

const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => void sendPresenceHeartbeat(), HEARTBEAT_MS)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    void sendPresenceHeartbeat()
  }
}

/** 签到码唯一缓冲：屏上数字键、扫码枪、实体键盘均写入此值 */
const manualCode = ref(DEV_DEFAULT_PATIENT_CODE || '')
const scannerInputRef = ref<HTMLInputElement | null>(null)
const inputFocused = ref(false)

const currentDate = ref('')
const currentClock = ref('')
const isProcessing = ref(false)
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultMessage = ref('')
const resultCountdown = ref(5)

/** 签到成功弹层的分行内容；失败时为空，仍走 resultMessage */
type SuccessDetail = {
  showNo: string
  queueCount: string
  queuePosition: string
  patientLine: string
  room: string
  checkInTime: string
  notice: string
  alreadyCheckedIn: boolean
}
const resultDetail = ref<SuccessDetail | null>(null)
const showNoSizeClass = computed(() => {
  const len = resultDetail.value?.showNo.length ?? 0
  if (len > 8) return 'result-no--compact'
  if (len > 5) return 'result-no--medium'
  return ''
})

const digitKeys = [1, 2, 3, 4, 5, 6, 7, 8, 9]

const IconScan = () =>
  h('svg', { viewBox: '0 0 24 24', fill: 'none' }, [
    h('path', {
      d: 'M4 7V5a1 1 0 011-1h2M4 17v2a1 1 0 001 1h2M16 4h2a1 1 0 011 1v2M16 20h2a1 1 0 001-1v-2',
      stroke: 'currentColor',
      'stroke-width': '1.5',
      'stroke-linecap': 'round',
    }),
    h('rect', { x: '7', y: '7', width: '10', height: '10', rx: '1', stroke: 'currentColor', 'stroke-width': '1.5' }),
  ])

const IconConfirm = () =>
  h('svg', { viewBox: '0 0 24 24', fill: 'none' }, [
    h('circle', { cx: '12', cy: '12', r: '9', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('path', {
      d: 'M9 12l2 2 4-4',
      stroke: 'currentColor',
      'stroke-width': '1.5',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    }),
  ])

const IconScreen = () =>
  h('svg', { viewBox: '0 0 24 24', fill: 'none' }, [
    h('rect', { x: '3', y: '5', width: '18', height: '12', rx: '2', stroke: 'currentColor', 'stroke-width': '1.5' }),
    h('path', {
      d: 'M8 21h8M12 17v4',
      stroke: 'currentColor',
      'stroke-width': '1.5',
      'stroke-linecap': 'round',
    }),
  ])

const clinicTips = [
  { icon: IconScan, title: '1. 扫码', desc: '将签到码放入扫描框自动识别' },
  { icon: IconConfirm, title: '2. 确认', desc: '核对信息无误后完成签到' },
  { icon: IconScreen, title: '3. 等待就诊', desc: '签到成功后在诊室外等候叫号' },
]
const pharmacyTips = [
  { icon: IconScan, title: '1. 扫码', desc: '扫描就诊卡或处方条码' },
  { icon: IconConfirm, title: '2. 确认', desc: '系统核验可取药处方并分配窗口' },
  { icon: IconScreen, title: '3. 等候取药', desc: '配药完成后请留意大屏叫号' },
]
const labTips = [
  { icon: IconScan, title: '1. 扫码', desc: '扫描就诊卡或检验条码' },
  { icon: IconConfirm, title: '2. 确认', desc: '系统核验项目并分配采血窗口' },
  { icon: IconScreen, title: '3. 等候采血', desc: '请到指定窗口等候叫号' },
]
const operationTips = computed(() => {
  if (deviceScene.value === 'PHARMACY') return pharmacyTips
  if (deviceScene.value === 'LAB') return labTips
  return clinicTips
})

/** 查询中 / 结果弹层展示期间禁止一切输入；关闭结果后恢复 */
const canAcceptInput = computed(
  () => deviceReady.value && !isProcessing.value && !resultVisible.value
)
const canKeypadInput = canAcceptInput
const canScanInput = canAcceptInput

watch(canScanInput, (ok) => {
  if (ok) nextTick(() => refocusScanner())
})

let clockTimer: ReturnType<typeof setInterval> | null = null
let resultTimer: ReturnType<typeof setInterval> | null = null
let resultCloseTimer: ReturnType<typeof setTimeout> | null = null
let refocusTimer: ReturnType<typeof setTimeout> | null = null

const pad = (n: number) => String(n).padStart(2, '0')

const updateClock = () => {
  const now = new Date()
  currentDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  currentClock.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const parsePatientCodeFromScan = (raw: string): string => {
  const text = raw.trim()
  if (!text) return ''
  try {
    const url = new URL(text, window.location.origin)
    const q = url.searchParams.get('patientCode')
    if (q) return q.trim()
  } catch {
    /* 纯编号 */
  }
  return text
}

const onKeyPress = (key: string) => {
  if (!canKeypadInput.value) return
  if (key === 'clear') {
    manualCode.value = ''
  } else if (key === 'del') {
    manualCode.value = manualCode.value.slice(0, -1)
  } else if (manualCode.value.length < 32) {
    manualCode.value += key
  }
  refocusScanner()
}

/** 后端 CallList / ResultData 字段为 PascalCase 或 msg */
const apiMessage = (body: { msg?: string; message?: string } | undefined, fallback = '') =>
  (body?.msg || body?.message || fallback).trim()

const pickCallListField = (info: Record<string, unknown>, ...keys: string[]): string => {
  for (const key of keys) {
    const v = info[key]
    if (v != null && String(v).trim()) return String(v).trim()
  }
  return ''
}

/** 弹层上的签到时刻，与顶栏时钟同为 HH:mm:ss */
const formatCheckInTime = (date = new Date()) => {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`
}

/** 一行用户信息 + 患者 ID；extra 为性别、年龄等补充，没有则省略 */
const buildPatientLine = (name: string, patientId: string, extra = '') => {
  const who = [name.trim(), extra.trim()].filter(Boolean).join(' ')
  const idPart = patientId.trim() ? `患者ID：${patientId.trim()}` : ''
  return [who, idPart].filter(Boolean).join('　') || '—'
}

/**
 * 签到后注意事项。
 * 门诊 CheckIn 原文是内部放号说明，不展示给患者；药房/检验用接口文案去掉「签到成功」前缀。
 */
const noticeForScene = (scene: 'CLINIC' | 'PHARMACY' | 'LAB', backendMessage?: string) => {
  const stripped = (backendMessage || '').trim().replace(/^签到成功[，,]\s*/, '')
  if (stripped && !stripped.includes('准予放号') && !stripped.includes('实体签到')) {
    return stripped
  }
  if (scene === 'PHARMACY') return '配药完成后请留意叫号'
  if (scene === 'LAB') return '请到指定窗口等候，留意采血叫号'
  return '请在诊室外等候，留意大屏幕叫号'
}

const formatQueueCount = (value?: number | null) =>
  value != null && value >= 0 ? String(value) : '—'

/** 排队位置从 1 起；0 表示当前不在等候队列 */
const formatQueuePosition = (value?: number | null) =>
  value != null && value > 0 ? String(value) : '—'

/** 把签到结果拆成弹层：序号、队列、患者、诊室与时间、注意事项 */
const buildSuccessDetail = (
  scene: 'CLINIC' | 'PHARMACY' | 'LAB',
  fields: {
    showNo?: string
    patientName?: string
    patientId?: string
    extra?: string
    room?: string
    notice?: string
    queueCount?: number | null
    queuePosition?: number | null
    checkInTime?: string
    alreadyCheckedIn?: boolean
  }
): SuccessDetail => ({
  showNo: fields.showNo?.trim() || '—',
  queueCount: formatQueueCount(fields.queueCount),
  queuePosition: formatQueuePosition(fields.queuePosition),
  patientLine: buildPatientLine(fields.patientName || '', fields.patientId || '', fields.extra || ''),
  room: fields.room?.trim() || '—',
  checkInTime: fields.checkInTime?.trim() || formatCheckInTime(),
  notice: noticeForScene(scene, fields.notice),
  alreadyCheckedIn: !!fields.alreadyCheckedIn,
})

const clearResultTimers = () => {
  if (resultTimer) {
    clearInterval(resultTimer)
    resultTimer = null
  }
  if (resultCloseTimer) {
    clearTimeout(resultCloseTimer)
    resultCloseTimer = null
  }
}

const closeResultOverlay = () => {
  clearResultTimers()
  resultVisible.value = false
  resultDetail.value = null
  manualCode.value = ''
  nextTick(() => refocusScanner())
}

/** 展示结果并清空输入；成功时 detail 驱动分行，失败只用 message。弹层关闭前禁止再次输入 */
const showResult = (success: boolean, message: string, detail: SuccessDetail | null = null) => {
  clearResultTimers()
  manualCode.value = ''
  resultSuccess.value = success
  resultMessage.value = message
  resultDetail.value = success ? detail : null
  resultCountdown.value = 5
  resultVisible.value = true

  resultTimer = setInterval(() => {
    resultCountdown.value -= 1
    if (resultCountdown.value <= 0 && resultTimer) {
      clearInterval(resultTimer)
      resultTimer = null
    }
  }, 1000)

  resultCloseTimer = setTimeout(() => {
    closeResultOverlay()
  }, 5000)
}

const performSignIn = async (code: string) => {
  if (isProcessing.value || resultVisible.value) return
  if (!deviceReady.value) {
    showResult(false, deviceError.value || '终端未就绪，无法签到')
    return
  }

  const trimmed = parsePatientCodeFromScan(code)
  if (!trimmed) {
    showResult(false, '请输入或扫描有效的患者编号')
    return
  }

  manualCode.value = trimmed
  isProcessing.value = true

  try {
    if (deviceScene.value === 'PHARMACY' || deviceScene.value === 'LAB') {
      const sceneUrl =
        deviceScene.value === 'PHARMACY' ? '/api/queue/pharmacyCheckIn' : '/api/queue/labCheckIn'
      const phRes = await axios.post(sceneUrl, {
        patientCode: trimmed,
        deviceCode: deviceId.value,
      })
      const phBody = phRes.data as {
        success?: boolean
        data?: {
          showNo?: string
          officeRoomName?: string
          patientName?: string
          patientCode?: string
          message?: string
          alreadyCheckedIn?: boolean
          checkInTime?: string
          queueCount?: number
          queuePosition?: number
        }
        msg?: string
        message?: string
      }
      if (!phBody?.success) {
        showResult(false, apiMessage(phBody, deviceScene.value === 'LAB' ? '检验签到失败' : '取药签到失败'))
        return
      }
      const d = phBody.data
      showResult(
        true,
        '',
        buildSuccessDetail(deviceScene.value, {
          showNo: d?.showNo,
          patientName: d?.patientName,
          patientId: d?.patientCode || trimmed,
          room: d?.officeRoomName,
          notice: d?.message,
          queueCount: d?.queueCount,
          queuePosition: d?.queuePosition,
          checkInTime: d?.checkInTime,
          alreadyCheckedIn: d?.alreadyCheckedIn,
        })
      )
      return
    }

    const infoRes = await axios.post('/api/queue/getSignInInfo', {
      patientCode: trimmed,
      deviceCode: deviceId.value,
    })
    const infoBody = infoRes.data as { success?: boolean; data?: Record<string, unknown>; msg?: string; message?: string }
    if (!infoBody?.success) {
      showResult(false, apiMessage(infoBody, '未找到待签到信息，或您已签到过了'))
      return
    }

    const info = infoBody.data
    if (!info?.id) {
      showResult(false, '签到记录无效，请联系工作人员')
      return
    }

    const checkRes = await axios.post('/api/Triage/CheckIn', {
      callListId: info.id,
      deviceCode: deviceId.value,
    })

    if (checkRes.status === 200) {
      const sex = pickCallListField(info, 'sex', 'Sex')
      const age = pickCallListField(info, 'ageStr', 'AgeStr')
      const checkBody = checkRes.data as {
        success?: boolean
        data?: {
          showNo?: string
          patientName?: string
          patientCode?: string
          officeRoomName?: string
          checkInTime?: string
          queueCount?: number
          queuePosition?: number
          alreadyCheckedIn?: boolean
          message?: string
        }
        msg?: string
        message?: string
      }
      if (checkBody && typeof checkBody === 'object' && checkBody.success === false) {
        showResult(false, apiMessage(checkBody, '签到出现异常，请稍后重试'))
        return
      }
      const signed = checkBody && typeof checkBody === 'object' ? checkBody.data : undefined
      showResult(
        true,
        '',
        buildSuccessDetail('CLINIC', {
          showNo: signed?.showNo || pickCallListField(info, 'ShowNo', 'showNo'),
          patientName: signed?.patientName || pickCallListField(info, 'PatientName', 'patientName'),
          patientId: signed?.patientCode || pickCallListField(info, 'PatientCode', 'patientCode') || trimmed,
          extra: [sex, age].filter(Boolean).join(' '),
          room: signed?.officeRoomName || pickCallListField(info, 'OutPatientOffice', 'outPatientOffice'),
          notice: signed?.message,
          queueCount: signed?.queueCount,
          queuePosition: signed?.queuePosition,
          checkInTime: signed?.checkInTime,
          alreadyCheckedIn: signed?.alreadyCheckedIn,
        })
      )
    } else {
      const failBody = checkRes.data as { msg?: string; message?: string } | string | undefined
      showResult(
        false,
        typeof failBody === 'string' ? failBody : apiMessage(failBody, '签到出现异常，请稍后重试')
      )
    }
  } catch (err: unknown) {
    const e = err as { response?: { data?: { msg?: string; message?: string } | string }; message?: string }
    const data = e.response?.data
    const msg =
      (typeof data === 'object' ? apiMessage(data) : '') ||
      (typeof data === 'string' ? data : '') ||
      e.message ||
      '网络不稳定，请重试'
    showResult(false, msg)
  } finally {
    isProcessing.value = false
  }
}

const onConfirm = () => {
  if (!canKeypadInput.value) return
  void performSignIn(manualCode.value)
}

const onScannerEnter = () => {
  if (!canScanInput.value) return
  const raw = manualCode.value.trim()
  if (!raw) return
  void performSignIn(raw)
}

const onScannerBlur = () => {
  inputFocused.value = false
  refocusScanner()
}

/**
 * 无论焦点在按钮/页面其它处，实体键盘与扫码枪输入都写入 manualCode。
 * 已聚焦隐藏框时交给原生 input，避免与 v-model 重复追加。
 */
const onGlobalKeydown = (e: KeyboardEvent) => {
  if (!canScanInput.value) return

  const focusedOnScanner = document.activeElement === scannerInputRef.value

  if (e.key === 'Enter') {
    if (focusedOnScanner) return
    e.preventDefault()
    onScannerEnter()
    return
  }

  if (!canKeypadInput.value) return
  if (focusedOnScanner) return

  if (e.key === 'Backspace') {
    e.preventDefault()
    manualCode.value = manualCode.value.slice(0, -1)
    refocusScanner()
    return
  }

  if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
    e.preventDefault()
    if (manualCode.value.length < 32) {
      manualCode.value += e.key
    }
    refocusScanner()
  }
}

const refocusScanner = () => {
  if (!canScanInput.value) return
  if (refocusTimer) clearTimeout(refocusTimer)
  refocusTimer = setTimeout(() => {
    scannerInputRef.value?.focus({ preventScroll: true })
  }, 50)
}

onMounted(() => {
  void loadDeviceConfig()
  startHeartbeat()
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('keydown', onGlobalKeydown, true)
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  nextTick(() => refocusScanner())

  const raw = route.query.patientCode
  const fromQuery = Array.isArray(raw)
    ? String(raw[0] ?? '').trim()
    : typeof raw === 'string'
      ? raw.trim()
      : ''
  if (fromQuery) {
    void performSignIn(fromQuery)
  }
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  stopHeartbeat()
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('keydown', onGlobalKeydown, true)
  clearResultTimers()
  if (refocusTimer) clearTimeout(refocusTimer)
})
</script>

<style scoped>
.signin-screen {
  width: 100%;
  height: 100%;
  background: transparent;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  isolation: isolate;
  font-family: var(--neu-font);
  color: var(--neu-text);
}

/* ── 顶部栏 ── */
.top-bar {
  flex-shrink: 0;
  margin: 14px 14px 0;
  border-radius: 20px;
}

.top-bar__inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.top-bar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(45, 212, 191, 0.12);
  border: 1px solid rgba(45, 212, 191, 0.3);
  color: var(--neu-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--neu-raised);
  flex-shrink: 0;
  overflow: hidden;
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-circle svg {
  width: 24px;
  height: 24px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hospital-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--neu-text);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.hospital-tagline {
  font-size: 10px;
  font-weight: 500;
  color: var(--neu-muted);
  letter-spacing: 0;
}

.time-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 14px;
  background: rgba(8, 16, 28, 0.45);
  box-shadow: var(--neu-inset);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.time-icon {
  width: 20px;
  height: 20px;
  color: var(--neu-accent);
  flex-shrink: 0;
}

.time-text {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.current-date {
  font-size: 10px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--neu-muted);
  white-space: nowrap;
  line-height: 1.2;
}

.current-clock {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--neu-highlight);
  white-space: nowrap;
  line-height: 1.1;
  letter-spacing: -0.01em;
}

/* ── 主内容 ── */
.main-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 14px 14px 4px;
  min-height: 0;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}

.main-body.input-locked .input-card {
  opacity: 0.45;
  pointer-events: none;
}

.page-header {
  text-align: center;
  margin-bottom: 16px;
  padding-top: 4px;
}

.slogan-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.slogan-deco {
  flex: 1;
  max-width: 48px;
  height: 2px;
  border-radius: 1px;
  background: var(--neu-bg);
  box-shadow: var(--neu-inset);
}

.slogan {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--neu-text);
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.slogan-sub {
  margin: 8px 0 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--neu-muted);
}

.area-hint {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--neu-accent);
  font-weight: 500;
}

.area-hint--error {
  color: var(--neu-error);
}

/* ── 核心卡片 ── */
.input-card {
  border-radius: 24px;
  padding: 20px;
  margin-bottom: 16px;
  transition: opacity 0.25s ease;
}

.input-box--top {
  margin-bottom: 14px;
}

.input-box--top .code-input {
  height: 52px;
  font-size: 18px;
}

/* ── 左右分栏（margin 兜底：旧 WebView 无 flex gap） ── */
.card-body {
  display: -webkit-flex;
  display: flex;
  margin-bottom: 14px;
  -webkit-align-items: stretch;
  align-items: stretch;
}

.left-panel {
  flex: 0 0 42%;
  -webkit-flex: 0 0 42%;
  min-width: 0;
  margin-right: 12px;
  display: -webkit-flex;
  display: flex;
  -webkit-flex-direction: column;
  flex-direction: column;
}

.right-panel {
  flex: 1;
  -webkit-flex: 1;
  min-width: 0;
  display: -webkit-flex;
  display: flex;
  -webkit-align-items: center;
  align-items: center;
}

/* ── 扫码取景框（凹陷） ── */
/* 工业平板安卓 WebView 常缺 aspect-ratio/inset，取景框会高度塌 0、遮罩无法铺满 */
.scan-viewfinder {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.viewfinder-frame {
  position: relative;
  width: 100%;
  flex: 1 1 auto;
  min-height: 140px;
  border-radius: 14px;
  background: var(--neu-bg);
  box-shadow: var(--neu-inset);
  overflow: hidden;
}

.viewfinder-grid {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(166, 180, 200, 0.15) 1px, transparent 1px),
    linear-gradient(90deg, rgba(166, 180, 200, 0.15) 1px, transparent 1px);
  background-size: 12px 12px;
}

.corner {
  position: absolute;
  width: 18px;
  height: 18px;
  border-color: var(--neu-accent);
  border-style: solid;
  z-index: 2;
  opacity: 0.85;
}

.corner--tl {
  top: 10px;
  left: 10px;
  border-width: 2px 0 0 2px;
}

.corner--tr {
  top: 10px;
  right: 10px;
  border-width: 2px 2px 0 0;
}

.corner--bl {
  bottom: 10px;
  left: 10px;
  border-width: 0 0 2px 2px;
}

.corner--br {
  bottom: 10px;
  right: 10px;
  border-width: 0 2px 2px 0;
}

.scan-line {
  position: absolute;
  left: 12px;
  right: 12px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--neu-accent), transparent);
  opacity: 0.7;
  z-index: 2;
  animation: scanMove 2.4s ease-in-out infinite;
}

@keyframes scanMove {
  0%,
  100% {
    top: 12%;
    opacity: 0.4;
  }
  50% {
    top: 88%;
    opacity: 0.9;
  }
}

.viewfinder-hint {
  margin: 10px 0 0;
  font-size: 10px;
  font-weight: 500;
  color: var(--neu-placeholder);
  line-height: 1.5;
  text-align: center;
}

/* ── 输入框（凹陷） ── */
.input-box {
  border-radius: 14px;
  transition: box-shadow 0.2s ease;
}

.input-box.focused {
  box-shadow: var(--neu-inset-focus);
  border-radius: 14px;
}

.code-input {
  width: 100%;
  box-sizing: border-box;
  height: 44px;
  padding: 0 14px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  text-align: center;
  color: var(--neu-highlight);
  background: rgba(8, 16, 28, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 14px;
  outline: none;
  box-shadow: var(--neu-inset);
  font-family: var(--neu-font);
  transition: box-shadow 0.2s ease;
}

.input-box.focused .code-input {
  box-shadow: var(--neu-inset-focus);
}

.code-input::placeholder {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0;
  color: var(--neu-placeholder);
}

/* ── 数字键盘（凸起按钮） ── */
.keypad {
  display: -ms-grid;
  display: grid;
  -ms-grid-columns: 1fr 8px 1fr 8px 1fr;
  grid-template-columns: repeat(3, 1fr);
  grid-gap: 8px;
  gap: 8px;
  width: 100%;
}

.key-btn {
  height: 40px;
  border: 1px solid var(--neu-border);
  border-radius: 14px;
  font-size: 17px;
  font-weight: 700;
  color: var(--neu-highlight);
  cursor: pointer;
  user-select: none;
  background: var(--neu-surface);
  backdrop-filter: blur(var(--glass-blur, 16px));
  box-shadow: var(--neu-raised);
  font-family: var(--neu-font);
  transition: box-shadow 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.key-btn:hover:not(:disabled) {
  box-shadow: var(--neu-raised-hover);
}

.key-btn:active:not(:disabled) {
  box-shadow: var(--neu-inset);
  color: var(--neu-accent-dark);
}

.key-btn--icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--neu-accent);
}

.key-btn--icon svg {
  width: 20px;
  height: 20px;
}

.key-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* ── 确认按钮（主按钮凸起） ── */
.confirm-btn {
  width: 100%;
  height: 54px;
  border: 1px solid rgba(45, 212, 191, 0.35);
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--neu-highlight);
  cursor: pointer;
  background: rgba(45, 212, 191, 0.14);
  backdrop-filter: blur(var(--glass-blur, 16px));
  box-shadow: var(--neu-raised);
  font-family: var(--neu-font);
  transition: box-shadow 0.15s ease, color 0.15s ease, background 0.15s ease, border-color 0.15s ease;
}

.confirm-btn:hover:not(:disabled) {
  box-shadow: var(--neu-raised-hover);
  background: rgba(45, 212, 191, 0.22);
  border-color: rgba(45, 212, 191, 0.5);
}

.confirm-btn:active:not(:disabled) {
  box-shadow: var(--neu-inset);
  color: var(--neu-accent-dark);
}

.confirm-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* ── 步骤指引卡片（横向三列） ── */
.tips-area {
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  gap: 8px;
  padding-bottom: 4px;
}

.tip-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  padding: 10px 6px;
  background: var(--neu-surface);
  backdrop-filter: blur(var(--glass-blur, 16px));
  border-radius: 16px;
  box-shadow: var(--neu-raised);
  border: 1px solid var(--neu-border);
  box-sizing: border-box;
  transition: box-shadow 0.15s ease;
}

.tip-card:active {
  box-shadow: var(--neu-inset);
}

.tip-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 212, 191, 0.12);
  border: 1px solid rgba(45, 212, 191, 0.28);
  color: var(--neu-accent);
  box-shadow: var(--neu-raised);
}

.tip-icon svg {
  width: 18px;
  height: 18px;
}

.tip-content {
  flex: 1;
  min-width: 0;
  width: 100%;
}

.tip-title {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--neu-text);
  line-height: 1.3;
}

.tip-desc {
  margin: 4px 0 0;
  font-size: 10px;
  font-weight: 500;
  color: var(--neu-muted);
  line-height: 1.4;
}

/* ── 底部状态栏 ── */
.status-bar {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 12px;
  margin-top: 4px;
}

.status-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: var(--neu-bg);
  box-shadow: -2px -2px 4px rgba(255, 255, 255, 0.9), 2px 2px 4px rgba(166, 180, 200, 0.35);
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 500;
  color: var(--neu-placeholder);
  min-width: 0;
}

.status-item--muted {
  flex-shrink: 1;
  text-align: right;
  opacity: 0.85;
}

.status-item svg {
  width: 12px;
  height: 12px;
  color: var(--neu-accent);
}

/* ── 扫码隐藏输入 ── */
.scanner-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
  left: -9999px;
}

/* ── 遮罩层（top/right/bottom/left，避免 inset 在旧 WebView 失效导致不居中） ── */
.overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: -webkit-flex;
  display: flex;
  -webkit-align-items: center;
  align-items: center;
  -webkit-justify-content: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.overlay-waiting {
  background: rgba(11, 21, 32, 0.72);
}

@supports ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
  .overlay-waiting {
    -webkit-backdrop-filter: blur(6px);
    backdrop-filter: blur(6px);
  }
}

.waiting-card {
  display: -webkit-flex;
  display: flex;
  -webkit-flex-direction: column;
  flex-direction: column;
  -webkit-align-items: center;
  align-items: center;
  padding: 32px 40px;
  margin: 0 auto;
  background: var(--neu-bg);
  border-radius: 24px;
  box-shadow: var(--neu-raised);
  box-sizing: border-box;
}

.waiting-spinner {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 3px solid rgba(107, 127, 227, 0.25);
  border-top-color: #6b7fe3;
  animation: waiting-spin 0.8s linear infinite;
}

@keyframes waiting-spin {
  to {
    transform: rotate(360deg);
  }
}

.waiting-text {
  margin: 16px 0 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--neu-accent);
}

.overlay-result {
  padding: 12px;
  background: rgba(11, 21, 32, 0.55);
}

@supports ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
  .overlay-result {
    -webkit-backdrop-filter: blur(4px);
    backdrop-filter: blur(4px);
  }
}

.result-modal {
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
  border-radius: 24px;
  padding: 32px 24px 24px;
  text-align: center;
  background: var(--neu-bg);
  box-shadow: var(--neu-raised);
  border: none;
  box-sizing: border-box;
}

.result-icon-wrap {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--neu-bg);
  box-shadow: var(--neu-raised);
}

.result-modal--success {
  max-width: none;
  width: 100%;
  padding: 28px 18px 20px;
}

.result-modal--fail .result-icon-wrap {
  color: var(--neu-error);
}

.result-title-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.result-no-label {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.28em;
  color: var(--neu-muted);
}

.result-no {
  margin: 2px 0 8px;
  font-size: 80px;
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: 0.04em;
  color: var(--neu-highlight);
  text-shadow: 0 0 22px rgba(45, 212, 191, 0.4);
}

.result-no--medium {
  font-size: 56px;
}

.result-no--compact {
  font-size: 40px;
  letter-spacing: 0.02em;
}

.result-line {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--neu-text);
}

.result-line--queue {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 0 16px;
  margin-top: 0;
  font-size: 16px;
}

.result-line--queue em {
  font-style: normal;
  font-weight: 800;
  font-size: 22px;
  color: var(--neu-highlight);
}

.result-line--place {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 0 12px;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.result-notice {
  margin: 12px 0 0;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.55;
  color: var(--neu-muted);
}

.result-icon {
  width: 36px;
  height: 36px;
}

.result-title {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--neu-text);
}

.result-modal--success .result-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--neu-success);
  font-size: 28px;
}

.result-message {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-line;
  font-weight: 500;
  color: var(--neu-muted);
}

.result-countdown-bar {
  margin: 20px 0 8px;
  height: 10px;
  background: var(--neu-bg);
  border-radius: 5px;
  box-shadow: var(--neu-inset);
  overflow: hidden;
}

.result-countdown-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 1s linear;
}

.fill--success {
  background: linear-gradient(90deg, var(--neu-success), #6ee7b7);
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.35);
}

.fill--fail {
  background: linear-gradient(90deg, var(--neu-error), #fca5a5);
  box-shadow: 0 0 8px rgba(248, 113, 113, 0.3);
}

.result-countdown {
  margin: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--neu-placeholder);
}

.modal-enter-active {
  transition: opacity 0.35s ease;
}

.modal-enter-active .result-modal {
  animation: modalIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-leave-active {
  transition: opacity 0.25s ease;
}

.modal-leave-active .result-modal {
  animation: modalOut 0.25s ease forwards;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes modalOut {
  to {
    opacity: 0;
    transform: scale(0.92) translateY(10px);
  }
}
</style>
