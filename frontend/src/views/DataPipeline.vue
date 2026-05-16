<template>
  <div>
    <h2 style="margin-bottom: 20px;">数据流水线</h2>

    <div class="card">
      <h3 style="margin-bottom: 16px;">数据集注册</h3>
      <div class="form-group">
        <label>数据集名称</label>
        <input v-model="datasetForm.datasetName" class="form-control" placeholder="联调数据集" />
      </div>
      <div class="form-group">
        <label>资产列表 (JSON)</label>
        <textarea v-model="datasetForm.assetsJson" class="form-control" rows="6"></textarea>
      </div>
      <div class="form-group">
        <label>过滤规则 (JSON)</label>
        <input v-model="datasetForm.filterRules" class="form-control" placeholder='{"speaker":"demo"}' />
      </div>
      <button class="btn btn-primary" @click="register">注册数据集</button>
      <div v-if="datasetId" style="margin-top: 12px; color: #52c41a;">数据集ID: {{ datasetId }}</div>
    </div>

    <div class="card">
      <h3 style="margin-bottom: 16px;">流水线执行</h3>
      <div class="form-group">
        <label>数据集ID</label>
        <input v-model="pipelineForm.datasetId" class="form-control" placeholder="上一步返回的数据集ID" />
      </div>
      <div class="form-group">
        <label>操作列表 (JSON)</label>
        <textarea v-model="pipelineForm.operationsJson" class="form-control" rows="6"></textarea>
      </div>
      <button class="btn btn-primary" @click="run">执行流水线</button>
    </div>

    <div v-if="pipelineResult" class="card">
      <div class="card-title">流水线结果</div>
      <pre style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(pipelineResult, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { registerDataset, runPipeline } from '../services/api'

const datasetForm = ref({
  datasetName: '联调数据集',
  assetsJson: JSON.stringify([
    {
      assetId: 'asset-001',
      uri: 'samples/a.wav',
      modality: 'audio',
      tags: ['speech', 'test']
    }
  ], null, 2),
  filterRules: '{"speaker":"demo"}'
})

const pipelineForm = ref({
  datasetId: '',
  operationsJson: JSON.stringify([
    { operation: 'normalize', parameters: { scale: '0-1' } },
    { operation: 'augment.cutmix', parameters: { ratio: 0.3 } }
  ], null, 2)
})

const datasetId = ref('')
const pipelineResult = ref<unknown>(null)

async function register() {
  try {
    const res = await registerDataset({
      datasetName: datasetForm.value.datasetName,
      assets: JSON.parse(datasetForm.value.assetsJson),
      filterRules: JSON.parse(datasetForm.value.filterRules || '{}')
    })
    datasetId.value = (res.data as any).datasetId
    pipelineForm.value.datasetId = datasetId.value
    alert('数据集注册成功')
  } catch (e) {
    alert('注册失败')
  }
}

async function run() {
  try {
    const res = await runPipeline({
      datasetId: pipelineForm.value.datasetId,
      operations: JSON.parse(pipelineForm.value.operationsJson)
    })
    pipelineResult.value = res.data
    alert('流水线执行成功')
  } catch (e) {
    alert('执行失败')
  }
}
</script>
