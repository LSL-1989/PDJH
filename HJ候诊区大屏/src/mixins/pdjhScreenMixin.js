/**
 * 候诊区各 pdjh 模板入口 mixin：启动时 GetScreensInput 并解析 contentConfig；
 * 订阅叫号广播，驱动 CallPopup 放大高亮展示被叫患者。
 */
import {
  parseContentConfig,
  buildScreenThemeClasses,
  applyScreenThemeVars,
  formatPatientName,
  isVoiceEnabled
} from '@/utils/screenContentConfig'
import { normalizeScreensInput, resolveScreenIdFromRoute, parseScreenBindingList } from '@/utils/screenBinding'
import {
  shouldSpeakOnThisScreen,
  speakCallText,
  resolveCallTimes,
  resolveSpeakRate
} from '@/utils/screenVoice'
import CallPopup from '@/components/CallPopup.vue'
import { isScreenPreview } from '@/utils/screenPreview'

/** 冻结态 state=0，与 TerminalConstants.SCREEN_STATE_FROZEN 一致 */
const SCREEN_STATE_FROZEN = 0

/** 叫号弹窗停留时长（毫秒） */
const CALL_POPUP_MS = 10000

export default {
  components: { CallPopup },
  data() {
    return {
      screenCfg: null,
      ScreensInput: {
        ID: resolveScreenIdFromRoute(this.$route)
      },
      DepartmentInput: {
        ID: this.$route.query.DepartmentID == undefined ? '' : this.$route.query.DepartmentID
      },
      callPopupVisible: false,
      callPopupShowNo: '',
      callPopupPatientName: '',
      callPopupOffice: '',
      callPopupCallText: '',
      _callPopupTimer: null,
      _callPopupHandler: null
    }
  },
  computed: {
    screenThemeClasses() {
      return buildScreenThemeClasses(this.screenCfg)
    }
  },
  provide() {
    return {
      screenCfg: () => this.screenCfg
    }
  },
  methods: {
    /** 拉取屏幕基础信息 + contentConfig（SCREEN_CFG_*）；冻结时跳转绑定页 */
    GetScreensInput() {
      this.$axios
        .post('/api/Screens/GetScreensInput', {
          ScreensInput: this.ScreensInput,
          DepartmentInput: this.DepartmentInput
        })
        .then((res) => {
          const data = res.data && res.data.data != null ? res.data.data : res.data
          const normalized = normalizeScreensInput(data)
          const frozen = normalized.state === SCREEN_STATE_FROZEN || normalized.State === SCREEN_STATE_FROZEN
          if (frozen && !isScreenPreview(this.$route)) {
            this.$router.replace({ path: '/bind', query: { frozen: '1' } })
            return
          }
          this.ScreensInput = normalized
          this.screenCfg = parseContentConfig(
            normalized.contentConfig || normalized.ContentConfig
          )
          this.$nextTick(() => {
            applyScreenThemeVars(this.screenCfg, this.$el)
          })
        })
        .catch((err) => {
          console.error('[Screen] GetScreensInput failed', err)
        })
    },
    /**
     * 判断叫号是否属于本屏：CallCode / 屏幕 ID / 绑定诊室名 任一匹配。
     * 兼容后端 CALL_MSG 与旧版 showMsg 消息。
     */
    isCallForThisScreen(msg) {
      if (!msg || typeof msg !== 'object') return false
      if (msg.type === 'STATE_UPDATE') return false
      const isCall =
        msg.type === 'CALL_MSG' ||
        !!(msg.callText || msg.showMsg || msg.patientName || msg.PatientName)
      if (!isCall) return false

      const code = String(msg.code || msg.Code || '').trim()
      const callCode = String(this.ScreensInput.CallCode || this.ScreensInput.callCode || '').trim()
      const screenId = String(this.ScreensInput.ID || this.ScreensInput.id || '').trim()
      if (code && (code === callCode || code === screenId)) return true

      const rooms = parseScreenBindingList(this.ScreensInput)
      if (rooms.length) {
        const office = String(msg.office || msg.Office || '').trim()
        const roomId = String(msg.officeRoomId || msg.OfficeRoomId || '').trim()
        if (rooms.some((r) => (roomId && r.Code === roomId) || (office && r.DeptName === office) || (code && r.Code === code))) {
          return true
        }
      }
      // 无绑定诊室时：仅靠 CallCode/屏幕 ID；避免全院误弹
      return false
    },
    showCallPopup(msg) {
      const rawName = msg.patientName || msg.PatientName || ''
      this.callPopupShowNo = msg.showNo || msg.ShowNo || ''
      this.callPopupPatientName = formatPatientName(rawName, this.screenCfg)
      this.callPopupOffice = msg.office || msg.Office || ''
      this.callPopupCallText = msg.callText || msg.showMsg || ''
      this.callPopupVisible = true
      if (this._callPopupTimer) clearTimeout(this._callPopupTimer)
      this._callPopupTimer = setTimeout(() => this.hideCallPopup(), CALL_POPUP_MS)
      this.tryScreenSpeak(msg)
    },
    tryScreenSpeak(msg) {
      if (isScreenPreview(this.$route)) return
      if (!isVoiceEnabled(this.screenCfg, this.ScreensInput)) return
      if (!shouldSpeakOnThisScreen(msg, this.ScreensInput)) return
      const text = msg.ttsCallText || msg.callText || msg.showMsg || ''
      // 优先用广播里的 callNum/rate（与库内最新配置一致），否则回落屏幕本地配置
      const times = resolveCallTimes({
        CallNum: msg.callNum ?? msg.CallNum ?? this.ScreensInput?.CallNum ?? this.ScreensInput?.callNum
      })
      const rate = resolveSpeakRate({
        Rate: msg.rate ?? msg.Rate ?? this.ScreensInput?.Rate ?? this.ScreensInput?.rate
      })
      speakCallText(text, times, rate)
    },
    hideCallPopup() {
      this.callPopupVisible = false
      if (this._callPopupTimer) {
        clearTimeout(this._callPopupTimer)
        this._callPopupTimer = null
      }
    },
    onHallCallMessage(raw) {
      let msg = raw
      if (typeof raw === 'string') {
        try {
          msg = JSON.parse(raw)
        } catch (e) {
          return
        }
      }
      if (!this.isCallForThisScreen(msg)) return
      this.showCallPopup(msg)
    },
    bindCallPopupListener() {
      if (!this.$proxy || this._callPopupHandler) return
      this._callPopupHandler = (raw) => this.onHallCallMessage(raw)
      this.$proxy.on('addMessageToList', this._callPopupHandler)
    }
  },
  mounted() {
    this.GetScreensInput()
    this.bindCallPopupListener()
  },
  beforeUnmount() {
    this.hideCallPopup()
    this._callPopupHandler = null
  }
}
