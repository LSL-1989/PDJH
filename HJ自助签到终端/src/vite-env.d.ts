/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DEV_PROXY_TARGET: string
  readonly VITE_DEV_DEFAULT_PATIENT_CODE?: string
  readonly VITE_HOSPITAL_NAME?: string
  readonly VITE_HOSPITAL_LOGO?: string
  readonly VITE_DEVICE_ID?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
