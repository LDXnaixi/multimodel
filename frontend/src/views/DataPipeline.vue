<template>
  <div class="data-page">
    <header class="page-header">
      <div>
        <h2>测试数据管理与辅助构建</h2>
        <p>数据接入、样本检索、处理、增强、融合与场景生成</p>
      </div>
      <button class="btn secondary" @click="refreshAll">刷新</button>
    </header>

    <nav class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-button"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </nav>

    <section v-if="activeTab === 'datasources'" class="panel">
      <div class="dropzone" @drop="handleDrop" @dragover.prevent>
        <div v-if="files.length === 0">
          <strong>拖拽或选择数据集文件夹</strong>
          <p>支持图片+同名标签，也支持只上传 labels/train 这类 TXT/JSON/CSV 标签或文本文件夹。</p>
          <button class="btn secondary" @click="triggerFileInput">选择文件夹</button>
          <input
            ref="fileInput"
            type="file"
            webkitdirectory
            directory
            multiple
            class="hidden-input"
            @change="handleFileSelect"
          />
        </div>
        <div v-else class="file-list">
          <div class="file-list-head">
            <span>待上传 {{ files.length }} 个文件</span>
            <button class="btn ghost" @click="files = []">清空</button>
          </div>
          <div v-for="(file, index) in files.slice(0, 12)" :key="index" class="file-row">
            <span>{{ relativeFilePath(file) }}</span>
            <button class="icon-btn" title="移除" @click="removeFile(index)">×</button>
          </div>
          <p v-if="files.length > 12" class="muted">还有 {{ files.length - 12 }} 个文件未展示</p>
        </div>
      </div>

      <div class="form-grid">
        <label>
          数据集名称
          <input v-model="datasourceForm.name" class="control" placeholder="例如 yolo-labels-lowconf" />
        </label>
        <label>
          数据源类型
          <select v-model="datasourceForm.type" class="control">
            <option value="local">本地文件</option>
            <option value="database">数据库登记</option>
            <option value="api">API 登记</option>
            <option value="s3">对象存储登记</option>
          </select>
        </label>
        <label>
          数据标签
          <input v-model="datasourceForm.tags" class="control" placeholder="逗号分隔，如 label,train,yolo" />
        </label>
      </div>
      <button class="btn primary" :disabled="uploading" @click="handleAddDataSource">
        {{ uploading ? '上传中...' : '添加数据源并上传' }}
      </button>

      <div class="section-title">
        <h3>服务器数据集</h3>
      </div>
      <div class="dataset-grid">
        <button
          v-for="dataset in datasets"
          :key="dataset.datasetId"
          class="dataset-tile"
          @click="openDataset(dataset.datasetId)"
        >
          <strong>{{ dataset.datasetName }}</strong>
          <span>{{ dataset.assetCount }} 个样本</span>
          <span>{{ dataset.supportedModalities?.join(' / ') || '未标注模态' }}</span>
          <small>{{ dataset.version }} · {{ dataset.status }}</small>
        </button>
        <p v-if="datasets.length === 0" class="muted">暂无数据集</p>
      </div>
    </section>

    <section v-if="activeTab === 'samples'" class="panel">
      <div class="form-grid">
        <label>
          所属数据集
          <select v-model="searchForm.datasetId" class="control">
            <option value="">全部数据集</option>
            <option v-for="dataset in datasets" :key="dataset.datasetId" :value="dataset.datasetId">
              {{ dataset.datasetName }}
            </option>
          </select>
        </label>
        <label>
          关键词
          <input v-model="searchForm.keyword" class="control" placeholder="文件名、标签、元数据或文本内容" />
        </label>
        <label>
          数据类型
          <select v-model="searchForm.dataType" class="control">
            <option value="">全部</option>
            <option value="image">图像</option>
            <option value="audio">音频</option>
            <option value="video">视频</option>
            <option value="text">文本</option>
            <option value="sensor">传感器</option>
          </select>
        </label>
        <label>
          事件类型
          <input v-model="searchForm.eventType" class="control" placeholder="如 A、B、E_hard" />
        </label>
        <label>
          起始时间
          <input v-model="searchForm.startTime" type="datetime-local" class="control" />
        </label>
        <label>
          结束时间
          <input v-model="searchForm.endTime" type="datetime-local" class="control" />
        </label>
        <label>
          标签内容
          <input v-model="searchForm.tags" class="control" placeholder="逗号分隔" />
        </label>
      </div>
      <div class="action-row">
        <button class="btn primary" @click="handleSearchSamples">搜索样本</button>
        <button class="btn secondary" @click="selectAllVisible">全选当前结果</button>
        <button class="btn secondary" @click="useSelectedSamples">一键使用已选样本</button>
        <button class="btn ghost" @click="clearSelectedSamples">清空选择</button>
        <span class="muted">已选 {{ selectedSampleIds.length }} 个</span>
      </div>

      <div class="sample-table">
        <div class="sample-row head">
          <span></span>
          <span>样本</span>
          <span>类型</span>
          <span>版本</span>
          <span>标签/元数据</span>
          <span>操作</span>
        </div>
        <div v-for="sample in samples" :key="sample.sampleId" class="sample-row">
          <input v-model="selectedSampleIds" type="checkbox" :value="sample.sampleId" />
          <div>
            <strong>{{ sample.name }}</strong>
            <small>{{ sample.sampleId }}</small>
          </div>
          <span>{{ sample.dataType }}</span>
          <span>{{ sample.version }}</span>
          <span class="tag-cell">
            <em v-for="tag in sample.tags" :key="tag">{{ tag }}</em>
            <small>{{ sample.metadata?.eventType || '无事件' }}</small>
          </span>
          <div class="row-actions">
            <button class="btn ghost" @click="previewSample(sample)">预览</button>
            <button class="btn ghost" @click="useOne(sample.sampleId)">使用</button>
          </div>
        </div>
        <p v-if="samples.length === 0" class="muted">暂无样本</p>
      </div>
    </section>

    <section v-if="activeTab === 'processing'" class="panel">
      <div class="form-grid">
        <label>
          样本 ID 列表
          <textarea v-model="processingForm.sampleIds" class="control" rows="3" placeholder="可从样本库勾选后填入，多个 ID 用逗号分隔"></textarea>
        </label>
        <label>
          格式转换目标
          <select v-model="processingForm.targetFormat" class="control">
            <option value="json">JSON</option>
            <option value="txt">TXT</option>
            <option value="csv">CSV</option>
            <option value="png">PNG</option>
            <option value="jpg">JPG</option>
          </select>
        </label>
      </div>
      <div class="step-list">
        <div v-for="(step, index) in processingForm.steps" :key="index" class="step-row">
          <select v-model="step.type" class="control">
            <option value="cleaning">数据清洗</option>
            <option value="denoising">去噪</option>
            <option value="standardization">标准化</option>
            <option value="normalization">归一化</option>
            <option value="format_conversion">格式转换</option>
          </select>
          <button class="icon-btn" title="删除步骤" @click="removeStep(index)">×</button>
        </div>
      </div>
      <div class="action-row">
        <button class="btn secondary" @click="addStep">添加步骤</button>
        <button class="btn secondary" @click="fillSelectedForProcessing">填入已选样本</button>
        <button class="btn primary" @click="handleProcessData">执行处理并保存新样本</button>
      </div>
      <pre v-if="processingResult" class="result">{{ JSON.stringify(processingResult, null, 2) }}</pre>
    </section>

    <section v-if="activeTab === 'augmentation'" class="panel">
      <div class="form-grid">
        <label>
          样本 ID 列表
          <textarea v-model="augmentationForm.sampleIds" class="control" rows="3"></textarea>
        </label>
        <label>
          数据类型
          <select v-model="augmentationForm.dataType" class="control">
            <option value="image">图像</option>
            <option value="text">文本</option>
            <option value="audio">音频</option>
          </select>
        </label>
        <label>
          增强倍数
          <input v-model.number="augmentationForm.factor" type="number" class="control" min="1" />
        </label>
      </div>
      <div class="method-grid">
        <label v-for="method in availableMethods" :key="method">
          <input v-model="augmentationForm.methods" type="checkbox" :value="method" />
          {{ augmentationMethodLabels[method] }}
        </label>
      </div>
      <div class="action-row">
        <button class="btn secondary" @click="fillSelectedForAugmentation">填入已选样本</button>
        <button class="btn primary" @click="handleAugmentData">执行增强并保存新样本</button>
      </div>
      <pre v-if="augmentationResult.length" class="result">{{ JSON.stringify(augmentationResult, null, 2) }}</pre>
    </section>

    <section v-if="activeTab === 'fusion'" class="panel">
      <label>
        待融合样本 ID
        <textarea v-model="fusionForm.sampleIds" class="control" rows="4"></textarea>
      </label>
      <div class="form-grid">
        <label>
          融合策略
          <select v-model="fusionForm.strategy" class="control">
            <option value="timestamp">时间戳对齐</option>
            <option value="event">事件关联</option>
            <option value="spatial">空间对齐</option>
          </select>
        </label>
      </div>
      <div class="action-row">
        <button class="btn secondary" @click="fillSelectedForFusion">填入已选样本</button>
        <button class="btn primary" @click="handleFuseData">执行融合并保存复合样本</button>
      </div>
      <pre v-if="fusionResult.length" class="result">{{ JSON.stringify(fusionResult, null, 2) }}</pre>
    </section>

    <section v-if="activeTab === 'scenarios'" class="panel">
      <label>
        场景描述
        <textarea v-model="scenarioForm.description" class="control" rows="4" placeholder="例如：低置信度、遮挡、复杂背景下的 YOLO 标签测试样本扩充"></textarea>
      </label>
      <div class="form-grid">
        <label>
          目标数量
          <input v-model.number="scenarioForm.targetCount" type="number" class="control" min="1" />
        </label>
        <label>
          基础数据集
          <select v-model="scenarioForm.baseDatasetId" class="control">
            <option value="">自动创建新数据集</option>
            <option v-for="dataset in datasets" :key="dataset.datasetId" :value="dataset.datasetId">
              {{ dataset.datasetName }}
            </option>
          </select>
        </label>
      </div>
      <button class="btn primary" @click="handleGenerateScenario">生成并保存场景样本</button>
      <pre v-if="scenarioResult.length" class="result">{{ JSON.stringify(scenarioResult, null, 2) }}</pre>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as api from '../services/api'
