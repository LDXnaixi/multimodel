<template>
  <div>
    <h2 style="margin-bottom: 20px;">任务详情</h2>

    <div v-if="task" class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3>{{ task.taskName }}</h3>
        <span :class="['badge', statusClass(task.status)]">{{ task.status }}</span>
      </div>
      <p style="color: #666; margin-bottom: 16px;">{{ task.scenarioDescription }}</p>
      <p style="font-size: 14px; color: #999;">任务ID: {{ task.taskId }}</p>
    </div>

    <div class="card">
      <div class="card-title">实时进度</div>
      <div v-if="progress">
        <div style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
            <span>{{ progress.nodeId }} - {{ progress.nodeStatus }}</span>
            <span>{{ progress.progress }}%</span>
          </div>
          <div style="height: 16px; background: #f0f0f0; border-radius: 8px; overflow: hidden;">
            <div :style="{ width: progress.progress + '%', height: '100%', background: '#1890ff', borderRadius: '8px', transition: 'width 0.3s' }"></div>
          </div>
        </div>
        <p style="color: #666; font-size: 14px;">{{ progress.message }}</p>
      </div>
      <div v-else style="color: #999; text-align: center; padding: 20px;">暂无进度数据</div>
    </div>

    <div class="card">
      <div class="card-title">节点列表</div>
      <table v-if="task?.nodes?.length">
        <thead>
          <tr>
            <th>节点ID</th>
            <th>节点名称</th>
            <th>类型</th>
            <th>优先级</th>
            <th>资源占比</th>
            <th>参数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="node in task.nodes" :key="node.nodeId">
            <td>{{ node.nodeId }}</td>
            <td>{{ node.nodeName }}</td>
            <td>{{ node.nodeType }}</td>
            <td>{{ node.priority }}</td>
            <td>{{ (node.resourceRatio * 100).toFixed(0) }}%</td>
            <td>{{ JSON.stringify(node.parameters) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else style="color: #999; text-align: center;">无节点数据</div>
    </div>

    <div class="card">
      <div class="card-title">任务控制</div>
      <div style="display: flex; gap: 8px;">
        <button class="btn btn-warning" @click="control('PAUSE')">暂停</button>
        <button class="btn btn-success" @click="control('RESUME')">继续</button>
        <button class="btn btn-primary" @click="control('RERUN')">重新运行</button>
        <button class="btn btn-danger" @click="control('TERMINATE')">终止</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTask, controlTask } from '../services/api'
import { wsService } from '../services/websocket'
import { useTaskStore } from '../stores/task'

const route = useRoute()
const taskStore = useTaskStore()
const taskId = route.params.taskId as string

const task = computed(() => taskStore.currentTask)
const progress = computed(() => taskStore.getTaskProgress(taskId))

function statusClass(status: string) {
  switch (status) {
    case 'RUNNING': return 'badge-success'
    case 'PAUSED': return 'badge-warning'
    case 'FAILED': return 'badge-error'
    case 'COMPLETED': return 'badge-info'
    default: return ''
  }
}

async function loadTask() {
  try {
    const res = await getTask(taskId)
    taskStore.setCurrentTask(res as any)
  } catch (e) {
    console.error('获取任务详情失败', e)
  }
}

async function control(action: string) {
  try {
    await controlTask(taskId, action)
    alert(`操作 ${action} 已发送`)
    loadTask()
  } catch (e) {
    alert('操作失败')
  }
}

onMounted(() => {
  loadTask()
  wsService.subscribe(taskId)
})
</script>
