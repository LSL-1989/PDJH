/**
 * 将后端返回的静态资源相对路径转为浏览器可访问 URL。
 * pdjh-server context-path 为 /api，开发环境经 /dev-api 代理。
 */
export function resolveBackendStaticUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const base = process.env.VUE_APP_BASE_API || ''
  const normalized = path.startsWith('/') ? path : `/${path}`
  return `${base}/api${normalized}`
}
