<template>
  <div class="data-management">
    <h2>测试数据管理平台</h2>

    <div class="tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        class="tab-button"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>

    <div class="tab-content">
      <!-- 数据源管理 -->
      <div v-if="activeTab === 'datasources'" class="card">
        <h3>数据源管理</h3>
        <div class="dropzone" @drop="handleDrop" @dragover.prevent>
          <div v-if="!hasFiles" class="dropzone-content">
            <p>拖拽文件到此处，或点击选择文件</p>
            <button class="btn btn-secondary" @click="triggerFileInput">选择文件</button>
            <input type="file" ref="fileInput" @change="handleFileSelect" multiple style="display: none;" />
          </div>
          <div v-else class="file-list">
            <div v-for="(file, index) in files" :key="index" class="file-item">
              <span>{{ file.name }}</span>
              <button class="btn btn-small btn-danger" @click="removeFile(index)">删除</button>
            </div>
          </div>
        </div>
        
        <div class="form-group" style="margin-top: 20px;">
          <label>数据源名称</label>
          <input v-model="datasourceForm.name" class="form-control" placeholder="数据源名称" />
        </div>
        <div class="form-group">
          <label>数据源类型</label>
          <select v-model="datasourceForm.type" class="form-control">
            <option value="local">本地文件</option>
            <option value="database">数据库</option>
            <option value="api">API接口</option>
            <option value="s3">对象存储</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="handleAddDataSource">添加数据源</button>
        
        <div style="margin-top: 30px;">
          <h4>已连接数据源</h4>
          <div v-if="dataSources.length > 0" class="data-source-list">
            <div v-for="ds in dataSources" :key="ds.sourceId" class="data-source-item">
              <span class="name">{{ ds.name }}</span>
              <span class="type">{{ ds.type }}</span>
              <span class="status" :class="ds.status?.toLowerCase()">{{ ds.status }}</span>
            </div>
          </div>
          <p v-else style="color: #999;">暂无数据源</p>
        </div>
      </div>

      <!-- 样本库管理 -->
      <div v-if="activeTab === 'samples'" class="card">
        <h3>样本库管理</h3>
        
        <div class="search-section">
          <div class="form-group">
            <label>关键词</label>
            <input v-model="searchForm.keyword" class="form-control" placeholder="搜索样本" />
          </div>
          <div class="form-group">
            <label>数据类型</label>
            <select v-model="searchForm.dataType" class="form-control">
              <option value="">全部</option>
              <option value="image">图像</option>
              <option value="audio">音频</option>
              <option value="text">文本</option>
              <option value="sensor">传感器</option>
            </select>
          </div>
          <div class="form-group">
            <label>标签</label>
            <input v-model="searchForm.tags" class="form-control" placeholder="用逗号分隔" />
          </div>
          <button class="btn btn-primary" @click="handleSearchSamples">搜索</button>
        </div>

        <div style="margin-top: 30px;">
          <h4>搜索结果</h4>
          <div v-if="samples.length > 0" class="sample-grid">
            <div v-for="sample in samples" :key="sample.sampleId" class="sample-card">
              <div class="sample-header">
                <span class="sample-name">{{ sample.name }}</span>
                <span class="sample-type">{{ sample.dataType }}</span>
              </div>
              <div class="sample-tags">
                <span v-for="tag in sample.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>
              <div class="sample-meta">
                <span>版本: {{ sample.version }}</span>
              </div>
            </div>
          </div>
          <p v-else style="color: #999;">暂无样本</p>
        </div>
      </div>

      <!-- 数据处理 -->
      <div v-if="activeTab === 'processing'" class="card">
        <h3>数据处理</h3>
        
        <div class="form-group">
          <label>样本ID</label>
          <input v-model="processingForm.sampleId" class="form-control" placeholder="输入样本ID" />
        </div>
        
        <div class="form-group">
          <label>处理步骤</label>
          <div class="step-builder">
            <div v-for="(step, index) in processingForm.steps" :key="index" class="step-item">
              <select v-model="step.type" class="form-control">
                <option value="cleaning">数据清洗</option>
                <option value="denoising">去噪</option>
                <option value="normalization">标准化</option>
                <option value="format_conversion">格式转换</option>
              </select>
              <button class="btn btn-small btn-danger" @click="removeStep(index)">删除</button>
            </div>
            <button class="btn btn-secondary" @click="addStep">添加步骤</button>
          </div>
        </div>
        
        <button class="btn btn-primary" @click="handleProcessData">执行处理</button>
        
        <div v-if="processingResult" class="result-section">
          <h4>处理结果</h4>
          <pre>{{ JSON.stringify(processingResult, null, 2) }}</pre>
        </div>
      </div>

      <!-- 数据增强 -->
      <div v-if="activeTab === 'augmentation'" class="card">
        <h3>数据增强</h3>
        
        <div class="form-group">
          <label>样本ID列表</label>
          <textarea v-model="augmentationForm.sampleIds" class="form-control" rows="3" placeholder="用逗号分隔"></textarea>
        </div>
        
        <div class="form-group">
          <label>数据类型</label>
          <select v-model="augmentationForm.dataType" class="form-control">
            <option value="image">图像</option>
            <option value="text">文本</option>
            <option value="audio">音频</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>增强方法</label>
          <div class="method-selector">
            <label v-for="method in availableMethods" :key="method" class="checkbox-label">
              <input type="checkbox" :value="method" v-model="augmentationForm.methods" />
              {{ method }}
            </label>
          </div>
        </div>
        
        <div class="form-group">
          <label>增强倍数</label>
          <input v-model.number="augmentationForm.factor" type="number" class="form-control" min="1" />
        </div>
        
        <button class="btn btn-primary" @click="handleAugmentData">执行增强</button>
        
        <div v-if="augmentationResult" class="result-section">
          <h4>增强结果</h4>
          <p>生成了 {{ augmentationResult.length }} 个增强样本</p>
        </div>
      </div>

      <!-- 数据融合 -->
      <div v-if="activeTab === 'fusion'" class="card">
        <h3>数据融合</h3>
        
        <div class="form-group">
          <label>待融合样本ID</label>
          <textarea v-model="fusionForm.sampleIds" class="form-control" rows="3" placeholder="用逗号分隔"></textarea>
        </div>
        
        <div class="form-group">
          <label>融合策略</label>
          <select v-model="fusionForm.strategy" class="form-control">
            <option value="timestamp">时间戳对齐</option>
            <option value="event">事件关联</option>
            <option value="spatial">空间对齐</option>
          </select>
        </div>
        
        <button class="btn btn-primary" @click="handleFuseData">执行融合</button>
        
        <div v-if="fusionResult" class="result-section">
          <h4>融合结果</h4>
          <p>生成了 {{ fusionResult.length }} 个融合样本</p>
        </div>
      </div>

      <!-- 场景生成 -->
      <div v-if="activeTab === 'scenarios'" class="card">
        <h3>场景生成</h3>
        
        <div class="form-group">
          <label>场景描述</label>
          <textarea v-model="scenarioForm.description" class="form-control" rows="4" placeholder="描述您想要生成的测试场景"></textarea>
        </div>
        
        <div class="form-group">
          <label>目标样本数量</label>
          <input v-model.number="scenarioForm.targetCount" type="number" class="form-control" min="1" />
        </div>
        
        <div class="form-group">
          <label>基础数据集（可选）</label>
          <input v-model="scenarioForm.baseDatasetId" class="form-control" placeholder="数据集ID" />
        </div>
        
        <button class="btn btn-primary" @click="handleGenerateScenario">生成场景</button>
        
        <div v-if="scenarioResult" class="result-section">
          <h4>场景生成结果</h4>
          <p>生成了 {{ scenarioResult.length }} 个场景样本</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import * as api from '../services/api'

