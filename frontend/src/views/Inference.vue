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
          <span class="info-label">Adapter：</span>
          <span class="info-value">{{ selectedModel.adapterType || '未配置' }}</span>
        </div>
      </div>

      <div class="form-group">
        <label>模态</label>
        <select v-model="form.modality" class="form-control" @change="onModalityChange">
          <option v-for="m in (selectedModel?.supportedModalities || ['image', 'audio', 'video', 'text'])" :key="m" :value="m">
            {{ m }}
          </option>
        </select>
      </div>

      <div class="input-source-panel">
        <div class="input-source-heading">
          <div>
            <strong>服务器测试数据</strong>
            <p>客户端只选择数据集和样本，文件位置由服务器内部解析。</p>
          </div>
          <button type="button" class="btn btn-outline" @click="loadDatasets">刷新数据集</button>
        </div>

        <div class="dataset-row">
          <div class="form-group">
            <label>选择数据集</label>
            <select v-model="selectedDatasetId" class="form-control" @change="loadSamples">
              <option value="">请选择服务器数据集</option>
              <option v-for="dataset in compatibleDatasets" :key="dataset.datasetId" :value="dataset.datasetId">
                {{ dataset.datasetName }}（{{ dataset.assetCount }} 个样本）
              </option>
            </select>
          </div>
          <div class="dataset-status">
            服务器共有 {{ datasets.length }} 个数据集，当前模态可用 {{ compatibleDatasets.length }} 个
          </div>
        </div>

        <div class="quick-upload">
          <label>服务器暂无所需数据？可选择包含同名图片和标签的数据集文件夹</label>
          <div class="quick-upload-row">
            <input v-model="uploadDatasetName" class="form-control" placeholder="新数据集名称" />
            <input ref="uploadInput" type="file" webkitdirectory directory multiple @change="onUploadFilesChange" />
            <button type="button" class="btn btn-success" :disabled="uploading || uploadFiles.length === 0" @click="uploadFromInference">
              {{ uploading ? '上传中...' : `上传 ${uploadFiles.length || ''} 个文件` }}
            </button>
          </div>
        </div>

        <div v-if="loadingSamples" class="empty-samples">正在从服务器读取样本……</div>
        <div v-else-if="samples.length" class="sample-picker">
          <label
            v-for="sample in samples"
            :key="sample.sampleId"
            class="sample-option"
            :class="{ selected: selectedSampleIds.includes(sample.sampleId) }"
          >
            <input v-model="selectedSampleIds" type="checkbox" :value="sample.sampleId" />
            <img v-if="sample.dataType === 'image'" :src="sample.contentUrl" :alt="sample.name" />
            <div v-else class="sample-placeholder">{{ sample.dataType }}</div>
            <span :title="sample.name">{{ sample.name }}</span>
            <small>{{ formatBytes(sample.fileSize) }}</small>
            <small class="label-name" :title="sample.labelRelativePath">
              标签：{{ sample.labelOriginalName }}
            </small>
          </label>
        </div>
        <div v-else class="empty-samples">
          {{ selectedDatasetId ? '该数据集没有当前模态的样本' : '请选择数据集，或上传新数据集' }}
        </div>
        <div class="selection-summary">已选择 {{ selectedSampleIds.length }} 个样本</div>
      </div>

      <div class="form-group">
        <div class="metric-heading">
          <label>评测指标</label>
          <span>已启用 {{ selectedMetrics.length }} / {{ metricOptions.length }}</span>
        </div>
        <div class="metric-selector">
          <button
            v-for="metric in metricOptions"
            :key="metric.id"
            type="button"
            class="metric-toggle"
            :class="{ active: selectedMetrics.includes(metric.id) }"
            :title="metric.description"
            :aria-pressed="selectedMetrics.includes(metric.id)"
            @click="toggleMetric(metric.id)"
          >
            {{ metric.label }}
          </button>
        </div>
        <div class="metric-actions">
          <button type="button" @click="selectAllMetrics">全选</button>
          <button type="button" @click="selectedMetrics = []">清空</button>
        </div>
      </div>

      <div class="form-group">
        <label>选项 (JSON)</label>
        <input v-model="form.options" class="form-control" placeholder='{"batchSize":1}' />
      </div>

      <div class="button-group">
        <button class="btn btn-primary" @click="submit" :disabled="isRunning || selectedSampleIds.length === 0">
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import {
  getModels,
  runInference,
  listDatasets,
  listDatasetSamples,
  uploadDataset,
  type DatasetSummary,
  type SampleSummary
} from '../services/api'
import { wsService } from '../services/websocket'
import { metricsForModel } from '../constants/metricCatalog'

