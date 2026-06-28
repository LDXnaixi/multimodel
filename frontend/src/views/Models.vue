<template>
  <div class="models-container">
    <div class="header-actions">
      <h2>模型管理</h2>
      <div class="upload-section">
        <input 
          type="file" 
          ref="fileInput" 
          style="display: none" 
          accept=".onnx" 
          @change="handleFileChange"
        />
        <button class="btn btn-primary" @click="triggerFileInput">
          <i class="icon-upload"></i> 读取 ONNX 模型
        </button>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-section">
      <span class="filter-label">分类筛选：</span>
      <div class="filter-buttons">
        <button 
          v-for="cat in categories" 
          :key="cat"
          class="btn"
          :class="{'btn-primary': selectedCategory === cat, 'btn-outline': selectedCategory !== cat}"
          @click="selectedCategory = cat"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <!-- 正在解析的提示 -->
    <div v-if="isAnalyzing" class="alert alert-info">
      正在解析模型文件: {{ selectedFileName }}...
    </div>

    <!-- 解析结果展示 -->
    <div v-if="analyzedModel" class="card result-card">
      <h3>新解析模型详情</h3>
      <div class="model-info-grid">
        <div class="info-item">
          <label>模型ID:</label> <span>{{ analyzedModel.modelId }}</span>
        </div>
        <div class="info-item">
          <label>模型名称:</label> <span>{{ analyzedModel.modelName }}</span>
        </div>
        <div class="info-item">
          <label>算法类型:</label> <span>{{ analyzedModel.algorithmType }}</span>
        </div>
        <div class="info-item">
          <label>部署状态:</label> <span>{{ analyzedModel.deploymentStatus }}</span>
        </div>
        <div class="info-item">
          <label>调用次数:</label> <span>{{ analyzedModel.invocationCount }}</span>
        </div>
        <div class="info-item">
          <label>平均耗时:</label> <span>{{ analyzedModel.averageLatency }}ms</span>
        </div>
      </div>
      <div style="margin-top: 15px;">
        <button class="btn btn-success" @click="clearAnalysis">确认并添加至列表</button>
      </div>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>模型ID</th>
            <th>名称</th>
            <th>分类</th>
            <th>模态</th>
            <th>版本</th>
            <th>调用次数</th>
            <th>平均耗时</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="model in filteredModels" :key="model.modelId">
            <td>{{ model.modelId }}</td>
            <td>
              <span class="model-name">
                {{ model.modelName }}
                <span v-if="model.isCustom" class="badge badge-info custom-badge">自定义</span>
              </span>
            </td>
            <td><span class="category-badge">{{ getCategoryName(model.modelCategory) }}</span></td>
            <td>
              <span v-for="m in model.supportedModalities" :key="m" class="badge badge-info" style="margin-right: 5px;">
                {{ m }}
              </span>
            </td>
            <td>{{ model.version }}</td>
            <td>{{ model.invocationCount ?? '-' }}</td>
            <td>{{ model.averageLatency ? (model.averageLatency + 'ms') : '-' }}</td>
            <td>
              <span class="badge" :class="getStatusClass(model.deploymentStatus)">
                {{ model.deploymentStatus }}
              </span>
            </td>
            <td>
              <button class="btn btn-small btn-primary" @click="goToInference(model.modelId)">测试</button>
            </td>
          </tr>
          <tr v-if="filteredModels.length === 0">
            <td colspan="9" style="text-align: center; color: #999;">暂无模型数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getModels, analyzeModel } from '../services/api'

const router = useRouter()
const models = ref<any[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const isAnalyzing = ref(false)
const selectedFileName = ref('')
const analyzedModel = ref<any>(null)
const selectedCategory = ref('全部')
const categories = ref(['全部', 'OBJECT_DETECTION', 'OCR', 'IMAGE_CLASSIFICATION', 'SEMANTIC_ANALYSIS', 'SPEECH_RECOGNITION', 'VISION_LANGUAGE', 'CUSTOM'])

const categoryNames: Record<string, string> = {
  'OBJECT_DETECTION': '目标检测',
  'OCR': '文本识别',
  'IMAGE_CLASSIFICATION': '图像分类',
  'SEMANTIC_ANALYSIS': '语义分析',
  'SPEECH_RECOGNITION': '语音识别',
  'VISION_LANGUAGE': '视觉语言',
  'CUSTOM': '自定义模型',
  'UNCLASSIFIED': '未分类'
}

const filteredModels = computed(() => {
  if (selectedCategory.value === '全部') return models.value
  return models.value.filter(m => m.modelCategory === selectedCategory.value)
})

async function loadModels() {
  try {
    const res = await getModels()
    models.value = res as any[]
  } catch (e) {
    console.error('获取模型列表失败', e)
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  selectedFileName.value = file.name
  isAnalyzing.value = true
  analyzedModel.value = null

  try {
    const fileInfo = {
      fileName: file.name,
      fileSize: file.size,
      lastModified: new Date(file.lastModified).toISOString()
    }

    const res = await analyzeModel(fileInfo)
    analyzedModel.value = res
    
    await new Promise(resolve => setTimeout(resolve, 1000))
  } catch (e) {
    console.error('分析模型失败', e)
    alert('分析模型失败')
  } finally {
    isAnalyzing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function clearAnalysis() {
  if (analyzedModel.value) {
    models.value.unshift(analyzedModel.value)
    analyzedModel.value = null
  }
}

function getCategoryName(category: string) {
  return categoryNames[category] || category
}

function getStatusClass(status: string) {
  if (!status) return 'badge-success'
  status = status.toUpperCase()
  if (status === 'RUNNING' || status === 'AVAILABLE') return 'badge-success'
  if (status === 'ANALYZED') return 'badge-info'
  return 'badge-warning'
}

function goToInference(modelId: string) {
  router.push({ path: '/inference', query: { modelId } })
}

onMounted(loadModels)
</script>

<style scoped>
.models-container {
  padding: 10px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.upload-section {
  display: flex;
  gap: 10px;
}

.filter-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-weight: 500;
  color: #666;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-small {
  padding: 4px 8px;
  font-size: 12px;
}

.btn-outline {
  background: transparent;
  color: #1890ff;
  border: 1px solid #1890ff;
}

.btn-outline:hover {
  background: #e6f7ff;
}

.result-card {
  border: 1px solid #e0e0e0;
  background-color: #f9f9f9;
  padding: 15px;
  margin-bottom: 20px;
}

.model-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.info-item label {
  font-weight: bold;
  color: #666;
  margin-right: 5px;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.custom-badge {
  font-size: 10px;
}

.category-badge {
  display: inline-block;
  padding: 2px 6px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.alert-info {
  padding: 10px;
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
  border-radius: 4px;
  margin-bottom: 20px;
}
</style>
