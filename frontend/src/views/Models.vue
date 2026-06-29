<template>
  <div class="models-container" @click="activeHelp = ''">
    <div class="header-actions">
      <h2>模型与 Adapter 管理</h2>
      <div class="action-row">
        <input ref="fileInput" type="file" class="hidden" accept=".onnx" @change="handleFileChange" />
        <button class="btn" @click.stop="triggerFileInput">读取 ONNX 元数据</button>
        <button class="btn btn-primary" @click.stop="openPresetManager">
          管理 Adapter 预设
          <span v-if="presets.length" class="button-count">{{ presets.length }}</span>
        </button>
      </div>
    </div>

    <section class="card">
      <div class="section-head">
        <div>
          <h3>注册本地模型</h3>
          <p class="section-desc">先选择一个预设 Adapter，再填写模型权重路径和运行命令。预设会自动带出数据格式、输出结构和评测指标。</p>
        </div>
        <button class="btn btn-primary" @click.stop="submitRegister">注册模型</button>
      </div>

      <div class="model-info-grid">
        <div class="form-field">
          <FieldLabel field-id="modelId" title="模型 ID" />
          <input v-model="registerForm.modelId" class="form-control" placeholder="例如 local-yolo-pose-onnx" />
          <p class="field-hint">系统内唯一标识。建议用英文、数字和短横线。</p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="modelName" title="模型名称" />
          <input v-model="registerForm.modelName" class="form-control" placeholder="例如 best.onnx YOLO Pose" />
          <p class="field-hint">给测试人员看的名称，可以写中文。</p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="version" title="版本号" />
          <input v-model="registerForm.version" class="form-control" placeholder="例如 1.0.0" />
          <p class="field-hint">用于回滚和报告对比。</p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="modelCategory" title="模型分类" />
          <select v-model="registerForm.modelCategory" class="form-control">
            <option v-for="cat in modelCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
          <p class="field-hint">影响模型在列表和报告里的归类。</p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="adapterType" title="Adapter 预设" important />
          <select
            v-model="registerForm.adapterType"
            class="form-control"
            :disabled="presetLoading || presets.length === 0"
            @change="applyPresetToRegister"
          >
            <option v-if="presetLoading" value="">正在读取预设...</option>
            <option v-else-if="presets.length === 0" value="">暂未读取到 Adapter 预设</option>
            <option v-for="preset in presets" :key="preset.presetId" :value="preset.adapterType">
              {{ preset.displayName }} / {{ preset.adapterType }}
            </option>
          </select>
          <p class="field-hint">
            决定输入数据、后处理、指标计算和输出格式。
            <button type="button" class="text-action" @click.stop="openPresetManager">查看和编辑预设</button>
          </p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="adapterStatus" title="Adapter 状态" important />
          <select v-model="registerForm.adapterStatus" class="form-control">
            <option value="DEMO">DEMO：演示适配</option>
            <option value="REAL">REAL：真实可运行</option>
            <option value="NOT_CONFIGURED">NOT_CONFIGURED：未配置</option>
          </select>
          <p class="field-hint">真实权重和命令都准备好时再标记为 REAL。</p>
        </div>

        <div class="form-field">
          <FieldLabel field-id="packageUri" title="模型权重或目录" important />
          <input
            ref="modelFileInput"
            type="file"
            class="hidden"
            accept=".onnx,.pt,.pth,.pdmodel,.pdiparams,.bin,.safetensors,.json,.yaml,.yml"
            @change="handleArtifactSelection($event, 'file')"
          />
          <input
            ref="modelDirectoryInput"
            type="file"
            class="hidden"
            webkitdirectory
            multiple
            @change="handleArtifactSelection($event, 'directory')"
          />
          <div class="path-picker">
            <input
              v-model="registerForm.packageUri"
              class="form-control path-value"
              placeholder="选择文件、文件夹，或填写服务器已有路径"
            />
            <button type="button" class="picker-button" :disabled="artifactUploading" @click.stop="modelFileInput?.click()">
              选择文件
            </button>
            <button type="button" class="picker-button" :disabled="artifactUploading" @click.stop="modelDirectoryInput?.click()">
              选择文件夹
            </button>
          </div>
          <p class="field-hint">
            {{ artifactUploading ? `正在上传 ${artifactSelectionName}...` : '选中后会上传到后端模型仓库，并自动填写可运行路径。' }}
          </p>
          <p v-if="artifactError" class="field-error">{{ artifactError }}</p>
        </div>

        <div class="form-field wide">
          <FieldLabel field-id="runtimeCommand" title="运行命令" important />
          <input v-model="registerForm.runtimeCommand" class="form-control" placeholder='例如 "C:\\Users\\...\\python.exe" test\\yolo_pose_onnx_runner.py --model test\\best.onnx' />
          <p class="field-hint">后端会启动这个命令，把推理请求 JSON 写入 stdin，并读取 stdout JSON。</p>
        </div>
      </div>

      <div class="preset-preview">
        <div class="preview-block">
          <strong>当前预设指标</strong>
          <span v-for="metric in registerPreview.metrics" :key="metric" class="badge badge-info">{{ metric }}</span>
        </div>
        <div class="preview-block">
          <strong>支持的数据格式</strong>
          <span v-for="format in registerPreview.datasetFormats" :key="format" class="format-chip">{{ format }}</span>
        </div>
      </div>
    </section>

    <section v-show="presetPanelOpen" ref="presetSection" class="card preset-section">
      <div class="section-head">
        <div>
          <h3>Adapter 预设</h3>
          <p class="section-desc">这里保存着 YOLO、PaddleOCR、分类、NLP、ASR、VLM 和自研模型的接入模板。</p>
        </div>
        <div class="action-row">
          <button class="btn" :disabled="presetLoading" @click.stop="loadPresets">刷新</button>
          <button class="btn" @click.stop="newPreset">新增预设</button>
          <button class="btn btn-primary" @click.stop="savePreset">保存预设</button>
          <button class="icon-close" title="收起预设管理" @click.stop="presetPanelOpen = false">×</button>
        </div>
      </div>

      <div v-if="presetError" class="inline-error">
        <span>{{ presetError }}</span>
        <button type="button" @click.stop="loadPresets">重新加载</button>
      </div>
      <div v-else-if="presetLoading" class="loading-state">正在加载 Adapter 预设...</div>
      <div v-else-if="presets.length === 0" class="empty-state">
        <strong>没有读取到预设</strong>
        <span>请确认后端已启动，再点击刷新。</span>
      </div>
      <div v-else class="preset-layout">
        <div class="preset-list">
          <button
            v-for="preset in presets"
            :key="preset.presetId"
            class="preset-item"
            :class="{ active: selectedPreset?.presetId === preset.presetId }"
            @click.stop="selectPreset(preset)"
          >
            <strong>{{ preset.displayName }}</strong>
            <span>{{ preset.adapterType }} · {{ preset.taskType }} · {{ preset.adapterStatus }}</span>
          </button>
        </div>

        <div class="preset-editor" v-if="presetForm">
          <div class="model-info-grid">
            <div class="form-field">
              <FieldLabel field-id="presetId" title="预设 ID" />
              <input v-model="presetForm.presetId" class="form-control" :disabled="!presetForm.custom" placeholder="例如 YOLO_POSE" />
              <p class="field-hint">预设唯一标识。内置预设不建议改 ID。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="displayName" title="显示名称" />
              <input v-model="presetForm.displayName" class="form-control" placeholder="例如 YOLO 姿态估计" />
              <p class="field-hint">显示在前端下拉框和预设列表里的名称。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="modelFamily" title="模型族" />
              <input v-model="presetForm.modelFamily" class="form-control" placeholder="例如 YOLO / OCR / NLP / ASR / VLM" />
              <p class="field-hint">用于把一类模型组织在一起。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="taskType" title="任务类型" important />
              <input v-model="presetForm.taskType" class="form-control" placeholder="例如 detect / pose / segment / text-recognition" />
              <p class="field-hint">描述这个 adapter 执行的具体任务。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="presetAdapterType" title="Adapter Type" important />
              <input v-model="presetForm.adapterType" class="form-control" placeholder="例如 YOLO_POSE" />
              <p class="field-hint">模型注册和推理时用它选择 adapter。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="presetAdapterStatus" title="预设状态" />
              <select v-model="presetForm.adapterStatus" class="form-control">
                <option value="DEMO">DEMO：演示适配</option>
                <option value="REAL">REAL：真实可运行</option>
                <option value="NOT_CONFIGURED">NOT_CONFIGURED：未配置</option>
              </select>
              <p class="field-hint">预设默认状态，注册模型时仍可单独覆盖。</p>
            </div>

            <div class="form-field">
              <FieldLabel field-id="runnerKind" title="运行方式" />
              <input v-model="presetForm.runnerKind" class="form-control" placeholder="例如 LOCAL_PROCESS_JSON" />
              <p class="field-hint">说明后端如何调用模型，当前主要支持本地进程 JSON 协议。</p>
            </div>

            <div class="form-field wide">
              <FieldLabel field-id="runnerTemplate" title="运行命令模板" important />
              <input v-model="presetForm.runnerTemplate" class="form-control" placeholder="例如 python ./model-runners/yolo_adapter.py --task pose" />
              <p class="field-hint">注册模型时可复制为 runtimeCommand，再替换真实路径。</p>
            </div>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="supportedModels" title="支持模型" />
            <textarea v-model="presetText.supportedModels" class="json-area" />
            <p class="field-hint">一行一个模型名称，例如 YOLOv8-pose、YOLO11-pose。</p>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="datasetFormats" title="数据集格式" important />
            <textarea v-model="presetText.datasetFormats" class="json-area" />
            <p class="field-hint">一行一个格式说明。这里要写清标签文件、目录结构和关键字段。</p>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="metrics" title="评测指标" important />
            <textarea v-model="presetText.metrics" class="json-area" />
            <p class="field-hint">一行一个指标，例如 mAP、Precision、Recall、WER、CER。</p>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="defaultConfig" title="默认配置 JSON" important />
            <textarea v-model="presetText.defaultConfig" class="json-area tall" />
            <p class="field-hint">必须是合法 JSON。用于 conf、iou、max_det、sample_rate、max_length 等默认参数。</p>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="outputSchema" title="输出结构 JSON" important />
            <textarea v-model="presetText.outputSchema" class="json-area tall" />
            <p class="field-hint">必须是合法 JSON。描述 adapter 输出里会有哪些字段，例如 boxes、keypoints、rec_text、answer。</p>
          </div>

          <div class="textarea-field">
            <FieldLabel field-id="compatibilityNotes" title="兼容说明" />
            <textarea v-model="presetForm.compatibilityNotes" class="json-area" />
            <p class="field-hint">写清哪些信息可自动推断，哪些需要权重、词表、字符表或标注文件。</p>
          </div>

          <button v-if="presetForm.custom" class="btn btn-warning" @click.stop="removePreset">删除自定义预设</button>
        </div>
      </div>
    </section>

    <section v-if="isAnalyzing" class="alert alert-info">正在解析模型文件：{{ selectedFileName }}</section>

    <section v-if="analyzedModel" class="card result-card">
      <h3>ONNX 解析结果</h3>
      <pre>{{ JSON.stringify(analyzedModel, null, 2) }}</pre>
      <button class="btn btn-success" @click.stop="clearAnalysis">加入模型列表</button>
    </section>

    <section class="filter-section">
      <span class="filter-label">分类筛选：</span>
      <button
        v-for="cat in categories"
        :key="cat"
        class="btn"
        :class="selectedCategory === cat ? 'btn-primary' : 'btn-outline'"
        @click.stop="selectedCategory = cat"
      >
        {{ cat }}
      </button>
    </section>

    <section class="card">
      <table>
        <thead>
          <tr>
            <th>模型ID</th>
            <th>名称</th>
            <th>分类</th>
            <th>Adapter</th>
            <th>状态</th>
            <th>指标</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="model in filteredModels" :key="model.modelId">
            <td>{{ model.modelId }}</td>
            <td>{{ model.modelName }}<span v-if="model.isCustom" class="badge badge-info custom-badge">自定义</span></td>
            <td><span class="category-badge">{{ model.modelCategory }}</span></td>
            <td>
              <strong>{{ model.adapterType || '-' }}</strong>
              <span class="badge" :class="getAdapterClass(model.adapterStatus)">{{ model.adapterStatus || 'UNKNOWN' }}</span>
              <div class="muted">{{ shortFormats(model.datasetFormats) }}</div>
            </td>
            <td><span class="badge" :class="getStatusClass(model.deploymentStatus)">{{ model.deploymentStatus }}</span></td>
            <td>{{ (model.availableMetrics || []).join(', ') }}</td>
            <td>
              <button class="btn btn-small btn-primary" @click.stop="goToInference(model.modelId)">测试</button>
              <button class="btn btn-small btn-success" @click.stop="changeStatus(model.modelId, 'RUNNING')">上线</button>
              <button class="btn btn-small btn-warning" @click.stop="changeStatus(model.modelId, 'OFFLINE')">下线</button>
              <button class="btn btn-small" @click.stop="rollback(model.modelId, model.version)">回滚</button>
            </td>
          </tr>
          <tr v-if="filteredModels.length === 0">
            <td colspan="7" class="empty">暂无模型数据</td>
          </tr>
        </tbody>
      </table>
      <button class="btn" @click.stop="loadRunLogs">查看运行日志</button>
    </section>

    <section v-if="runLogs.length" class="card">
      <h3>模型运行日志</h3>
      <pre>{{ JSON.stringify(runLogs, null, 2) }}</pre>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  analyzeModel,
  deleteAdapterPreset,
  getAdapterPresets,
  getModelRunLogs,
  getModels,
  registerModel,
  rollbackModel,
  saveAdapterPreset,
  uploadModelArtifact,
  updateModelStatus
} from '../services/api'