const route = useRoute()
const models = ref<any[]>([])
const selectedModelId = ref('yolov8-detection')
const isRunning = ref(false)
const result = ref<any>(null)
const selectedMetrics = ref<string[]>([])
const datasets = ref<DatasetSummary[]>([])
const samples = ref<SampleSummary[]>([])
const selectedDatasetId = ref('')
const selectedSampleIds = ref<string[]>([])
const loadingSamples = ref(false)
const uploading = ref(false)
const uploadDatasetName = ref('')
const uploadFiles = ref<File[]>([])
const uploadInput = ref<HTMLInputElement | null>(null)
let removeCatalogListener: (() => boolean | void) | undefined

const form = ref({
  modelId: 'yolov8-detection',
  modality: 'image',
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

const metricOptions = computed(() => metricsForModel(selectedModel.value))
const compatibleDatasets = computed(() => datasets.value.filter(dataset =>
  !dataset.supportedModalities?.length || dataset.supportedModalities.includes(form.value.modality)
))

async function loadModels() {
  try {
    const res = await getModels()
    models.value = res as any[]
    if (route.query.modelId) {
      selectedModelId.value = route.query.modelId as string
    } else if (!models.value.some(model => model.modelId === selectedModelId.value) && models.value.length > 0) {
      selectedModelId.value = models.value[0].modelId
    }
    onModelChange()
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
}

async function loadDatasets() {
  try {
    datasets.value = await listDatasets()
    if (!compatibleDatasets.value.some(dataset => dataset.datasetId === selectedDatasetId.value)) {
      selectedDatasetId.value = compatibleDatasets.value[0]?.datasetId || ''
    }
    await loadSamples()
  } catch (e) {
    console.error('加载服务器数据集失败', e)
  }
}

async function loadSamples() {
  selectedSampleIds.value = []
  samples.value = []
  if (!selectedDatasetId.value) return
  loadingSamples.value = true
  try {
    const allSamples = await listDatasetSamples(selectedDatasetId.value)
    samples.value = allSamples.filter(sample => sample.dataType === form.value.modality)
  } catch (e) {
    console.error('加载服务器样本失败', e)
  } finally {
    loadingSamples.value = false
  }
}

function onModalityChange() {
  if (!compatibleDatasets.value.some(dataset => dataset.datasetId === selectedDatasetId.value)) {
    selectedDatasetId.value = compatibleDatasets.value[0]?.datasetId || ''
  }
  loadSamples()
}

function onUploadFilesChange(event: Event) {
  uploadFiles.value = Array.from((event.target as HTMLInputElement).files || [])
  if (!uploadDatasetName.value && uploadFiles.value.length) {
    const firstPath = (uploadFiles.value[0] as File & { webkitRelativePath?: string }).webkitRelativePath || ''
    uploadDatasetName.value = firstPath.split('/')[0] || `推理数据-${new Date().toLocaleString()}`
  }
}

async function uploadFromInference() {
  if (!uploadDatasetName.value.trim() || uploadFiles.value.length === 0) {
    alert('请填写数据集名称并选择文件')
    return
  }
  uploading.value = true
  try {
    const dataset = await uploadDataset(uploadDatasetName.value.trim(), uploadFiles.value, ['inference-upload'])
    await loadDatasets()
    selectedDatasetId.value = dataset.datasetId
    await loadSamples()
    uploadFiles.value = []
    uploadDatasetName.value = ''
    if (uploadInput.value) uploadInput.value.value = ''
  } catch (e: any) {
    const detail = e?.response?.data?.message || e?.message || '未知错误'
    alert(`上传失败：${detail}`)
    console.error('推理数据集上传失败', e)
  } finally {
    uploading.value = false
  }
}

function onModelChange() {
  const model = selectedModel.value
  if (model) {
    form.value.modelId = model.modelId
    const modelDefaults = new Set((model.availableMetrics || []).map((item: string) => item.toLowerCase()))
    selectedMetrics.value = metricOptions.value
      .filter(metric => metric.defaultEnabled || modelDefaults.has(metric.id.toLowerCase()))
      .map(metric => metric.id)
    if (model.supportedModalities?.length > 0) {
      form.value.modality = model.supportedModalities[0]
    }
    onModalityChange()
  }
}

function toggleMetric(metricId: string) {
  selectedMetrics.value = selectedMetrics.value.includes(metricId)
    ? selectedMetrics.value.filter(item => item !== metricId)
    : [...selectedMetrics.value, metricId]
}

function selectAllMetrics() {
  selectedMetrics.value = metricOptions.value.map(metric => metric.id)
}

function switchModel(modelId: string) {
  selectedModelId.value = modelId
  onModelChange()
}

async function submit() {
  if (selectedMetrics.value.length === 0) {
    alert('请至少启用一个评测指标')
    return
  }
  if (selectedSampleIds.value.length === 0) {
    alert('请至少选择一个服务器样本')
    return
  }
  isRunning.value = true
  try {
    const data = {
      modelId: form.value.modelId,
      modality: form.value.modality,
      inputs: selectedSampleIds.value.map(sampleId => ({
        inputId: sampleId,
        sampleId,
        attributes: { datasetId: selectedDatasetId.value }
      })),
      requestedMetrics: selectedMetrics.value,
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

function formatBytes(size?: number) {
  if (size == null) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function getConfidenceClass(confidence: number) {
  if (confidence >= 0.8) return 'confidence-high'
  if (confidence >= 0.6) return 'confidence-medium'
  return 'confidence-low'
}

onMounted(async () => {
  await Promise.all([loadModels(), loadDatasets()])
  removeCatalogListener = wsService.on('DATA_CATALOG_CHANGED', () => {
    loadDatasets()
  })
})

onBeforeUnmount(() => {
  removeCatalogListener?.()
})
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

.input-source-panel {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #dfe5ec;
  border-radius: 6px;
  background: #fbfcfe;
}

.input-source-heading,
.quick-upload-row,
.dataset-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.input-source-heading {
  margin-bottom: 16px;
}

.input-source-heading p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 12px;
}

.dataset-row .form-group {
  flex: 1;
}

.dataset-status,
.selection-summary {
  color: #667085;
  font-size: 12px;
}

.quick-upload {
  padding: 12px;
  border-radius: 4px;
  background: #f1f5f9;
}

.quick-upload > label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
}

.quick-upload-row .form-control {
  flex: 1;
}

.sample-picker {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.sample-option {
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 6px;
  padding: 8px;
  border: 1px solid #dfe5ec;
  border-radius: 5px;
  background: #fff;
  cursor: pointer;
}

.sample-option.selected {
  border-color: #1677ff;
  box-shadow: 0 0 0 1px #1677ff;
}

.sample-option img,
.sample-placeholder {
  grid-column: 1 / -1;
  width: 100%;
  height: 92px;
  object-fit: cover;
  border-radius: 3px;
  background: #eef2f6;
}

.sample-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667085;
}

.sample-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sample-option small {
  grid-column: 2;
  color: #98a2b3;
}

.sample-option .label-name {
  grid-column: 1 / -1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #667085;
}

.empty-samples {
  margin-top: 14px;
  padding: 22px;
  text-align: center;
  color: #98a2b3;
  background: #fff;
  border: 1px dashed #d0d5dd;
}

.selection-summary {
  margin-top: 8px;
  text-align: right;
}

.metric-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.metric-heading label {
  margin: 0;
}

.metric-heading span {
  color: #667085;
  font-size: 12px;
}

.metric-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e4e7ec;
  background: #fafafa;
}

.metric-toggle {
  min-height: 32px;
  padding: 5px 10px;
  border: 1px solid #d0d5dd;
  border-radius: 4px;
  color: #475467;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: color .15s, border-color .15s, background .15s;
}

.metric-toggle:hover {
  border-color: #84adff;
  color: #175cd3;
}

.metric-toggle.active {
  border-color: #1677ff;
  color: #fff;
  background: #1677ff;
}

.metric-actions {
  display: flex;
  gap: 12px;
  margin-top: 7px;
}

.metric-actions button {
  padding: 0;
  border: 0;
  color: #1677ff;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
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