const tabs = [
  { id: 'datasources', name: '数据源管理' },
  { id: 'samples', name: '样本库管理' },
  { id: 'processing', name: '数据处理' },
  { id: 'augmentation', name: '数据增强' },
  { id: 'fusion', name: '数据融合' },
  { id: 'scenarios', name: '场景生成' }
]

const activeTab = ref('datasources')
const fileInput = ref<HTMLInputElement | null>(null)
const files = ref<File[]>([])

const hasFiles = computed(() => files.value.length > 0)

const datasourceForm = ref({
  name: '',
  type: 'local'
})

const searchForm = ref({
  keyword: '',
  dataType: '',
  tags: ''
})

const processingForm = ref({
  sampleId: '',
  steps: [{ type: 'cleaning' }]
})

const augmentationForm = ref({
  sampleIds: '',
  dataType: 'image',
  methods: [] as string[],
  factor: 2
})

const fusionForm = ref({
  sampleIds: '',
  strategy: 'timestamp'
})

const scenarioForm = ref({
  description: '',
  targetCount: 10,
  baseDatasetId: ''
})

const dataSources = ref<any[]>([])
const samples = ref<any[]>([])
const processingResult = ref<any>(null)
const augmentationResult = ref<any[]>([])
const fusionResult = ref<any[]>([])
const scenarioResult = ref<any[]>([])

const availableMethods = computed(() => {
  switch (augmentationForm.value.dataType) {
    case 'image':
      return ['crop', 'flip', 'noise_injection', 'color_transform', 'cutout', 'mixup', 'cutmix']
    case 'text':
      return ['synonym_replacement', 'pronoun_replacement', 'sentence_transform', 'back_translation']
    case 'audio':
      return ['time_stretch', 'pitch_shift', 'noise_injection', 'volume_adjust']
    default:
      return []
  }
})

