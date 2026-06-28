<template>
  <div>
    <h2 style="margin-bottom: 20px;">总览</h2>
    <div class="grid-4">
      <div class="metric-card">
        <div class="metric-label">服务状态</div>
        <div class="metric-value" :style="{ color: health?.status === 'UP' ? '#52c41a' : '#ff4d4f' }">
          {{ health?.status || '-' }}
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-label">服务名称</div>
        <div class="metric-value" style="font-size: 18px;">{{ health?.service || '-' }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">WebSocket 客户端数</div>
        <div class="metric-value">{{ health?.websocketClients ?? '-' }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">CPU 使用率</div>
        <div class="metric-value" :style="{ color: cpuAlert ? '#ff4d4f' : '#1890ff' }">
          {{ latestMetric ? latestMetric.cpuUsage.toFixed(1) + '%' : '-' }}
        </div>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 20px;">
      <div class="card">
        <div class="card-title">最新告警</div>
        <div v-if="appStore.alerts.length === 0" style="color: #999; text-align: center; padding: 20px;">
          暂无告警
        </div>
        <ul v-else style="list-style: none;">
          <li v-for="alert in appStore.alerts.slice(0, 5)" :key="alert.alertId" style="padding: 10px 0; border-bottom: 1px solid #f0f0f0;">
            <span :class="['badge', alert.level === 'WARN' ? 'badge-warning' : 'badge-error']">{{ alert.level }}</span>
            <span style="margin-left: 8px;">{{ alert.message }}</span>
            <span style="float: right; color: #999; font-size: 12px;">{{ formatTime(alert.timestamp) }}</span>
          </li>
        </ul>
      </div>
      <div class="card">
        <div class="card-title">资源指标</div>
        <div v-if="!latestMetric" style="color: #999; text-align: center; padding: 20px;">等待数据推送...</div>
        <div v-else>
          <div class="resource-bar" v-for="item in resourceItems" :key="item.key">
            <div class="resource-label">{{ item.label }}</div>
            <div class="resource-track">
              <div class="resource-fill" :style="{ width: getValue(item.key) + '%', background: getColor(item.key) }"></div>
            </div>
            <div class="resource-value">{{ getValue(item.key).toFixed(1) }}%</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getHealth } from '../services/api'
import { useAppStore } from '../stores/app'

const appStore = useAppStore()
const health = ref<Record<string, unknown> | null>(null)

const latestMetric = computed(() => appStore.latestMetric)

const cpuAlert = computed(() => {
  if (!latestMetric.value) return false
  return latestMetric.value.cpuUsage > latestMetric.value.cpuUsageThreshold
})

const resourceItems = [
  { key: 'cpuUsage', label: 'CPU' },
  { key: 'memoryUsage', label: '内存' },
  { key: 'heapUsage', label: '堆内存' },
  { key: 'diskUsage', label: '磁盘' },
  { key: 'networkThroughput', label: '网络吞吐' },
  { key: 'gpuUsage', label: 'GPU' }
]

function getValue(key: string): number {
  if (!latestMetric.value) return 0
  return (latestMetric.value as Record<string, number>)[key] || 0
}

function getColor(key: string): string {
  const val = getValue(key)
  if (key === 'cpuUsage' && latestMetric.value && val > latestMetric.value.cpuUsageThreshold) return '#ff4d4f'
  if (key === 'memoryUsage' && latestMetric.value && val > latestMetric.value.memoryUsageThreshold) return '#ff4d4f'
  if (val > 80) return '#faad14'
  return '#1890ff'
}

function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString()
}

async function fetchHealth() {
  try {
    const res = await getHealth()
    health.value = res
  } catch (e) {
    console.error('获取健康状态失败', e)
  }
}

onMounted(() => {
  fetchHealth()
  const timer = setInterval(fetchHealth, 5000)
  return () => clearInterval(timer)
})
</script>

<style scoped>
.resource-bar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.resource-label {
  width: 80px;
  font-size: 14px;
}

.resource-track {
  flex: 1;
  height: 16px;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  margin: 0 12px;
}

.resource-fill {
  height: 100%;
  border-radius: 8px;
  transition: width 0.3s;
}

.resource-value {
  width: 60px;
  text-align: right;
  font-size: 14px;
  font-weight: 500;
}
</style>
