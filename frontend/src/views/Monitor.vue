<template>
  <div>
    <h2 style="margin-bottom: 20px;">资源监控</h2>

    <div class="grid-3">
      <div class="metric-card">
        <div class="metric-label">CPU 使用率</div>
        <div class="metric-value">{{ metric?.cpuUsage?.toFixed(1) ?? '-' }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">内存使用率</div>
        <div class="metric-value">{{ metric?.memoryUsage?.toFixed(1) ?? '-' }}%</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">GPU 使用率</div>
        <div class="metric-value">{{ metric?.gpuUsage?.toFixed(1) ?? '-' }}%</div>
      </div>
    </div>

    <div class="card" style="margin-top: 20px;">
      <div class="card-title">阈值配置</div>
      <div style="display: flex; gap: 16px; align-items: flex-end;">
        <div class="form-group" style="flex: 1;">
          <label>CPU 阈值 (%)</label>
          <input v-model.number="thresholds.cpuUsageThreshold" type="number" class="form-control" />
        </div>
        <div class="form-group" style="flex: 1;">
          <label>内存阈值 (%)</label>
          <input v-model.number="thresholds.memoryUsageThreshold" type="number" class="form-control" />
        </div>
        <button class="btn btn-primary" @click="saveThresholds">保存</button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">历史告警</div>
      <table v-if="alerts.length">
        <thead>
          <tr>
            <th>时间</th>
            <th>级别</th>
            <th>类别</th>
            <th>消息</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="alert in alerts" :key="alert.alertId">
            <td>{{ new Date(alert.timestamp).toLocaleString() }}</td>
            <td>
              <span :class="['badge', alert.level === 'WARN' ? 'badge-warning' : 'badge-error']">{{ alert.level }}</span>
            </td>
            <td>{{ alert.category }}</td>
            <td>{{ alert.message }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else style="color: #999; text-align: center;">暂无告警</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getMetrics, getAlerts, updateThresholds } from '../services/api'
import { useAppStore } from '../stores/app'

const appStore = useAppStore()
const metric = computed(() => appStore.latestMetric)
const alerts = computed(() => appStore.alerts)

const thresholds = ref({
  cpuUsageThreshold: 75,
  memoryUsageThreshold: 80
})

async function loadMetrics() {
  try {
    const res = await getMetrics()
    const data = res.data as Record<string, unknown>
    if (data.cpuUsageThreshold) thresholds.value.cpuUsageThreshold = data.cpuUsageThreshold as number
    if (data.memoryUsageThreshold) thresholds.value.memoryUsageThreshold = data.memoryUsageThreshold as number
  } catch (e) {
    console.error('获取指标失败', e)
  }
}

async function loadAlerts() {
  try {
    const res = await getAlerts()
    const list = (res.data as any)?.alerts || []
    list.forEach((a: any) => appStore.addAlert(a))
  } catch (e) {
    console.error('获取告警失败', e)
  }
}

async function saveThresholds() {
  try {
    await updateThresholds(thresholds.value)
    alert('阈值已更新')
  } catch (e) {
    alert('更新失败')
  }
}

onMounted(() => {
  loadMetrics()
  loadAlerts()
})
</script>