import { wsService } from '../services/websocket'

const tabs = [
  { id: 'datasources', name: '数据源接入' },
  { id: 'samples', name: '样本库' },
  { id: 'processing', name: '数据处理' },
  { id: 'augmentation', name: '数据增强' },
  { id: 'fusion', name: '数据融合' },
  { id: 'scenarios', name: '场景生成' }
]

const activeTab = ref('datasources')
const fileInput = ref<HTMLInputElement | null>(null)
const files = ref<File[]>([])
const uploading = ref(false)
const datasets = ref<api.DatasetSummary[]>([])
const samples = ref<api.SampleSummary[]>([])
const selectedSampleIds = ref<string[]>([])
let removeCatalogListener: (() => boolean | void) | undefined

const datasourceForm = ref({ name: '', type: 'local', tags: 'drag-upload,local' })
const searchForm = ref({ datasetId: '', keyword: '', dataType: '', eventType: '', startTime: '', endTime: '', tags: '' })
const processingForm = ref({ sampleIds: '', targetFormat: 'json', steps: [{ type: 'cleaning' }, { type: 'normalization' }] })
const augmentationForm = ref({ sampleIds: '', dataType: 'text', methods: [] as string[], factor: 1 })
const fusionForm = ref({ sampleIds: '', strategy: 'timestamp' })
const scenarioForm = ref({ description: '', targetCount: 5, baseDatasetId: '' })

