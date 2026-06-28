<template>
  <div class="inference-container">
    <h2>模型推理</h2>
    
    <div class="card">
      <div class="form-group">
        <label>选择模型</label>
        <select v-model="selectedModelId" class="form-control" @change="onModelChange">
          <option v-for="model in models" :key="model.modelId" :value="model.modelId">
            {{ model.modelName }} ({{ model.modelCategory }})
          </option>
        </select>
      </div>
      
      <div v-if="selectedModel" class="model-info-panel">
        <div class="info-row">
          <span class="info-label">版本：</span>
          <span class="info-value">{{ selectedModel.version }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">支持模态：</span>
          <span class="info-value">{{ selectedModel.supportedModalities?.join(', ') }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">评测指标：</span>
          <span class="info-value">{{ selectedModel.availableMetrics?.join(', ') }}</span>
        </div>
      </div>

      <div class="form-group">
        <label>模态</label>
        <select v-model="form.modality" class="form-control">
          <option v-for="m in (selectedModel?.supportedModalities || ['image', 'audio', 'video', 'text'])" :key="m" :value="m">
            {{ m }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>输入数据 (JSON)</label>
        <textarea v-model="form.inputsJson" class="form-control" rows="6"></textarea>
      </div>

      <div class="form-group">
        <label>评测指标</label>
        <input v-model="form.metrics" class="form-control" :placeholder="selectedModel?.availableMetrics?.join(', ') || 'mAP,Precision,Recall'" />
      </div>

      <div class="form-group">
        <label>选项 (JSON)</label>
        <input v-model="form.options" class="form-control" placeholder='{"batchSize":1}' />
      </div>

      <div class="button-group">
        <button class="btn btn-primary" @click="submit" :disabled="isRunning">
          {{ isRunning ? '推理中...' : '执行推理' }}
        </button>
        
        <!-- 视觉语言模型动态切换测试 -->
        <div v-if="isVisionLanguageModel" class="compare-section">
          <label class="compare-label">动态切换测试：</label>
          <div class="vl-buttons">
            <button 
              v-for="vlModel in vlModels" 
              :key="vlModel.modelId"
              class="btn"
              :class="{'btn-outline': selectedModelId !== vlModel.modelId, 'btn-primary': selectedModelId === vlModel.modelId}"
              @click="switchModel(vlModel.modelId)"
            >
              {{ vlModel.modelName }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 推理结果 -->
    <div v-if="result" class="card">
      <div class="card-title">推理结果</div>
      
      <div class="result-summary">
        <div class="summary-item">
          <span class="summary-label">任务ID</span>
          <span class="summary-value">{{ result.jobId }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">耗时</span>
          <span class="summary-value">{{ result.durationMs }}ms</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">状态</span>
          <span class="summary-value">{{ result.status }}</span>
        </div>
      </div>

      <!-- 评测指标展示 -->
      <div v-if="result.metrics && result.metrics.length > 0" class="metrics-section">
        <div class="section-title">评测指标</div>
        <div class="metrics-grid">
          <div v-for="metric in result.metrics" :key="metric.name" class="metric-card">
            <div class="metric-name">{{ metric.name }}</div>
            <div class="metric-value">{{ metric.value }}</div>
            <div class="metric-unit">{{ metric.unit }}</div>
            <div class="metric-desc">{{ metric.description }}</div>
          </div>
        </div>
      </div>

      <!-- 输出结果 -->
      <div v-if="result.outputs && result.outputs.length > 0" class="outputs-section">
        <div class="section-title">输出详情</div>
        <div class="outputs-list">
          <div v-for="output in result.outputs" :key="output.inputId" class="output-item">
            <div class="output-header">
              <span class="input-id">{{ output.inputId }}</span>
              <span class="confidence-badge" :class="getConfidenceClass(output.confidence)">
                {{ (output.confidence * 100).toFixed(1) }}%
              </span>
            </div>
            <div class="output-label">{{ output.label }}</div>
            <div v-if="output.extra" class="output-extra">
              <pre>{{ JSON.stringify(output.extra, null, 2) }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getModels, runInference } from '../services/api'

const route = useRoute()
const models = ref<any[]>([])
const selectedModelId = ref('yolov8-detection')
const isRunning = ref(false)
const result = ref<any>(null)

const form = ref({
  modelId: 'yolov8-detection',
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

const selectedModel = computed(() => {
  return models.value.find(m => m.modelId === selectedModelId.value)
})

const vlModels = computed(() => {
  return models.value.filter(m => m.modelCategory === 'VISION_LANGUAGE')
})

const isVisionLanguageModel = computed(() => {
  return selectedModel.value?.modelCategory === 'VISION_LANGUAGE'
})

async function loadModels() {
  try {
    const res = await getModels()
    models.value = res as any[]
    if (route.query.modelId) {
      selectedModelId.value = route.query.modelId as string
      onModelChange()
    }
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
}

function onModelChange() {
  const model = selectedModel.value
  if (model) {
    form.value.modelId = model.modelId
    form.value.metrics = model.availableMetrics?.join(', ') || ''
    if (model.supportedModalities?.length > 0) {
      form.value.modality = model.supportedModalities[0]
    }
  }
}

function switchModel(modelId: string) {
  selectedModelId.value = modelId
  onModelChange()
}

async function submit() {
  isRunning.value = true
  try {
    const data = {
      modelId: form.value.modelId,
      modality: form.value.modality,
      inputs: JSON.parse(form.value.inputsJson),
      requestedMetrics: form.value.metrics.split(',').map(s => s.trim()),
      options: JSON.parse(form.value.options || '{}')
    }
    const res = await runInference(data)
    result.value = res
  } catch (e) {
    alert('请求失败，请检查输入格式')
  } finally {
    isRunning.value = false
  }
}

function getConfidenceClass(confidence: number) {
  if (confidence >= 0.8) return 'confidence-high'
  if (confidence >= 0.6) return 'confidence-medium'
  return 'confidence-low'
}

onMounted(loadModels)
</script>

<style scoped>
.inference-container {
  padding: 10px;
}

.model-info-panel {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.info-row {
  margin-bottom: 8px;
}

.info-label {
  font-weight: 500;
  color: #666;
  margin-right: 8px;
}

.info-value {
  color: #333;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.compare-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.compare-label {
  font-weight: 500;
  color: #666;
}

.vl-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-outline {
  background: transparent;
  color: #1890ff;
  border: 1px solid #1890ff;
}

.btn-outline:hover {
  background: #e6f7ff;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f6f8fa;
  border-radius: 4px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e0e0e0;
}

.metrics-section {
  margin-bottom: 24px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.metric-card {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 4px;
  text-align: center;
}

.metric-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 4px;
}

.metric-unit {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.metric-desc {
  font-size: 11px;
  color: #999;
  line-height: 1.4;
}

.outputs-section {
  margin-bottom: 16px;
}

.outputs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.output-item {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 4px;
}

.output-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.input-id {
  font-weight: 500;
  color: #333;
}

.confidence-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.confidence-high {
  background: #d4edda;
  color: #155724;
}

.confidence-medium {
  background: #fff3cd;
  color: #856404;
}

.confidence-low {
  background: #f8d7da;
  color: #721c24;
}

.output-label {
  color: #666;
  margin-bottom: 8px;
}

.output-extra pre {
  background: #fff;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
}
</style>
