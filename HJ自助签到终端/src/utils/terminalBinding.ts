export {
  STORAGE_KEY,
  TERMINAL_STATUS,
  SHELL_DEVICE_ID_MAX_LEN,
  isValidShellDeviceId,
  normalizeShellDeviceId,
  getShellDeviceId,
  getShellTerminalType,
  getStoredDeviceCode,
  setStoredDeviceCode,
  registerTerminal,
  resolveTerminal
} from '@pdjh/common/terminalBinding'

export { installAndroidNativeMock } from '@pdjh/common/androidNative.mock'
