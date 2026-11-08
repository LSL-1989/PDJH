/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEV_PROXY_TARGET: string
  readonly VITE_HOSPITAL_NAME?: string
  readonly VITE_HOSPITAL_LOGO?: string
  readonly VITE_DEVICE_ID?: string
  readonly VITE_WS_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'sockjs-client/dist/sockjs' {
  export default class SockJS {
    constructor(url: string, _reserved?: unknown, options?: Record<string, unknown>)
  }
}
