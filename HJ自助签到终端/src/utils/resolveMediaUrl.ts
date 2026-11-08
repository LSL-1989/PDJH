/** 将后端返回的相对资源路径转为可访问 URL（医院 Logo 等；与门边屏一致） */
export function resolveMediaUrl(path: string | undefined | null): string {
  const raw = (path || '').trim()
  if (!raw) return ''
  if (/^https?:\/\//i.test(raw)) return raw
  if (raw.startsWith('/api/')) return raw
  if (raw.startsWith('/')) return `/api${raw}`
  return `/api/${raw}`
}
