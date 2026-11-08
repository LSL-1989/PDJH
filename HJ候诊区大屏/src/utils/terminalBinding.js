/** 复用 monorepo 共享模块（与 packages/common 保持同步） */
export {
  STORAGE_KEY,
  TERMINAL_STATUS,
  SHELL_DEVICE_ID_MAX_LEN,
  getShellDeviceId,
  getShellTerminalType,
  getStoredDeviceCode,
  setStoredDeviceCode,
  normalizeShellDeviceId,
  isValidShellDeviceId,
  registerTerminal,
  resolveTerminal,
  isTransientResolveFailure
} from '../../../packages/common/src/terminalBinding.js';

export { installAndroidNativeMock } from '../../../packages/common/src/androidNative.mock.js';
