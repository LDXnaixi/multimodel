<template>
  <div>
    <h2 style="margin-bottom: 20px;">环境一致性报告</h2>
    <div class="card">
      <button class="btn btn-primary" @click="capture">采集当前环境</button>
      <button class="btn" style="margin-left: 8px; background: #f0f0f0;" @click="load">刷新历史</button>
    </div>

    <div v-if="latest" class="card">
      <div class="card-title">最新报告</div>
      <pre style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(latest, null, 2) }}</pre>
    </div>

    <div class="card">
      <div class="card-title">历史环境快照</div>
      <table v-if="reports.length">
        <thead>
          <tr>
            <th>报告ID</th>
            <th>采集时间</th>
            <th>一致性</th>
            <th>冲突数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.reportId">
            <td>{{ report.reportId }}</td>
            <td>{{ new Date(report.capturedAt).toLocaleString() }}</td>
            <td><span :class="['badge', report.consistent ? 'badge-success' : 'badge-error']">{{ report.consistent ? '通过' : '不一致' }}</span></td>
            <td>{{ report.conflicts?.length || 0 }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else style="color: #999; text-align: center;">暂无环境报告</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { captureEnvironment, getEnvironmentReports } from '../services/api'

const reports = ref<any[]>([])
const latest = ref<any>(null)

async function capture() {
  latest.value = await captureEnvironment()
  await load()
}

async function load() {
  reports.value = await getEnvironmentReports() as any[]
  latest.value = reports.value[0] || latest.value
}

onMounted(load)
</script>
