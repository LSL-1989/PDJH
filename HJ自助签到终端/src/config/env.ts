/**
 * 环境常量（与门边屏一致走 import.meta.env）。
 * 勿再用 Vite define 注入的全局 __DEV__：关掉 @vite/client 后安卓 WebView 无该全局会直接白屏。
 */
export const IS_DEV = import.meta.env.DEV
/** 与门边屏 VITE_HOSPITAL_NAME 同源；为空时页面兜底「某某医院」 */
export const HOSPITAL_NAME = (import.meta.env.VITE_HOSPITAL_NAME as string) || ''
/** 与门边屏 VITE_HOSPITAL_LOGO 同源；优先被 GetDeviceConfig.hospitalLogo 覆盖 */
export const HOSPITAL_LOGO = (import.meta.env.VITE_HOSPITAL_LOGO as string) || ''
export const DEVICE_ID = (import.meta.env.VITE_DEVICE_ID as string) || ''
export const DEV_DEFAULT_PATIENT_CODE =
  (import.meta.env.VITE_DEV_DEFAULT_PATIENT_CODE as string) || ''
