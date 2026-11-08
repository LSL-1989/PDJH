/**
 * 诊室门边屏只读预览（管理后台「预览效果」新开页）
 * 消费者：main.ts 首屏安装、路由守卫跳过 Resolve、GetDisplay 带 preview=true
 * 依赖：地址栏 hash 查询参数 preview=1；不写本地设备编号，不刷新 lastSeenAt
 */

const PREVIEW_FLAG = '__CLINIC_PREVIEW__'

type PreviewWindow = Window & { __CLINIC_PREVIEW__?: boolean }

/** 当前窗口是否为只读预览。首屏写入 window 标记，避免后续丢掉 query 后误走绑定。 */
export function isClinicPreview(): boolean {
  return typeof window !== 'undefined' && !!(window as PreviewWindow)[PREVIEW_FLAG]
}

/**
 * 根据地址栏 hash 标记预览窗，并关掉本页叫号声音。
 * 不改 localStorage 设备编号，避免把管理端浏览器登记成现场门边屏。
 */
export function installClinicPreviewGuard(): void {
  if (typeof window === 'undefined') return
  const hash = window.location.hash || ''
  const queryIndex = hash.indexOf('?')
  const params = new URLSearchParams(queryIndex >= 0 ? hash.slice(queryIndex + 1) : '')
  const flag = params.get('preview')
  const preview = flag === '1' || flag === 'true'
  ;(window as PreviewWindow)[PREVIEW_FLAG] = preview
  if (!preview) return

  const synth = window.speechSynthesis
  if (synth && !(synth as SpeechSynthesis & { __previewMuted?: boolean }).__previewMuted) {
    synth.speak = function previewMutedSpeak() {}
    synth.cancel = function previewMutedCancel() {}
    ;(synth as SpeechSynthesis & { __previewMuted?: boolean }).__previewMuted = true
  }

  const audioProto = window.HTMLAudioElement && window.HTMLAudioElement.prototype
  if (audioProto && !(audioProto as HTMLAudioElement & { __previewMuted?: boolean }).__previewMuted) {
    audioProto.play = function previewMutedPlay() {
      return Promise.resolve()
    }
    ;(audioProto as HTMLAudioElement & { __previewMuted?: boolean }).__previewMuted = true
  }
}
