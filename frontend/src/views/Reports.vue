<template>
  <div>
    <h2 style="margin-bottom: 20px;">报告导出</h2>
    <div class="card">
      <div class="form-group">
        <label>任务ID</label>
        <input v-model="taskId" class="form-control" placeholder="请输入任务ID" />
      </div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-primary" @click="query">查询报告</button>
        <button class="btn btn-success" @click="exportReport('CSV')">导出 CSV</button>
        <button class="btn btn-success" @click="exportReport('JSON')">导出 JSON</button>
        <button class="btn btn-success" @click="exportReport('XML')">导出 XML</button>
        <button class="btn btn-success" @click="exportReport('PDF')">导出 PDF</button>
      </div>
    </div>

    <div v-if="report" class="card">
      <div class="card-title">报告内容</div>
      <pre style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(report, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getReport, exportReport as apiExportReport } from '../services/api'

const taskId = ref('')
const report = ref<unknown>(null)

async function query() {
  if (!taskId.value) return
  try {
    const res = await getReport(taskId.value)
    report.value = res
  } catch (e) {
    alert('查询失败')
  }
}

async function exportReport(format: string) {
  if (!taskId.value) {
    alert('请输入任务ID')
    return
  }
  try {
    const res = await apiExportReport(taskId.value, format)
    const blob = new Blob([res as unknown as BlobPart])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `report-${taskId.value}.${format.toLowerCase()}`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    alert('导出失败')
  }
}
</script>
