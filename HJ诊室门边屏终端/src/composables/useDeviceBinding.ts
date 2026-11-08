import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import type { ClinicDisplayData, StandbyReason } from '../types/clinic'
import { fetchClinicDisplay, resolveDeviceCodeFromRoute } from '../api/clinicScreen'

export function useDeviceBinding() {
  const route = useRoute()
  const deviceCode = ref('')
  const displayData = ref<ClinicDisplayData | null>(null)
  const standbyReason = ref<StandbyReason | null>(null)
  const standbyMessage = ref('')
  const loading = ref(true)

  const load = async () => {
    loading.value = true
    const code = resolveDeviceCodeFromRoute(route.query.deviceCode)
    deviceCode.value = code

    if (!code) {
      standbyReason.value = 'no_device_code'
      standbyMessage.value = '未配置设备编号'
      displayData.value = null
      loading.value = false
      return
    }

    const result = await fetchClinicDisplay(code)
    if (result.ok) {
      standbyReason.value = null
      standbyMessage.value = ''
      displayData.value = result.data
    } else {
      standbyReason.value = result.reason
      standbyMessage.value = result.message
      displayData.value = null
    }
    loading.value = false
  }

  onMounted(() => {
    void load()
  })

  return {
    deviceCode,
    displayData,
    standbyReason,
    standbyMessage,
    loading,
    reload: load
  }
}

export function useDeviceBindingWithPoll(
  onData: (data: ClinicDisplayData) => void,
  onStandby: (reason: StandbyReason, message: string) => void
) {
  const route = useRoute()
  const deviceCode = ref('')
  let pollTimer: ReturnType<typeof setInterval> | null = null

  const stopPoll = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const refresh = async () => {
    const code = resolveDeviceCodeFromRoute(route.query.deviceCode)
    deviceCode.value = code
    if (!code) {
      stopPoll()
      onStandby('no_device_code', '未配置设备编号')
      return
    }
    const result = await fetchClinicDisplay(code)
    if (result.ok) {
      onData(result.data)
      return result.data
    }
    stopPoll()
    onStandby(result.reason, result.message)
    return null
  }

  const startPoll = (intervalSec: number) => {
    stopPoll()
    pollTimer = setInterval(() => {
      void refresh()
    }, intervalSec * 1000)
  }

  onUnmounted(stopPoll)

  return { deviceCode, refresh, startPoll, stopPoll }
}
