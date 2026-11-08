/** 9:16 竖屏设计稿基准尺寸（450×800），供 ScreenFrame 等比缩放 */
export const DESIGN_WIDTH = 450
export const DESIGN_HEIGHT = 800

/** Android WebView 上 innerHeight 比 visualViewport 更可靠（对齐门边屏） */
function getViewportSize() {
  const vw = document.documentElement.clientWidth || window.innerWidth
  const vh = window.innerHeight || document.documentElement.clientHeight
  return { vw, vh }
}

function applyViewportCssVars() {
  const { vw, vh } = getViewportSize()
  document.documentElement.style.setProperty('--viewport-width', `${vw}px`)
  document.documentElement.style.setProperty('--viewport-height', `${vh}px`)
}

/** 首屏前调用，避免 Android WebView 初次渲染高度塌陷 */
export function initViewportCssVars() {
  applyViewportCssVars()
}

export function useScreenScale(scaleWrapRef: { value: HTMLElement | null }) {
  const updateScreenScale = () => {
    applyViewportCssVars()
    const el = scaleWrapRef.value
    if (!el) return
    const { vw, vh } = getViewportSize()
    const scale = Math.min(vw / DESIGN_WIDTH, vh / DESIGN_HEIGHT)
    el.style.setProperty('--screen-scale', String(Math.round(scale * 100) / 100))
  }

  const bindScaleListeners = () => {
    updateScreenScale()
    window.addEventListener('resize', updateScreenScale)
    window.addEventListener('orientationchange', updateScreenScale)
    window.visualViewport?.addEventListener('resize', updateScreenScale)
  }

  const unbindScaleListeners = () => {
    window.removeEventListener('resize', updateScreenScale)
    window.removeEventListener('orientationchange', updateScreenScale)
    window.visualViewport?.removeEventListener('resize', updateScreenScale)
  }

  return { updateScreenScale, bindScaleListeners, unbindScaleListeners }
}