async function loadDataSources() {
  try {
    const res = await api.listDataSources()
    dataSources.value = res as any[]
  } catch (e) {
    console.error('加载数据源失败', e)
  }
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer?.files) {
    const droppedFiles = Array.from(e.dataTransfer.files)
    files.value = [...files.value, ...droppedFiles]
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) {
    const selectedFiles = Array.from(target.files)
    files.value = [...files.value, ...selectedFiles]
  }
}

function removeFile(index: number) {
  files.value.splice(index, 1)
}

async function handleAddDataSource() {
  try {
    await api.addDataSource({
      name: datasourceForm.value.name,
      type: datasourceForm.value.type,
      config: JSON.stringify({ files: files.value.map(f => f.name) })
    })
    alert('数据源添加成功')
    await loadDataSources()
    files.value = []
    datasourceForm.value.name = ''
  } catch (e) {
    alert('添加数据源失败')
  }
}

async function handleSearchSamples() {
  try {
    const tagsArray = searchForm.value.tags.split(',').map(t => t.trim()).filter(t => t)
    const res = await api.searchSamples({
      keyword: searchForm.value.keyword || undefined,
      dataType: searchForm.value.dataType || undefined,
      tags: tagsArray.length > 0 ? tagsArray : undefined
    })
    samples.value = res as any[]
  } catch (e) {
    alert('搜索失败')
  }
}

function addStep() {
  processingForm.value.steps.push({ type: 'cleaning' })
}

function removeStep(index: number) {
  processingForm.value.steps.splice(index, 1)
}

async function handleProcessData() {
  try {
    const res = await api.processData({
      sampleId: processingForm.value.sampleId,
      steps: processingForm.value.steps.map(step => ({ type: step.type, config: {} }))
    })
    processingResult.value = res
    alert('数据处理完成')
  } catch (e) {
    alert('处理失败')
  }
}

async function handleAugmentData() {
  try {
    const sampleIdsArray = augmentationForm.value.sampleIds.split(',').map(id => id.trim()).filter(id => id)
    const res = await api.augmentData({
      sampleIds: sampleIdsArray,
      dataType: augmentationForm.value.dataType,
      configs: augmentationForm.value.methods.map(method => ({ method, parameters: {} })),
      augmentationFactor: augmentationForm.value.factor
    })
    augmentationResult.value = res as any[]
    alert('数据增强完成')
  } catch (e) {
    alert('增强失败')
  }
}

async function handleFuseData() {
  try {
    const sampleIdsArray = fusionForm.value.sampleIds.split(',').map(id => id.trim()).filter(id => id)
    const res = await api.fuseData({
      sampleIds: sampleIdsArray,
      fusionStrategy: fusionForm.value.strategy,
      alignmentConfig: {}
    })
    fusionResult.value = res as any[]
    alert('数据融合完成')
  } catch (e) {
    alert('融合失败')
  }
}

async function handleGenerateScenario() {
  try {
    const res = await api.generateScenario({
      userDescription: scenarioForm.value.description,
      baseDatasetId: scenarioForm.value.baseDatasetId || undefined,
      targetCount: scenarioForm.value.targetCount,
      constraints: {}
    })
    scenarioResult.value = res as any[]
    alert('场景生成完成')
  } catch (e) {
    alert('生成失败')
  }
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
.data-management {
  padding: 10px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.tab-button {
  padding: 10px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  transition: all 0.2s;
}

.tab-button:hover {
  color: #1890ff;
}

.tab-button.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  font-weight: 600;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.dropzone {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  background: #fafafa;
}

.dropzone:hover {
  border-color: #1890ff;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.data-source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-source-item {
  display: flex;
  gap: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}

.data-source-item .name {
  font-weight: 500;
}

.data-source-item .status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.data-source-item .status.connected {
  background: #d4edda;
  color: #155724;
}

.search-section {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  align-items: end;
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.sample-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: white;
}

.sample-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sample-name {
  font-weight: 500;
}

.sample-type {
  font-size: 12px;
  padding: 2px 8px;
  background: #e6f7ff;
  color: #1890ff;
  border-radius: 4px;
}

.sample-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.tag {
  font-size: 12px;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
  color: #666;
}

.sample-meta {
  font-size: 12px;
  color: #999;
}

.step-builder {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  gap: 8px;
}

.method-selector {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.result-section {
  margin-top: 24px;
  padding: 16px;
  background: #f6f8fa;
  border-radius: 4px;
}

.result-section pre {
  background: white;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
}

.btn-small {
  padding: 4px 8px;
  font-size: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover {
  background: #40a9ff;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background: #ff7875;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}
</style>