const router = useRouter()
const models = ref<any[]>([])
const presets = ref<any[]>([])
const selectedPreset = ref<any>(null)
const presetForm = ref<any>(null)
const presetText = ref<Record<string, string>>({})
const fileInput = ref<HTMLInputElement | null>(null)
const modelFileInput = ref<HTMLInputElement | null>(null)
const modelDirectoryInput = ref<HTMLInputElement | null>(null)
const presetSection = ref<HTMLElement | null>(null)
const isAnalyzing = ref(false)
const selectedFileName = ref('')
const analyzedModel = ref<any>(null)
const selectedCategory = ref('全部')
const runLogs = ref<any[]>([])
const activeHelp = ref('')
const presetPanelOpen = ref(false)
const presetLoading = ref(false)
const presetError = ref('')
const artifactUploading = ref(false)
const artifactSelectionName = ref('')
const artifactError = ref('')

const helpText: Record<string, string> = {
  modelId: '唯一 ID，建议使用英文、数字、短横线或下划线。示例：local-yolo-pose-onnx。',
  modelName: '显示名称，给测试人员识别模型用。示例：best.onnx YOLO 姿态模型。',
  version: '模型版本号，用于回滚和报告对比。推荐语义化版本，例如 1.0.0。',
  modelCategory: '模型大类，决定列表筛选和报告归类。YOLO 选 OBJECT_DETECTION，OCR 选 OCR，文本模型选 SEMANTIC_ANALYSIS。',
  adapterType: '最关键字段。它决定后端使用哪套预处理、后处理、数据集格式和指标计算逻辑。YOLO 姿态模型选 YOLO_POSE。',
  adapterStatus: 'DEMO 表示演示适配；REAL 表示真实模型命令可运行；NOT_CONFIGURED 表示只有预设，还没配权重或命令。',
  packageUri: '模型权重或目录路径。可以是 test\\best.onnx、C:\\models\\best.onnx、/opt/models/dbnet，也可以是 demo://local/xxx。',
  runtimeCommand: '后端启动的命令。命令需从 stdin 读取 InferenceRequest JSON，并向 stdout 输出 {outputs, metrics, durationMs, runnerStatus} JSON。',
  presetId: '预设的唯一 ID。内置预设如 YOLO_POSE、PADDLEOCR_DB_DET。自定义预设可以写 custom-yolo-pose-v2。',
  displayName: '预设显示名，写给使用者看的名称。例如 YOLO 姿态、DBNet 文本检测。',
  modelFamily: '模型族名称。例如 YOLO、OCR、NLP、ASR、VLM、CUSTOM。',
  taskType: '具体任务类型。YOLO 可填 detect、pose、segment、classify、obb；OCR 可填 text-detection 或 text-recognition。',
  presetAdapterType: '模型注册时引用的 adapterType。建议全大写加下划线，例如 YOLO_POSE。',
  presetAdapterStatus: '这个预设的默认状态。真实 runner 和依赖齐全时设 REAL，否则保持 DEMO 或 NOT_CONFIGURED。',
  runnerKind: '运行方式。当前推荐 LOCAL_PROCESS_JSON，表示后端通过本地进程和 JSON stdin/stdout 通信。',
  runnerTemplate: '命令模板，供注册模型时复制。示例：python ./model-runners/yolo_adapter.py --task pose。',
  supportedModels: '一行一个模型名或模型族，例如 YOLOv8-pose、YOLO11-pose、Qwen-VL。',
  datasetFormats: '写清数据集目录和标签格式。一行一个说明。例如 YOLO pose: data.yaml + labels/*.txt，标签为 class box keypoints。',
  metrics: '一行一个指标。检测常用 mAP/Precision/Recall，OCR 识别常用 RecognitionRate/RejectionRate/CER，语音常用 WER/CER。',
  defaultConfig: '合法 JSON。写默认推理参数，例如 {"conf":0.25,"iou":0.45,"max_det":300,"kpt_shape":"auto"}。',
  outputSchema: '合法 JSON。写输出字段结构，例如 {"boxes":"xyxy","keypoints":"auto Nx2/Nx3","scores":"confidence"}。',
  compatibilityNotes: '写适配边界，例如哪些字段能自动推断，哪些必须由用户提供，比如字符表、词表、data.yaml、类别名。'
}

