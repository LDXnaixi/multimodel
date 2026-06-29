import axios, { type AxiosInstance } from 'axios'

export interface ApiResponse<T = unknown> {
  success: boolean
  code: string
  message: string
  data: T
  serverTime: number
  traceId: string
}

export interface DatasetSummary {
  datasetId: string
  datasetName: string
  assetCount: number
  supportedModalities: string[]
  version: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface SampleSummary {
  sampleId: string
  datasetId: string
  name: string
  dataType: string
  contentUrl: string
  imageRelativePath?: string
  originalName: string
  contentType?: string
  fileSize?: number
  sha256?: string
  labelContentUrl?: string
  labelRelativePath?: string
  labelOriginalName?: string
  labelContentType?: string
  labelFileSize?: number
  labelSha256?: string
  tags: string[]
  metadata: Record<string, unknown>
  version: string
  createdAt?: string
}

const api: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use((config) => {
  config.headers['X-Trace-Id'] = generateTraceId()
  const username = localStorage.getItem('username')
  if (username) {
    config.headers['X-User-Name'] = username
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') return response.data
    const body = response.data
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return body.data
    }
    return body
  },
  (error) => Promise.reject(error)
)

function generateTraceId(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// Health
export function getHealth(): Promise<{ service: string; status: string; websocketClients: number }> {
  return api.get('/system/health') as any
}

// Models
export function getModels(): Promise<unknown[]> {
  return api.get('/models') as any
}

export function analyzeModel(data: { fileName: string; fileSize: number; lastModified: string }): Promise<any> {
  return api.post('/models/analyze', data) as any
}

export function registerModel(data: Record<string, unknown>): Promise<any> {
  return api.post('/models', data) as any
}

export function uploadModelArtifact(data: FormData, kind: 'file' | 'directory'): Promise<any> {
  return api.post(`/models/artifacts/upload?kind=${kind}`, data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as any
}

export function updateModelStatus(modelId: string, status: string): Promise<any> {
  return api.post(`/models/${modelId}/status`, { status }) as any
}

export function rollbackModel(modelId: string, version: string): Promise<any> {
  return api.post(`/models/${modelId}/rollback`, { version }) as any
}

export function getModelRunLogs(): Promise<unknown[]> {
  return api.get('/models/run-logs') as any
}

export function getAdapterPresets(): Promise<unknown[]> {
  return api.get('/adapters/presets') as any
}

export function saveAdapterPreset(data: Record<string, unknown>, presetId?: string): Promise<unknown> {
  if (presetId) return api.put(`/adapters/presets/${presetId}`, data) as any
  return api.post('/adapters/presets', data) as any
}

export function deleteAdapterPreset(presetId: string): Promise<unknown> {
  return api.delete(`/adapters/presets/${presetId}`) as any
}

// Tasks
export function createTask(data: Record<string, unknown>): Promise<{ taskId: string }> {
  return api.post('/tasks', data) as any
}

export function getTasks(): Promise<unknown[]> {
  return api.get('/tasks') as any
}

export function getTask(taskId: string): Promise<unknown> {
  return api.get(`/tasks/${taskId}`) as any
}

export function startTask(taskId: string): Promise<unknown> {
  return api.post(`/tasks/${taskId}/start`) as any
}

export function controlTask(taskId: string, action: string): Promise<unknown> {
  return api.post(`/tasks/${taskId}/control`, { action }) as any
}

export function validateTask(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/tasks/validate', data) as any
}

export function simulateTask(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/tasks/simulate', data) as any
}

export function rerunTask(taskId: string, data: Record<string, unknown>): Promise<unknown> {
  return api.post(`/tasks/${taskId}/rerun`, data) as any
}

export function saveTaskTemplate(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/tasks/templates', data) as any
}

export function getTaskTemplates(): Promise<unknown[]> {
  return api.get('/tasks/templates') as any
}

export function createTaskFromTemplate(templateId: string, data: Record<string, unknown>): Promise<unknown> {
  return api.post(`/tasks/templates/${templateId}/tasks`, data) as any
}

// Inference
export function runInference(data: Record<string, unknown>): Promise<any> {
  return api.post('/inference/run', data) as any
}

// Data Pipeline
export function registerDataset(data: Record<string, unknown>): Promise<{ datasetId: string }> {
  return api.post('/data/datasets/register', data) as any
}

export function uploadDataset(datasetName: string, files: File[], tags: string[] = []): Promise<DatasetSummary> {
  const data = new FormData()
  data.append('datasetName', datasetName)
  tags.forEach(tag => data.append('tags', tag))
  files.forEach(file => {
    const relativePath = (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name
    data.append('relativePaths', relativePath)
    data.append('files', file, file.name)
  })
  return api.post('/data/datasets/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as any
}

export function listDatasets(): Promise<DatasetSummary[]> {
  return api.get('/data/datasets') as any
}

export function listDatasetSamples(datasetId: string): Promise<SampleSummary[]> {
  return api.get(`/data/datasets/${encodeURIComponent(datasetId)}/samples`) as any
}

export function getSample(sampleId: string): Promise<SampleSummary> {
  return api.get(`/data/samples/${encodeURIComponent(sampleId)}`) as any
}

export function runPipeline(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/data/pipelines/run', data) as any
}

export function listDataSources(): Promise<unknown[]> {
  return api.get('/data/datasources') as any
}

export function addDataSource(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/data/datasources', data) as any
}

export function searchSamples(data: Record<string, unknown>): Promise<unknown[]> {
  return api.post('/data/samples/search', data) as any
}

export function processData(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/data/processing', data) as any
}

export function augmentData(data: Record<string, unknown>): Promise<unknown[]> {
  return api.post('/data/augmentation', data) as any
}

export function fuseData(data: Record<string, unknown>): Promise<unknown[]> {
  return api.post('/data/fusion', data) as any
}

export function generateScenario(data: Record<string, unknown>): Promise<unknown[]> {
  return api.post('/data/scenarios/generate', data) as any
}

// Monitor
export function getMetrics(): Promise<unknown> {
  return api.get('/monitor/metrics') as any
}

export function getAlerts(): Promise<unknown> {
  return api.get('/monitor/alerts') as any
}

export function updateThresholds(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/monitor/thresholds', data) as any
}

// Reports
export function getReport(taskId: string): Promise<unknown> {
  return api.get(`/reports/${taskId}`) as any
}

export function exportReport(taskId: string, format: string) {
  return api.get(`/reports/${taskId}/export?format=${format}`, { responseType: 'blob' })
}

// Users
export function mockLogin(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/users/mock-login', data) as any
}

export function getLoginStats(): Promise<unknown> {
  return api.get('/users/login-stats') as any
}

export function getLoginSummary(): Promise<unknown> {
  return api.get('/users/login-summary') as any
}

// Environment
export function captureEnvironment(): Promise<unknown> {
  return api.post('/environment/capture') as any
}

export function getEnvironmentReports(): Promise<unknown[]> {
  return api.get('/environment/reports') as any
}

export default api
