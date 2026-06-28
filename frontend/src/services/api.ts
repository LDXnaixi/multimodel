import axios, { type AxiosInstance } from 'axios'

export interface ApiResponse<T = unknown> {
  success: boolean
  code: string
  message: string
  data: T
  serverTime: number
  traceId: string
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
  (response) => response.data,
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

// Inference
export function runInference(data: Record<string, unknown>): Promise<any> {
  return api.post('/inference/run', data) as any
}

// Data Pipeline
export function registerDataset(data: Record<string, unknown>): Promise<{ datasetId: string }> {
  return api.post('/data/datasets/register', data) as any
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

export function addSample(data: Record<string, unknown>): Promise<unknown> {
  return api.post('/data/samples', data) as any
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

export default api
