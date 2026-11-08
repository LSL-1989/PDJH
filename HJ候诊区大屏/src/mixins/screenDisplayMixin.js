/**
 * 候诊区大屏 Header/Main 共用：读取 contentConfig 并驱动字幕、脱敏、布局等展示
 */
import {
  parseContentConfig,
  formatPatientName,
  resolveMarqueeText,
  isMarqueeVisible,
  isVoiceEnabled,
  showDisplayField,
  applyLayoutToStaff
} from '@/utils/screenContentConfig'

export default {
  inject: {
    screenCfgProvider: { from: 'screenCfg', default: null }
  },
  computed: {
    screenContentCfg() {
      const fromParent = this.screenCfgProvider ? this.screenCfgProvider() : null
      const fromScreens =
        this.ScreensInput &&
        (this.ScreensInput.contentConfig || this.ScreensInput.ContentConfig)
      return parseContentConfig(fromParent || fromScreens)
    },
    screenMarqueeText() {
      return resolveMarqueeText(this.screenContentCfg, this.ScreensInput)
    },
    showMarqueeBar() {
      return isMarqueeVisible(this.screenContentCfg)
    },
    screenLayoutClass() {
      return `screen-layout-${(this.screenContentCfg.layout || '2COL').toLowerCase()}`
    }
  },
  methods: {
    formatPatientName(name) {
      return formatPatientName(name, this.screenContentCfg)
    },
    hideInsurantName(name) {
      return this.formatPatientName(name)
    },
    showField(field) {
      return showDisplayField(field, this.screenContentCfg)
    },
    screenVoiceEnabled() {
      return isVoiceEnabled(this.screenContentCfg, this.ScreensInput)
    },
    applyScreenLayoutPages() {
      applyLayoutToStaff(this.screenContentCfg, this)
    }
  },
  watch: {
    screenContentCfg: {
      handler() {
        this.applyScreenLayoutPages()
      },
      deep: true,
      immediate: true
    },
    ScreensInput() {
      this.applyScreenLayoutPages()
    }
  }
}
