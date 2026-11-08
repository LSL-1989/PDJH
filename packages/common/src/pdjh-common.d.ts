/** @pdjh/common 类型声明（JS 模块） */

export const STORAGE_KEY: string

export const TERMINAL_STATUS: {
  NOT_REGISTERED: string
  PENDING: string
  FROZEN: string
  READY: string
}

export const SHELL_DEVICE_ID_MAX_LEN: number

export function isValidShellDeviceId(id: string): boolean
export function normalizeShellDeviceId(raw: string): string
export function generateShellDeviceId(): string
export function getShellDeviceId(): string
export function getShellTerminalType(fallback?: string): string
export function getStoredDeviceCode(): string
export function setStoredDeviceCode(code: string): void
export function registerTerminal(
  deviceCode: string,
  terminalType: string
): Promise<{ ok: boolean; code?: number; msg?: string; data?: object }>
export function isTransientResolveFailure(result: {
  ok?: boolean
  code?: number
  msg?: string
} | null | undefined): boolean
export function resolveTerminal(
  deviceCode: string,
  terminalType: string
): Promise<{
  ok: boolean
  status: string
  code: number
  msg: string
  data: Record<string, unknown> | null
}>

export function installAndroidNativeMock(terminalType?: string): void

export function desensitizeName(val: string | null | undefined): string
