/**
 * 候诊区大屏只读预览（管理后台「预览效果」新开页）
 * 消费者：main.js 首屏安装、App.vue 跳过设备绑定、axios 拦截写接口、播报静音
 * 依赖：地址栏 hash 查询参数 preview=1；不调用 Terminal/Register、Resolve，不写 lastSeenAt
 */

const PREVIEW_FLAG = '__SCREEN_PREVIEW__'

/**
 * 当前窗口是否为只读预览。
 * 以首屏写入的 window 标记为准，避免模板内部跳转丢掉 query 后误走绑定/叫号。
 * @param {import('vue-router').RouteLocationNormalized|undefined} route
 * @returns {boolean}
 */
export function isScreenPreview(route) {
  if (typeof window !== 'undefined' && window[PREVIEW_FLAG]) return true
  const query = route && route.query ? route.query : {}
  const flag = query.preview
  return flag === '1' || flag === 'true' || flag === true
}

/**
 * 根据地址栏 hash 标记预览窗，并关掉本页叫号声音。
 * 只静音 speechSynthesis 与 audio 元素；视频宣传素材仍可播放。
 * 不改 localStorage 设备编号，避免把管理端浏览器登记成现场屏。
 */
export function installScreenPreviewGuard() {
  if (typeof window === 'undefined') return
  const hash = window.location.hash || ''
  const queryIndex = hash.indexOf('?')
  const params = new URLSearchParams(queryIndex >= 0 ? hash.slice(queryIndex + 1) : '')
  const flag = params.get('preview')
  const preview = flag === '1' || flag === 'true'
  window[PREVIEW_FLAG] = preview
  if (!preview) return

  const synth = window.speechSynthesis
  if (synth && !synth.__previewMuted) {
    synth.speak = function previewMutedSpeak() {}
    synth.cancel = function previewMutedCancel() {}
    synth.__previewMuted = true
  }

  const audioProto = window.HTMLAudioElement && window.HTMLAudioElement.prototype
  if (audioProto && !audioProto.__previewMuted) {
    audioProto.play = function previewMutedPlay() {
      return Promise.resolve()
    }
    audioProto.__previewMuted = true
  }
}
