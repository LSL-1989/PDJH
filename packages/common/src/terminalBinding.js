/**
 * 安卓 WebView 终端双线绑定（Shell UUID + Register/Resolve）
 * 消费者：候诊区大屏、诊室门边屏、自助签到终端 BindingGuide
 *
 * Resolve 返回的 templateRoute 仅候诊区大屏（SCREEN）使用；CLINIC 门边屏忽略该字段。
 */

export const STORAGE_KEY = 'pdjh_device_code';

export const TERMINAL_STATUS = {
  NOT_REGISTERED: 'NOT_REGISTERED',
  PENDING: 'PENDING',
  FROZEN: 'FROZEN',
  READY: 'READY'
};

/** 本机 Shell 设备 ID 最大长度（登记/解析统一约束） */
export const SHELL_DEVICE_ID_MAX_LEN = 20;

const SHELL_DEVICE_ID_PATTERN = /^[A-Z0-9-]+$/;

/**
 * 校验本机设备 ID 格式：≤20 位，仅数字、大写字母与连字符。
 * @param {string} id
 */
export function isValidShellDeviceId(id) {
  if (!id || typeof id !== 'string') return false;
  const trimmed = id.trim();
  return trimmed.length > 0 && trimmed.length <= SHELL_DEVICE_ID_MAX_LEN && SHELL_DEVICE_ID_PATTERN.test(trimmed);
}

/**
 * 将 Shell 原始 ID 规范为大写、数字与「-」，并截断至 20 位；无效时返回空串。
 * 用于「使用本机设备 ID」路径，确保登记编号最终拼接长度 ≤20。
 * @param {string} raw
 */
export function normalizeShellDeviceId(raw) {
  if (raw == null || raw === '') return '';
  let s = String(raw).trim().toUpperCase().replace(/[^A-Z0-9-]/g, '');
  s = s.replace(/-{2,}/g, '-').replace(/^-+|-+$/g, '');
  if (!s) return '';
  if (s.length > SHELL_DEVICE_ID_MAX_LEN) {
    s = s.slice(0, SHELL_DEVICE_ID_MAX_LEN).replace(/-+$/g, '');
  }
  return SHELL_DEVICE_ID_PATTERN.test(s) && s.length <= SHELL_DEVICE_ID_MAX_LEN ? s : '';
}

/**
 * 生成符合规范的本机设备 ID（XXXX-XXXX-XXXX-XXXX，19 位）。
 * 供开发 Mock 与 Android Shell 对齐；真机原始值经 normalizeShellDeviceId 再入库。
 */
export function generateShellDeviceId() {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const pick = () => {
    if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
      const buf = new Uint8Array(1);
      crypto.getRandomValues(buf);
      return chars[buf[0] % chars.length];
    }
    return chars[Math.floor(Math.random() * chars.length)];
  };
  const segment = (len) => Array.from({ length: len }, pick).join('');
  // 4+1+4+1+4+1+4 = 19 ≤ 20
  return `${segment(4)}-${segment(4)}-${segment(4)}-${segment(4)}`;
}

/** Shell 持久化设备 ID；开发环境回退 localStorage Mock。始终返回已规范化编号或空串。 */
export function getShellDeviceId() {
  if (typeof window !== 'undefined' && window.AndroidNative?.getDeviceId) {
    const normalized = normalizeShellDeviceId(window.AndroidNative.getDeviceId());
    if (normalized) return normalized;
  }
  const stored = getStoredDeviceCode();
  if (stored) return normalizeShellDeviceId(stored);
  return '';
}

/** Shell 写死终端类型；开发用 localStorage 或入参兜底 */
export function getShellTerminalType(fallback = 'SCREEN') {
  if (typeof window !== 'undefined' && window.AndroidNative?.getTerminalType) {
    const t = window.AndroidNative.getTerminalType();
    if (t && String(t).trim()) return String(t).trim().toUpperCase();
  }
  if (typeof window !== 'undefined') {
    const dev = window.__PDJH_TERMINAL_TYPE__;
    if (dev) return String(dev).toUpperCase();
  }
  return (fallback || 'SCREEN').toUpperCase();
}

