import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import type { ClinicDisplayData, StandbyReason } from '../types/clinic'
import { fetchClinicDisplay, resolveDeviceCodeFromRoute } from '../api/clinicScreen'

export function useQueuePoll(
  onData: (data: ClinicDisplayData) => void,
  onStandby: (reason: StandbyReason, message: string) => void
) {
  const route = useRoute()
  const displayData = ref<ClinicDisplayData | null>(null)
  const loading = ref(true)
  let pollTimer: ReturnType<typeof setInterval> | null = null
  let currentInterval = 5

  const stopPoll = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const startPoll = (intervalSec: number) => {
    currentInterval = intervalSec > 0 ? intervalSec : 5
    stopPoll()
    pollTimer = setInterval(() => {
      void refresh()
    }, currentInterval * 1000)
  }

  const refresh = async () => {
    const code = resolveDeviceCodeFromRoute(route.query.deviceCode)
    if (!code) {
      stopPoll()
      onStandby('no_device_code', '未配置设备编号')
      loading.value = false
      return null
    }
    const result = await fetchClinicDisplay(code)
    loading.value = false
    if (result.ok) {
      displayData.value = result.data
      onData(result.data)
      if (!pollTimer || result.data.screen.scrollIntervalSec !== currentInterval) {
        startPoll(result.data.screen.scrollIntervalSec)
      }
      return result.data
    }
    onStandby(result.reason, result.message)
    // 断网/瞬时失败：保持轮询以便恢复；业务绑定类错误才停轮询
    if (result.reason === 'network_error' || result.reason === 'unknown') {
      if (!pollTimer) startPoll(currentInterval)
    } else {
      stopPoll()
    }
    return null
  }

  onMounted(() => {
    void refresh()
  })

  onUnmounted(stopPoll)

  return { displayData, loading, refresh, startPoll, stopPoll }
}
