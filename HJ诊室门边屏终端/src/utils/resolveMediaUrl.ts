/** 将后端返回的相对资源路径转为可访问 URL（头像、医院 Logo 等） */
export function resolveMediaUrl(path: string | undefined | null): string {
  const raw = (path || '').trim()
  if (!raw) return ''
  if (/^https?:\/\//i.test(raw)) return raw
  if (raw.startsWith('/api/')) return raw
  if (raw.startsWith('/')) return `/api${raw}`
  return `/api/${raw}`
}
