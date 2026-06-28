<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2>任务管理</h2>
      <button class="btn btn-primary" @click="showCreate = true">创建任务</button>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>任务ID</th>
            <th>任务名称</th>
            <th>描述</th>
            <th>状态</th>
            <th>节点数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in taskStore.tasks" :key="task.taskId">
            <td>{{ task.taskId }}</td>
            <td>{{ task.taskName }}</td>
            <td>{{ task.scenarioDescription }}</td>
            <td>
              <span :class="['badge', statusClass(task.status)]">{{ task.status }}</span>
            </td>
            <td>{{ task.nodes?.length || 0 }}</td>
            <td>
              <button class="btn btn-primary" style="padding: 4px 8px; font-size: 12px; margin-right: 4px;" @click="goDetail(task.taskId)">详情</button>
              <button class="btn btn-success" style="padding: 4px 8px; font-size: 12px;" @click="startTask(task.taskId)">启动</button>
            </td>
          </tr>
          <tr v-if="taskStore.tasks.length === 0">
            <td colspan="6" style="text-align: center; color: #999;">暂无任务</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal-content">
        <h3 style="margin-bottom: 16px;">创建任务</h3>
        <div class="form-group">
          <label>任务名称</label>
          <input v-model="form.taskName" class="form-control" placeholder="请输入任务名称" />
        </div>
        <div class="form-group">
          <label>场景描述</label>
          <input v-model="form.scenarioDescription" class="form-control" placeholder="请输入场景描述" />
        </div>
        <div class="form-group">
          <label>节点配置 (JSON)</label>
          <textarea v-model="form.nodesJson" class="form-control" rows="10" placeholder='[{"nodeId":"data-access","nodeName":"数据接入","nodeType":"DATA","priority":1,"resourceRatio":0.2,"parameters":{"sourceType":"image"}}]'></textarea>
        </div>
        <div style="display: flex; gap: 8px; justify-content: flex-end;">
          <button class="btn" style="background: #f0f0f0;" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="submitCreate">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createTask, getTasks, startTask as apiStartTask } from '../services/api'
import { useTaskStore } from '../stores/task'

const router = useRouter()
const taskStore = useTaskStore()
const showCreate = ref(false)

const form = ref({
  taskName: '多模态联调测试任务',
  scenarioDescription: '验证前后端通讯、心跳与进度推送',
  nodesJson: JSON.stringify([
    {
      nodeId: 'data-access',
      nodeName: '数据接入',
      nodeType: 'DATA',
      priority: 1,
      resourceRatio: 0.2,
      parameters: { sourceType: 'image' }
    },
    {
      nodeId: 'model-invoke',
      nodeName: '模型调用',
      nodeType: 'MODEL',
      priority: 2,
      resourceRatio: 0.5,
      parameters: { modelId: 'yolov8-demo' }
    }
  ], null, 2)
})

function statusClass(status: string) {
  switch (status) {
    case 'RUNNING': return 'badge-success'
    case 'PAUSED': return 'badge-warning'
    case 'FAILED': return 'badge-error'
    case 'COMPLETED': return 'badge-info'
    default: return ''
  }
}

async function loadTasks() {
  try {
    const res = await getTasks()
    taskStore.setTasks(res as any[])
  } catch (e) {
    console.error('获取任务列表失败', e)
  }
}

function goDetail(taskId: string) {
  router.push(`/tasks/${taskId}`)
}

async function startTask(taskId: string) {
  try {
    await apiStartTask(taskId)
    alert('任务启动成功')
    loadTasks()
  } catch (e) {
    alert('任务启动失败')
  }
}

async function submitCreate() {
  try {
    const nodes = JSON.parse(form.value.nodesJson)
    const res = await createTask({
      taskName: form.value.taskName,
      scenarioDescription: form.value.scenarioDescription,
      nodes
    })
    alert(`任务创建成功: ${(res as any).taskId}`)
    showCreate.value = false
    loadTasks()
  } catch (e) {
    alert('创建失败，请检查 JSON 格式')
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}
</style>
