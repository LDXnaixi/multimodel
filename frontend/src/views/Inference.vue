<template>
  <div>
    <h2 style="margin-bottom: 20px;">模型推理</h2>
    <div class="card">
      <div class="form-group">
        <label>模型ID</label>
        <input v-model="form.modelId" class="form-control" placeholder="例如: yolov8-demo" />
      </div>
      <div class="form-group">
        <label>模态</label>
        <select v-model="form.modality" class="form-control">
          <option value="image">图像 (image)</option>
          <option value="audio">音频 (audio)</option>
          <option value="video">视频 (video)</option>
          <option value="text">文本 (text)</option>
        </select>
      </div>
      <div class="form-group">
        <label>输入数据 (JSON)</label>
        <textarea v-model="form.inputsJson" class="form-control" rows="6"></textarea>
      </div>
      <div class="form-group">
        <label>评测指标</label>
        <input v-model="form.metrics" class="form-control" placeholder="逗号分隔，如: mAP,Precision,Recall" />
      </div>
      <div class="form-group">
        <label>选项 (JSON)</label>
        <input v-model="form.options" class="form-control" placeholder='{"batchSize":1}' />
      </div>
      <button class="btn btn-primary" @click="submit">执行推理</button>
    </div>

    <div v-if="result" class="card">
      <div class="card-title">推理结果</div>
      <pre style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(result, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { runInference } from '../services/api'

const form = ref({
  modelId: 'yolov8-demo',
  modality: 'image',
  inputsJson: JSON.stringify([
    {
      inputId: 'img-001',
      sourceUri: 'samples/car.png',
      attributes: { scene: 'factory' }
    }
  ], null, 2),
  metrics: 'mAP,Precision,Recall',
  options: '{"batchSize":1}'
})

const result = ref<unknown>(null)

async function submit() {
  try {
    const data = {
      modelId: form.value.modelId,
      modality: form.value.modality,
      inputs: JSON.parse(form.value.inputsJson),
      requestedMetrics: form.value.metrics.split(',').map(s => s.trim()),
      options: JSON.parse(form.value.options || '{}')
    }
    const res = await runInference(data)
    result.value = res.data
    alert('推理请求已发送，结果将同步推送至 WebSocket')
  } catch (e) {
    alert('请求失败，请检查输入格式')
  }
}
</script>