const processingResult = ref<unknown>(null)
const augmentationResult = ref<unknown[]>([])
const fusionResult = ref<unknown[]>([])
const scenarioResult = ref<unknown[]>([])

const augmentationMethodLabels: Record<string, string> = {
  synonym_replacement: '同义词替换',
  pronoun_replacement: '指代替换',
  sentence_transform: '句式变换',
  back_translation: '回译',
  noise_injection: '噪声注入',
  crop: '裁剪',
  flip: '水平翻转',
  color_transform: '色彩与亮度变换',
  geometric_transform: '几何变换',
  cutout: '随机遮挡（Cutout）',
  mixup: '样本混合（Mixup）',
  cutmix: '区域混合（CutMix）',
  time_stretch: '时间拉伸',
  pitch_shift: '音调变换',
  volume_adjust: '音量调整'
}

const availableMethods = computed(() => {
  if (augmentationForm.value.dataType === 'image') {
    return ['crop', 'flip', 'noise_injection', 'color_transform', 'geometric_transform', 'cutout', 'mixup', 'cutmix']
  }
  if (augmentationForm.value.dataType === 'audio') {
    return ['noise_injection', 'time_stretch', 'pitch_shift', 'volume_adjust']
  }
  return ['synonym_replacement', 'pronoun_replacement', 'sentence_transform', 'back_translation', 'noise_injection']
})

function triggerFileInput() {
  fileInput.value?.click()
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer?.files) {
    files.value = [...files.value, ...Array.from(e.dataTransfer.files)]
    inferDatasetName()
  }
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) {
    files.value = [...files.value, ...Array.from(target.files)]
    inferDatasetName()
  }
}

function inferDatasetName() {
  if (datasourceForm.value.name || files.value.length === 0) return
  const first = relativeFilePath(files.value[0]).split('/')[0]
  datasourceForm.value.name = first || 'uploaded-dataset'
}

