/**
 * 根据后端 RBAC 返回的 Routes（PageUrl）过滤前端 constantRoutes，生成侧边栏菜单。
 * 匹配：PageUrl 与路由 name / path 片段（忽略大小写）一致即视为有权限。
 * 展示名：优先使用服务端 PageShowName（菜单管理可改），router meta.title 仅作兜底。
 * 同一 PageUrl 对应不同名称时丢弃该键（例如两条子菜单都曾写成 index），避免后一条盖掉前一条的侧栏标题。
 */

/** PageUrl -> PageShowName，供侧边栏、面包屑、页签标题解析 */
export function buildMenuTitleMap(serverRoutes) {
  const map = {}
  const conflicted = new Set()
  if (!Array.isArray(serverRoutes)) {
    return map
  }
  serverRoutes.forEach((item) => {
    const url = (item.PageUrl || '').trim().toLowerCase()
    const name = item.PageShowName
    if (!url || !name) return
    if (conflicted.has(url)) return
    if (Object.prototype.hasOwnProperty.call(map, url) && map[url] !== name) {
      delete map[url]
      conflicted.add(url)
      return
    }
    map[url] = name
  })
  return map
}

function routeTitleCandidates(route) {
  const candidates = []
  if (route && route.name) {
    candidates.push(String(route.name).toLowerCase())
  }
  if (route && route.path) {
    String(route.path)
      .split('/')
      .forEach((seg) => {
        const k = seg.trim().toLowerCase()
        if (k && !k.startsWith(':')) candidates.push(k)
      })
  }
  return candidates
}

/** 按 PageUrl 解析展示标题；无服务端配置时回退 meta.title */
export function resolveMenuTitle(route, titleMap) {
  if (!route) return ''
  const candidates = routeTitleCandidates(route)
  if (titleMap) {
    for (let i = 0; i < candidates.length; i++) {
      const title = titleMap[candidates[i]]
      if (title) return title
    }
  }
  return (route.meta && route.meta.title) || ''
}

function applyServerTitles(route, titleMap) {
  if (!route) return route
  const title = resolveMenuTitle(route, titleMap)
  const next = {
    ...route,
    meta: route.meta
      ? { ...route.meta, title: title || route.meta.title }
      : (title ? { title } : route.meta)
  }
  if (next.children && next.children.length) {
    next.children = next.children.map((child) => applyServerTitles(child, titleMap))
  }
  return next
}

export function buildAllowedMenuKeys(serverRoutes) {
  const keys = new Set()
  if (!Array.isArray(serverRoutes)) {
    return keys
  }
  serverRoutes.forEach((item) => {
    const url = (item.PageUrl || '').trim()
    if (!url) return
    url.split('/').forEach((seg) => {
      const k = seg.trim().toLowerCase()
      if (k) keys.add(k)
    })
  })
  return keys
}

function routeEntryMatches(route, allowedKeys) {
  if (!route || !allowedKeys || allowedKeys.size === 0) return false
  const candidates = []
  if (route.name) {
    candidates.push(String(route.name).toLowerCase())
  }
  if (route.path) {
    String(route.path)
      .split('/')
      .forEach((seg) => {
        const k = seg.trim().toLowerCase()
        if (k && !k.startsWith(':')) candidates.push(k)
      })
  }
  return candidates.some((c) => allowedKeys.has(c))
}

/**
 * @param {Array} constantRoutes 前端完整路由表
 * @param {Array} serverRoutes 登录接口返回的 Routes
 * @returns {{ sidebarRoutes: Array, allowedKeys: Set }}
 */
export function filterRoutesByPermission(constantRoutes, serverRoutes) {
  const allowedKeys = buildAllowedMenuKeys(serverRoutes)
  const titleMap = buildMenuTitleMap(serverRoutes)

  if (allowedKeys.size === 0) {
    const sidebarRoutes = constantRoutes.map((route) => applyServerTitles(route, titleMap))
    return { sidebarRoutes, allowedKeys, titleMap }
  }

  const sidebarRoutes = constantRoutes
    .map((route) => {
      if (route.hidden) {
        return route
      }

      if (route.children && route.children.length) {
        const children = route.children.filter((child) =>
          routeEntryMatches(child, allowedKeys)
        )
        const parentOk = routeEntryMatches(route, allowedKeys)
        if (children.length === 0 && !parentOk) {
          return null
        }
        if (children.length === 0) {
          return null
        }

        const next = { ...route, children: [...children] }
        if (next.redirect && children[0]) {
          const base = next.path.endsWith('/') ? next.path.slice(0, -1) : next.path
          next.redirect = `${base}/${children[0].path}`
        }
        return next
      }

      return routeEntryMatches(route, allowedKeys) ? route : null
    })
    .filter(Boolean)
    .map((route) => applyServerTitles(route, titleMap))

  return { sidebarRoutes, allowedKeys, titleMap }
}

/** 路由导航是否落在已授权菜单范围内 */
export function isPathAllowed(route, allowedKeys) {
  if (!allowedKeys || allowedKeys.size === 0) {
    return true
  }
  if (!route || !route.path) {
    return true
  }
  if (route.meta && route.meta.public) {
    return true
  }
  const segments = route.path.split('/').filter(Boolean)
  for (let i = segments.length; i > 0; i--) {
    const partial = '/' + segments.slice(0, i).join('/')
    const fake = { path: partial, name: route.name }
    if (routeEntryMatches(fake, allowedKeys)) {
      return true
    }
  }
  if (route.name && routeEntryMatches({ name: route.name, path: '' }, allowedKeys)) {
    return true
  }
  return false
}
