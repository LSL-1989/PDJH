export type StandbyReason =
  | 'no_device_code'
  | 'not_registered'
  | 'not_bound'
  | 'pending'
  | 'frozen'
  | 'network_error'
  | 'unknown'

export interface ClinicScreenInfo {
  id: string
  state: number
  bulletin: string
  scrollIntervalSec: number
  callCode?: string
  nameDesensitize?: boolean
  showWaitList?: boolean
}

export interface ClinicRoomInfo {
  id: string
  name: string
  deptId: string
  deptName: string
  floor: string
  roomNo: string
}

export interface ClinicDoctorInfo {
  id: string
  name: string
  title: string
  avatar: string
  /** 医生简介（doctor.remarks） */
  remarks: string
  specialty: string
  isOnDuty: number
}

export interface ClinicPatientRow {
  doctorId?: string
  showNo: string
  xh: number
  patientName: string
  /** 分诊状态：REVISIT 复诊，WAITING/null 初诊 */
  triageState?: string
  state?: number
}

export interface ClinicDisplayData {
  /** 全院医院名称，来自 sys_config.HospitalName */
  hospitalName?: string
  /** 医院 Logo 地址，来自 sys_config.HospitalLogo */
  hospitalLogo?: string
  screen: ClinicScreenInfo
  room: ClinicRoomInfo
  doctors: ClinicDoctorInfo[]
  currentPatients: ClinicPatientRow[]
  waitingPatients: ClinicPatientRow[]
}

export interface ApiResult<T> {
  success?: boolean
  code?: number
  msg?: string
  message?: string
  data?: T
}