function safeGetItem(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSetItem(key, value) {
  try {
    localStorage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

function safeRemoveItem(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    /* 隐私模式 / WebView 可能禁用 localStorage */
  }
}

export function getStoredDeviceCode() {
  if (typeof localStorage === 'undefined') return '';
  return safeGetItem(STORAGE_KEY)?.trim() || '';
}

export function setStoredDeviceCode(code) {
  if (typeof localStorage === 'undefined') return;
  if (code?.trim()) {
    safeSetItem(STORAGE_KEY, code.trim());
  } else {
    safeRemoveItem(STORAGE_KEY);
  }
}

/**
 * 规范化 Shell 服务根：去尾斜杠，去掉误填的 /clinic|/screen|/checkin|/api。
 * @param {string} raw
 */
function normalizeServerBase(raw) {
  let base = String(raw || '').trim().replace(/\/+$/, '');
  if (!base) return '';
  base = base.replace(/\/(clinic|screen|checkin|api)$/i, '');
  return base.replace(/\/+$/, '');
}

/**
 * 绑定接口基址。
 * http(s) 页面一律走同源相对 /api（与门边屏业务 axios、Vite 代理、Nginx 反代一致），
 * 避免 AndroidNative.getServerBase() 指向 :8081 或带 /clinic 后缀时跨源/404，被前端当成「网络异常」。
 * 仅 file:// 等无 host 场景回退 Shell 绝对根。
 */
function apiBase() {
  if (typeof window === 'undefined') return '';
  if (window.location.protocol === 'http:' || window.location.protocol === 'https:') {
    return '';
  }
  return normalizeServerBase(window.AndroidNative?.getServerBase?.());
}

/**
 * @returns {Promise<{ httpStatus: number, body: object|null, netError?: string }>}
 */
async function requestJson(path, options = {}, timeoutMs = 8000) {
  const base = apiBase();
  const url = `${base}${path}`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const method = String(options.method || 'GET').toUpperCase();
  const headers = { ...(options.headers || {}) };
  if (method !== 'GET' && method !== 'HEAD' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  try {
    const res = await fetch(url, {
      ...options,
      headers,
      signal: controller.signal
    });
    let body = null;
    try {
      body = await res.json();
    } catch {
      body = null;
    }
    if (!body) {
      return {
        httpStatus: res.status,
        body: null,
        netError: res.status ? `服务响应异常(${res.status})` : '网络异常'
      };
    }
    return { httpStatus: res.status, body };
  } catch (err) {
    const aborted = err && /** @type {{ name?: string }} */ (err).name === 'AbortError';
    return { httpStatus: 0, body: null, netError: aborted ? '请求超时' : '网络异常' };
  } finally {
    clearTimeout(timer);
  }
}

/**
 * 登记终端（路径 A 自助待配置）
 * @returns {{ ok: boolean, code?: number, msg?: string, data?: object }}
 */
export async function registerTerminal(deviceCode, terminalType) {
  const { body, netError } = await requestJson('/api/Terminal/Register', {
    method: 'POST',
    body: JSON.stringify({ deviceCode, terminalType })
  });
  if (!body) {
    return { ok: false, code: 500, msg: netError || '网络异常' };
  }
  if (body.success) {
    return { ok: true, code: 200, msg: body.msg, data: body.data };
  }
  return { ok: false, code: body.code || 500, msg: body.msg || '登记失败' };
}

/**
 * 解析终端配置状态
 * @returns {{ ok: boolean, status: string, code: number, msg: string, data: object|null }}
 */
export async function resolveTerminal(deviceCode, terminalType) {
  const q = new URLSearchParams({ deviceCode, terminalType });
  const { body, netError } = await requestJson(`/api/Terminal/Resolve?${q.toString()}`, {
    method: 'GET'
  });
  if (!body) {
    return { ok: false, status: 'UNKNOWN', code: 500, msg: netError || '网络异常', data: null };
  }
  const data = body.data || null;
  const status = data?.status || inferStatusFromCode(body.code);
  if (body.success && status === TERMINAL_STATUS.READY) {
    return { ok: true, status, code: 200, msg: body.msg || '', data };
  }
  if (body.code === 428) {
    return { ok: false, status: TERMINAL_STATUS.PENDING, code: 428, msg: body.msg || '', data };
  }
  if (body.code === 404) {
    return { ok: false, status: TERMINAL_STATUS.NOT_REGISTERED, code: 404, msg: body.msg || '', data };
  }
  if (body.code === 403) {
    return { ok: false, status: TERMINAL_STATUS.FROZEN, code: 403, msg: body.msg || '', data };
  }
  return {
    ok: false,
    status: status || 'UNKNOWN',
    code: body.code || 500,
    msg: body.msg || '解析失败',
    data
  };
}

/**
 * Resolve 是否因断网、超时或空响应失败。
 * 404 未登记、428 待配置、403 冻结是业务状态，不算瞬时失败。
 * @param {{ ok?: boolean, code?: number, msg?: string }|null|undefined} result
 */
export function isTransientResolveFailure(result) {
  if (!result || result.ok) return false
  if (result.code === 0) return true
  return /网络异常|请求超时|服务响应异常/.test(String(result.msg || ''))
}

function inferStatusFromCode(code) {
  if (code === 404) return TERMINAL_STATUS.NOT_REGISTERED;
  if (code === 428) return TERMINAL_STATUS.PENDING;
  if (code === 403) return TERMINAL_STATUS.FROZEN;
  return 'UNKNOWN';
}