function removeFile(index: number) {
  files.value.splice(index, 1)
}

function relativeFilePath(file: File) {
  return (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name
}

async function refreshAll() {
  await Promise.all([loadDatasets(), handleSearchSamples()])
}

async function loadDatasets() {
  datasets.value = await api.listDatasets()
}

async function openDataset(datasetId: string) {
  activeTab.value = 'samples'
  searchForm.value.datasetId = datasetId
  scenarioForm.value.baseDatasetId = datasetId
  await handleSearchSamples()
}

async function handleAddDataSource() {
  if (!datasourceForm.value.name.trim()) {
    alert('请填写数据集名称')
    return
  }
  if (datasourceForm.value.type === 'local' && files.value.length === 0) {
    alert('请选择要上传的文件夹')
    return
  }
  uploading.value = true
  try {
    const tags = datasourceForm.value.tags.split(',').map(tag => tag.trim()).filter(Boolean)
    let dataset: api.DatasetSummary | null = null
    if (files.value.length > 0) {
      dataset = await api.uploadDataset(datasourceForm.value.name, files.value, tags)
    }
    await api.addDataSource({
      name: datasourceForm.value.name,
      type: datasourceForm.value.type,
      config: JSON.stringify({
        files: files.value.map(relativeFilePath),
        uploadMode: 'multi-modal-folder',
        datasetId: dataset?.datasetId,
        tags
      })
    })
    files.value = []
    datasourceForm.value.name = ''
    await refreshAll()
    if (dataset?.datasetId) await openDataset(dataset.datasetId)
    alert('数据源添加成功')
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleSearchSamples() {
  const tags = searchForm.value.tags.split(',').map(tag => tag.trim()).filter(Boolean)
  samples.value = await api.searchSamples({
    datasetId: searchForm.value.datasetId || undefined,
    keyword: searchForm.value.keyword || undefined,
    dataType: searchForm.value.dataType || undefined,
    eventType: searchForm.value.eventType || undefined,
    startTime: searchForm.value.startTime ? new Date(searchForm.value.startTime).toISOString() : undefined,
    endTime: searchForm.value.endTime ? new Date(searchForm.value.endTime).toISOString() : undefined,
    tags: tags.length ? tags : undefined
  }) as api.SampleSummary[]
}

function selectAllVisible() {
  selectedSampleIds.value = Array.from(new Set([...selectedSampleIds.value, ...samples.value.map(sample => sample.sampleId)]))
  useSelectedSamples()
}

function useOne(sampleId: string) {
  selectedSampleIds.value = [sampleId]
  useSelectedSamples()
}

function useSelectedSamples() {
  const ids = selectedSampleIds.value.join(',')
  processingForm.value.sampleIds = ids
  augmentationForm.value.sampleIds = ids
  fusionForm.value.sampleIds = ids
  if (selectedSampleIds.value.length > 0) {
    const first = samples.value.find(sample => sample.sampleId === selectedSampleIds.value[0])
    if (first?.datasetId) {
      scenarioForm.value.baseDatasetId = first.datasetId
    }
  }
}

function clearSelectedSamples() {
  selectedSampleIds.value = []
  processingForm.value.sampleIds = ''
  augmentationForm.value.sampleIds = ''
  fusionForm.value.sampleIds = ''
}

function previewSample(sample: api.SampleSummary) {
  window.open(sample.contentUrl, '_blank')
}

function addStep() {
  processingForm.value.steps.push({ type: 'cleaning' })
}

function removeStep(index: number) {
  processingForm.value.steps.splice(index, 1)
}

async function handleProcessData() {
  const ids = processingForm.value.sampleIds.split(',').map(id => id.trim()).filter(Boolean)
  if (ids.length === 0) {
    alert('请先选择样本')
    return
  }
  processingResult.value = await api.processData({
    sampleIds: ids,
    steps: processingForm.value.steps.map(step => ({
      type: step.type,
      config: { targetFormat: processingForm.value.targetFormat }
    }))
  })
  await handleSearchSamples()
}

function fillSelectedForProcessing() {
  processingForm.value.sampleIds = selectedSampleIds.value.join(',')
}

function fillSelectedForAugmentation() {
  augmentationForm.value.sampleIds = selectedSampleIds.value.join(',')
}

async function handleAugmentData() {
  const ids = augmentationForm.value.sampleIds.split(',').map(id => id.trim()).filter(Boolean)
  if (ids.length === 0) {
    alert('请先选择样本')
    return
  }
  augmentationResult.value = await api.augmentData({
    sampleIds: ids,
    dataType: augmentationForm.value.dataType,
    configs: augmentationForm.value.methods.map(method => ({ method, parameters: {} })),
    augmentationFactor: augmentationForm.value.factor
  }) as unknown[]
  await handleSearchSamples()
}

function fillSelectedForFusion() {
  fusionForm.value.sampleIds = selectedSampleIds.value.join(',')
}

async function handleFuseData() {
  const ids = fusionForm.value.sampleIds.split(',').map(id => id.trim()).filter(Boolean)
  if (ids.length < 2) {
    alert('融合至少需要两个样本')
    return
  }
  fusionResult.value = await api.fuseData({
    sampleIds: ids,
    fusionStrategy: fusionForm.value.strategy,
    alignmentConfig: {}
  }) as unknown[]
  await handleSearchSamples()
}

async function handleGenerateScenario() {
  scenarioResult.value = await api.generateScenario({
    userDescription: scenarioForm.value.description,
    baseDatasetId: scenarioForm.value.baseDatasetId || undefined,
    targetCount: scenarioForm.value.targetCount,
    constraints: {}
  }) as unknown[]
  await refreshAll()
}

onMounted(() => {
  refreshAll()
  removeCatalogListener = wsService.on('DATA_CATALOG_CHANGED', () => refreshAll())
})

onBeforeUnmount(() => {
  removeCatalogListener?.()
})
</script>

<style scoped>
.data-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 10px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-header h2 {
  margin: 0 0 4px;
}

.page-header p,
.muted {
  color: #667085;
  margin: 0;
}

.tabs {
  display: flex;
  gap: 6px;
  border-bottom: 1px solid #e4e7ec;
  overflow-x: auto;
}

.tab-button {
  padding: 10px 14px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #475467;
  cursor: pointer;
  white-space: nowrap;
}

.tab-button.active {
  color: #1677ff;
  border-bottom-color: #1677ff;
  font-weight: 600;
}

.panel {
  background: #fff;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 18px;
}

.dropzone {
  display: grid;
  place-items: center;
  min-height: 170px;
  border: 2px dashed #cfd7e6;
  border-radius: 8px;
  background: #f8fafc;
  text-align: center;
  margin-bottom: 16px;
  padding: 18px;
}

.hidden-input {
  display: none;
}

.file-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.file-list-head,
.file-row,
.action-row,
.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-list-head {
  justify-content: space-between;
}

.file-row {
  justify-content: space-between;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #344054;
  font-size: 13px;
  font-weight: 600;
}

.control {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #d0d5dd;
  border-radius: 6px;
  background: #fff;
  color: #101828;
  font: inherit;
  font-weight: 400;
}

textarea.control {
  resize: vertical;
}

.btn,
.icon-btn {
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.btn {
  min-height: 34px;
  padding: 7px 12px;
  font-weight: 600;
}

.btn.primary {
  background: #1677ff;
  color: #fff;
}

.btn.secondary {
  background: #eef4ff;
  color: #175cd3;
}

.btn.ghost {
  background: #f2f4f7;
  color: #344054;
}

.icon-btn {
  width: 28px;
  height: 28px;
  background: #fee4e2;
  color: #b42318;
  font-size: 18px;
  line-height: 1;
}

.section-title {
  margin-top: 22px;
}

.dataset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
}

.dataset-tile {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 12px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.dataset-tile:hover {
  border-color: #1677ff;
}

.sample-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
}

.sample-row {
  display: grid;
  grid-template-columns: 28px minmax(210px, 1.5fr) 80px 80px minmax(180px, 1fr) 150px;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
}

.sample-row.head {
  background: #f8fafc;
  color: #475467;
  font-size: 12px;
  font-weight: 700;
}

.sample-row small {
  display: block;
  color: #667085;
  margin-top: 2px;
  word-break: break-all;
}

.tag-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag-cell em {
  padding: 2px 6px;
  border-radius: 999px;
  background: #f2f4f7;
  color: #344054;
  font-style: normal;
  font-size: 12px;
}

.step-list,
.method-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.step-row {
  display: grid;
  grid-template-columns: minmax(180px, 320px) 32px;
  gap: 8px;
}

.method-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.method-grid label {
  flex-direction: row;
  align-items: center;
  font-weight: 500;
}

.result {
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  background: #101828;
  color: #f9fafb;
  overflow-x: auto;
  font-size: 12px;
}

@media (max-width: 900px) {
  .sample-row {
    grid-template-columns: 28px 1fr;
  }

  .sample-row.head {
    display: none;
  }
}
</style>
