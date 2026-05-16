<template>
  <div>
    <h2 style="margin-bottom: 20px;">模型列表</h2>
    <div class="card">
      <table>
        <thead>
          <tr>
            <th>模型ID</th>
            <th>名称</th>
            <th>模态</th>
            <th>版本</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="model in models" :key="(model as any).modelId">
            <td>{{ (model as any).modelId }}</td>
            <td>{{ (model as any).name }}</td>
            <td>{{ (model as any).modality }}</td>
            <td>{{ (model as any).version }}</td>
            <td>
              <span class="badge badge-success">{{ (model as any).status || 'AVAILABLE' }}</span>
            </td>
          </tr>
          <tr v-if="models.length === 0">
            <td colspan="5" style="text-align: center; color: #999;">暂无模型数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getModels } from '../services/api'

const models = ref<unknown[]>([])

async function loadModels() {
  try {
    const res = await getModels()
    models.value = res.data as unknown[]
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
}

onMounted(loadModels)
</script>
