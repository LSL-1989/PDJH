/**
 * 候诊大屏浏览器 TTS（播报通道为 SCREEN/BOTH，或后端 speakOnScreen=true 时）。
 * 语速与后台「语速」一致：SAPI 量纲 -10~10（0 为正常）。
 */

function resolveScreenId(screensInput) {
  const si = screensInput || {}
  return String(si.ID || si.id || '').trim()
}

/**
 * 是否应由本屏发声。
 * - speakOnScreen === true 且 speakScreenId 匹配本屏（或未指定 speakScreenId）
 * - 或本屏 voiceChannel 为 SCREEN / BOTH
 */
export function shouldSpeakOnThisScreen(msg, screensInput) {
  if (!msg || typeof msg !== 'object') return false
  const channel = String(
    (screensInput && (screensInput.voiceChannel || screensInput.VoiceChannel)) ||
      msg.voiceChannel ||
      'VOICE'
  )
    .trim()
    .toUpperCase()
  const speakFlag = msg.speakOnScreen === true || msg.speakOnScreen === 'true' || msg.speakOnScreen === 1
  const myId = resolveScreenId(screensInput)
  const speakScreenId = String(msg.speakScreenId || '').trim()
  if (speakScreenId && myId && speakScreenId !== myId) {
    return false
  }
  if (channel === 'SCREEN' || channel === 'BOTH') return true
  return speakFlag
}

/** 从屏幕配置读取呼叫次数（至少 1，最多 5） */
export function resolveCallTimes(screensInput, fallback = 1) {
  const si = screensInput || {}
  const n = Number(si.CallNum ?? si.callNum ?? fallback)
  if (!Number.isFinite(n)) return Math.max(1, Math.min(5, Number(fallback) || 1))
  return Math.max(1, Math.min(5, Math.round(n)))
}

/** 从屏幕配置读取语速（SAPI -10~10） */
export function resolveSpeakRate(screensInput, fallback = 0) {
  const si = screensInput || {}
  const n = Number(si.Rate ?? si.rate ?? fallback)
  if (!Number.isFinite(n)) return 0
  return Math.max(-10, Math.min(10, Math.round(n)))
}

/** SAPI 语速 -10~10 → Web Speech utterance.rate（约 0.1~2，默认 1） */
export function sapiRateToBrowser(rate) {
  const n = Number(rate)
  if (!Number.isFinite(n)) return 1
  return Math.max(0.1, Math.min(10, 1 + n * 0.1))
}

/**
 * 浏览器 speechSynthesis 播报文案。
 * 预览窗直接返回，避免管理端跟着现场叫号出声。
 * @param {string} text
 * @param {number} [times=1] 重复次数
 * @param {number} [rate=0] SAPI 语速 -10~10
 */
export function speakCallText(text, times = 1, rate = 0) {
  if (typeof window !== 'undefined' && window.__SCREEN_PREVIEW__) return
  const content = String(text || '').trim()
  if (!content || typeof window === 'undefined' || !window.speechSynthesis) return
  const synth = window.speechSynthesis
  const n = Math.max(1, Math.min(Number(times) || 1, 5))
  const browserRate = sapiRateToBrowser(rate)
  try {
    synth.cancel()
  } catch (e) {
    /* ignore */
  }
  for (let i = 0; i < n; i++) {
    const u = new SpeechSynthesisUtterance(content)
    u.lang = 'zh-CN'
    u.rate = browserRate
    synth.speak(u)
  }
}