const modelCategories = ['OBJECT_DETECTION', 'OCR', 'IMAGE_CLASSIFICATION', 'SEMANTIC_ANALYSIS', 'SPEECH_RECOGNITION', 'VISION_LANGUAGE', 'CUSTOM']
const categories = ['全部', ...modelCategories]
const registerForm = ref<any>({
  modelId: '',
  modelName: '',
  version: '1.0.0',
  modelCategory: 'OBJECT_DETECTION',
  adapterType: 'YOLO_DETECT',
  adapterStatus: 'DEMO',
  packageUri: '',
  runtimeCommand: ''
})

const FieldLabel = defineComponent({
  props: {
    fieldId: { type: String, required: true },
    title: { type: String, required: true },
    important: { type: Boolean, default: false }
  },
  setup(props) {
    return () => h('div', { class: 'label-row' }, [
      h('span', { class: 'field-label' }, [
        props.title,
        props.important ? h('strong', { class: 'required-mark' }, ' *') : null
      ]),
      h('button', {
        class: 'help-button',
        type: 'button',
        title: `查看“${props.title}”填写说明`,
        'aria-label': `查看“${props.title}”填写说明`,
        onClick: (event: MouseEvent) => {
          event.stopPropagation()
          activeHelp.value = activeHelp.value === props.fieldId ? '' : props.fieldId
        }
      }, '?'),
      activeHelp.value === props.fieldId
        ? h('div', { class: 'help-popover', onClick: (event: MouseEvent) => event.stopPropagation() }, [
            h('span', { class: 'help-title' }, `${props.title}怎么填？`),
            h('p', { class: 'help-copy' }, helpText[props.fieldId] || '暂无说明')
          ])
        : null
    ])
  }
})

