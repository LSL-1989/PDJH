/**
 * 开发环境 AndroidNative Mock（无 Shell 时注入）
 * 在 dev 入口调用 installAndroidNativeMock(terminalType)
 */

import { generateShellDeviceId, isValidShellDeviceId } from './terminalBinding.js';

const MOCK_ID_KEY = 'pdjh_mock_shell_device_id';

function ensureMockDeviceId() {
  let id = null;
  try {
    id = localStorage.getItem(MOCK_ID_KEY);
  } catch {
    id = null;
  }
  if (!isValidShellDeviceId(id)) {
    id = generateShellDeviceId();
    try {
      localStorage.setItem(MOCK_ID_KEY, id);
    } catch {
      /* 隐私模式等场景下仅使用内存中的临时 ID */
    }
  }
  return id;
}

/**
 * @param {string} terminalType SCREEN | CLINIC | CHECKIN
 */
export function installAndroidNativeMock(terminalType = 'SCREEN') {
  if (typeof window === 'undefined') return;
  if (window.AndroidNative?.getDeviceId) return;

  window.AndroidNative = {
    getDeviceId() {
      return ensureMockDeviceId();
    },
    getTerminalType() {
      return terminalType;
    },
    getServerBase() {
      return '';
    }
  };
  window.__PDJH_TERMINAL_TYPE__ = terminalType;
}
