import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface AlertItem {
  alertId: string
  level: string
  category: string
  message: string
  timestamp: number
}

export interface ResourceMetric {
  cpuUsage: number
  memoryUsage: number
  heapUsage: number
  diskUsage: number
  networkThroughput: number
  gpuUsage: number
  cpuUsageThreshold: number
  memoryUsageThreshold: number
  timestamp: number
}

export const useAppStore = defineStore('app', () => {
  const alerts = ref<AlertItem[]>([])
  const latestMetric = ref<ResourceMetric | null>(null)
  const wsConnected = ref(false)

  function addAlert(alert: AlertItem) {
    alerts.value.unshift(alert)
    if (alerts.value.length > 50) {
      alerts.value.pop()
    }
  }

  function clearAlerts() {
    alerts.value = []
  }

  function setLatestMetric(metric: ResourceMetric) {
    latestMetric.value = metric
  }

  function setWsConnected(connected: boolean) {
    wsConnected.value = connected
  }

  return {
    alerts,
    latestMetric,
    wsConnected,
    addAlert,
    clearAlerts,
    setLatestMetric,
    setWsConnected
  }
})