const filteredModels = computed(() => {
  if (selectedCategory.value === '全部') return models.value
  return models.value.filter(m => m.modelCategory === selectedCategory.value)
})

const registerPreview = computed(() => presets.value.find(p => p.adapterType === registerForm.value.adapterType) || { metrics: [], datasetFormats: [] })

async function loadAll() {
  const [modelResult] = await Promise.allSettled([getModels(), loadPresets()])
  if (modelResult.status === 'fulfilled') models.value = modelResult.value as any[]
}

async function loadPresets() {
  presetLoading.value = true
  presetError.value = ''
  try {
    presets.value = await getAdapterPresets() as any[]
    const currentId = selectedPreset.value?.presetId
    const current = presets.value.find(item => item.presetId === currentId) || presets.value[0]
    if (current) {
      selectPreset(current)
      if (!presets.value.some(item => item.adapterType === registerForm.value.adapterType)) {
        registerForm.value.adapterType = current.adapterType
      }
    }
  } catch (error: any) {
    presets.value = []
    presetError.value = error?.response?.data?.message || 'Adapter 预设加载失败，请确认后端已在 8080 端口启动。'
  } finally {
    presetLoading.value = false
  }
}

async function openPresetManager() {
  presetPanelOpen.value = true
  if (!presets.value.length && !presetLoading.value) await loadPresets()
  await nextTick()
  presetSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  selectedFileName.value = file.name
  isAnalyzing.value = true
  analyzedModel.value = null
  try {
    analyzedModel.value = await analyzeModel({ fileName: file.name, fileSize: file.size, lastModified: new Date(file.lastModified).toISOString() })
  } finally {
    isAnalyzing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function handleArtifactSelection(event: Event, kind: 'file' | 'directory') {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  artifactUploading.value = true
  artifactError.value = ''
  artifactSelectionName.value = kind === 'directory'
    ? ((files[0] as File & { webkitRelativePath?: string }).webkitRelativePath?.split('/')[0] || '模型目录')
    : files[0].name

  const formData = new FormData()
  files.forEach(file => {
    const relativePath = (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name
    formData.append('files', file, file.name)
    formData.append('relativePaths', relativePath)
  })

  try {
    const result = await uploadModelArtifact(formData, kind)
    registerForm.value.packageUri = result.packageUri
  } catch (error: any) {
    artifactError.value = error?.response?.data?.message || '上传失败，请确认后端已启动，并检查文件大小限制。'
  } finally {
    artifactUploading.value = false
    input.value = ''
  }
}

function clearAnalysis() {
  if (analyzedModel.value) models.value.unshift(analyzedModel.value)
  analyzedModel.value = null
}

function applyPresetToRegister() {
  const preset = presets.value.find(p => p.adapterType === registerForm.value.adapterType)
  if (!preset) return
  registerForm.value.adapterStatus = preset.adapterStatus
  if (!registerForm.value.runtimeCommand) registerForm.value.runtimeCommand = preset.runnerTemplate
}

async function submitRegister() {
  const preset = presets.value.find(p => p.adapterType === registerForm.value.adapterType)
  await registerModel({
    ...registerForm.value,
    supportedModalities: preset?.supportedModalities || ['image'],
    availableMetrics: preset?.metrics || ['Accuracy', 'Latency'],
    adapterConfig: preset?.defaultConfig || {},
    datasetFormats: preset?.datasetFormats || []
  })
  registerForm.value.modelId = ''
  registerForm.value.modelName = ''
  await loadAll()
}

function selectPreset(preset: any) {
  selectedPreset.value = preset
  presetForm.value = JSON.parse(JSON.stringify(preset))
  presetText.value = {
    supportedModels: (preset.supportedModels || []).join('\n'),
    datasetFormats: (preset.datasetFormats || []).join('\n'),
    metrics: (preset.metrics || []).join('\n'),
    defaultConfig: JSON.stringify(preset.defaultConfig || {}, null, 2),
    outputSchema: JSON.stringify(preset.outputSchema || {}, null, 2)
  }
}

function newPreset() {
  const base = selectedPreset.value || {}
  presetForm.value = {
    ...JSON.parse(JSON.stringify(base)),
    presetId: `custom-${Date.now()}`,
    displayName: '自定义预设',
    adapterType: `CUSTOM_${Date.now()}`,
    custom: true
  }
  presetText.value = {
    supportedModels: 'custom',
    datasetFormats: 'custom-jsonl',
    metrics: 'Accuracy\nLatency',
    defaultConfig: '{}',
    outputSchema: '{}'
  }
}

async function savePreset() {
  if (!presetForm.value) return
  const payload = {
    ...presetForm.value,
    supportedModels: lines(presetText.value.supportedModels),
    datasetFormats: lines(presetText.value.datasetFormats),
    metrics: lines(presetText.value.metrics),
    defaultConfig: parseJson(presetText.value.defaultConfig),
    outputSchema: parseJson(presetText.value.outputSchema)
  }
  await saveAdapterPreset(payload, payload.presetId)
  await loadAll()
}

async function removePreset() {
  if (!presetForm.value?.presetId) return
  await deleteAdapterPreset(presetForm.value.presetId)
  selectedPreset.value = null
  presetForm.value = null
  await loadAll()
}

function lines(value: string) {
  return value.split(/\r?\n/).map(item => item.trim()).filter(Boolean)
}

function parseJson(value: string) {
  try {
    return JSON.parse(value || '{}')
  } catch {
    alert('JSON 格式不正确')
    throw new Error('invalid json')
  }
}

async function changeStatus(modelId: string, status: string) {
  await updateModelStatus(modelId, status)
  await loadAll()
}

async function rollback(modelId: string, version: string) {
  const target = prompt('请输入要回滚的版本号', version || '1.0.0')
  if (!target) return
  await rollbackModel(modelId, target)
  await loadAll()
}

async function loadRunLogs() {
  runLogs.value = await getModelRunLogs() as any[]
}

function shortFormats(formats: string[] = []) {
  return formats.slice(0, 2).join('；')
}

function getStatusClass(status: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'RUNNING' || normalized === 'AVAILABLE') return 'badge-success'
  if (normalized === 'ANALYZED') return 'badge-info'
  return 'badge-warning'
}

function getAdapterClass(status: string) {
  if (status === 'REAL') return 'badge-success'
  if (status === 'NOT_CONFIGURED') return 'badge-warning'
  return 'badge-info'
}

function goToInference(modelId: string) {
  router.push({ path: '/inference', query: { modelId } })
}

onMounted(loadAll)
</script>

<style scoped>
.models-container { padding: 10px; }
.hidden { display: none; }
.header-actions, .section-head, .filter-section { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.section-head h3 { margin-bottom: 4px; }
.section-desc { margin: 0; color: #666; line-height: 1.5; max-width: 860px; }
.action-row { display: flex; gap: 8px; flex-wrap: wrap; }
.button-count { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; margin-left: 5px; padding: 0 5px; border-radius: 9px; color: #096dd9; background: #fff; font-size: 11px; }
.filter-section { justify-content: flex-start; align-items: center; flex-wrap: wrap; }
.model-info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 14px; }
.form-field, .textarea-field { position: relative; min-width: 0; }
.wide { grid-column: span 2; }
.label-row { position: relative; display: flex; align-items: center; gap: 7px; min-height: 24px; margin-bottom: 5px; }
.field-label { font-weight: 700; color: #333; }
.required-mark { color: #d4380d; }
.field-hint { margin: 5px 0 0; color: #666; font-size: 12px; line-height: 1.45; }
.field-error { margin: 5px 0 0; color: #cf1322; font-size: 12px; }
.help-button { display: inline-flex; align-items: center; justify-content: center; width: 17px; height: 17px; border-radius: 50%; border: 0; color: #667085; background: #edf0f4; font-size: 11px; font-weight: 700; cursor: pointer; padding: 0; transition: color .15s, background .15s; }
.help-button:hover, .help-button:focus-visible { color: #fff; background: #1677ff; outline: none; }
.help-popover { position: absolute; z-index: 30; left: 0; top: 30px; width: min(300px, 82vw); padding: 9px 11px; color: #344054; background: #fff; border: 1px solid #e4e7ec; border-radius: 4px 10px 10px 10px; box-shadow: 0 7px 18px rgba(16,24,40,.12); font-size: 12px; line-height: 1.5; }
.help-popover::before { content: ''; position: absolute; top: -6px; left: 13px; width: 10px; height: 10px; background: #fff; border-left: 1px solid #e4e7ec; border-top: 1px solid #e4e7ec; transform: rotate(45deg); }
.help-title { display: block; margin-bottom: 3px; color: #101828; font-size: 11px; font-weight: 700; }
.help-copy { margin: 0; }
.text-action { border: 0; padding: 0; color: #1677ff; background: transparent; font-size: inherit; cursor: pointer; }
.text-action:hover { text-decoration: underline; }
.path-picker { display: grid; grid-template-columns: minmax(0, 1fr) auto auto; }
.path-picker .path-value { min-width: 0; border-radius: 4px 0 0 4px; background: #fff; }
.picker-button { min-width: 76px; padding: 0 10px; border: 1px solid #d9d9d9; border-left: 0; color: #344054; background: #f8fafc; cursor: pointer; }
.picker-button:last-child { border-radius: 0 4px 4px 0; }
.picker-button:hover:not(:disabled) { color: #1677ff; background: #eef6ff; }
.picker-button:disabled { color: #98a2b3; cursor: wait; }
.preset-section { scroll-margin-top: 12px; }
.icon-close { width: 32px; height: 32px; border: 0; color: #667085; background: transparent; font-size: 24px; line-height: 28px; cursor: pointer; }
.icon-close:hover { color: #101828; background: #f2f4f7; }
.inline-error, .loading-state, .empty-state { min-height: 72px; display: flex; align-items: center; justify-content: center; gap: 10px; padding: 16px; border: 1px solid #eaecf0; color: #667085; background: #fafafa; }
.inline-error { justify-content: space-between; color: #b42318; background: #fffbfa; border-color: #fecdca; }
.inline-error button { border: 0; color: #b42318; background: transparent; font-weight: 700; cursor: pointer; }
.empty-state { flex-direction: column; }
.preset-preview { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 16px; }
.preview-block { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 10px; background: #fafafa; border: 1px solid #eee; border-radius: 6px; }
.format-chip { font-size: 12px; background: #f5f5f5; border: 1px solid #ddd; padding: 2px 6px; border-radius: 4px; }
.preset-layout { display: grid; grid-template-columns: 310px minmax(0, 1fr); gap: 16px; }
.preset-list { display: flex; flex-direction: column; gap: 8px; max-height: 720px; overflow: auto; }
.preset-item { text-align: left; border: 1px solid #ddd; background: #fff; padding: 10px; border-radius: 6px; cursor: pointer; }
.preset-item span { display: block; color: #666; font-size: 12px; margin-top: 4px; }
.preset-item.active { border-color: #1890ff; background: #eef7ff; }
.preset-editor label { display: block; margin: 10px 0 4px; font-weight: 600; color: #555; }
.json-area { width: 100%; min-height: 62px; font-family: Consolas, monospace; border: 1px solid #ddd; border-radius: 4px; padding: 8px; resize: vertical; }
.json-area.tall { min-height: 120px; }
.btn-small { padding: 4px 8px; font-size: 12px; margin-right: 4px; }
.btn-outline { background: transparent; color: #1890ff; border: 1px solid #1890ff; }
.category-badge { display: inline-block; padding: 2px 6px; background: #f0f0f0; border-radius: 4px; font-size: 12px; color: #666; }
.custom-badge { margin-left: 6px; font-size: 10px; }
.muted { color: #777; font-size: 12px; margin-top: 4px; max-width: 280px; }
.empty { text-align: center; color: #999; }
.result-card pre, .card pre { background: #f6f8fa; padding: 12px; border-radius: 4px; max-height: 280px; overflow: auto; }
.alert-info { padding: 10px; background: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; border-radius: 4px; margin-bottom: 16px; }
@media (max-width: 900px) {
  .preset-layout, .preset-preview { grid-template-columns: 1fr; }
  .wide { grid-column: span 1; }
  .path-picker { grid-template-columns: 1fr 1fr; }
  .path-picker .path-value { grid-column: 1 / -1; border-radius: 4px 4px 0 0; }
  .picker-button { min-height: 34px; border-left: 1px solid #d9d9d9; }
  .picker-button:last-child { border-radius: 0 0 4px 0; }
}
</style>
