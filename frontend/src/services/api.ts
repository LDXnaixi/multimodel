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
export function getHealth() {
  return api.get<ApiResponse<{ service: string; status: string; websocketClients: number }>>('/system/health')
}

// Models
export function getModels() {
  return api.get<ApiResponse<unknown[]>>('/models')
}

// Tasks
export function createTask(data: Record<string, unknown>) {
  return api.post<ApiResponse<{ taskId: string }>>('/tasks', data)
}

export function getTasks() {
  return api.get<ApiResponse<unknown[]>>('/tasks')
}

export function getTask(taskId: string) {
  return api.get<ApiResponse<unknown>>(`/tasks/${taskId}`)
}

export function startTask(taskId: string) {
  return api.post<ApiResponse<unknown>>(`/tasks/${taskId}/start`)
}

export function controlTask(taskId: string, action: string) {
  return api.post<ApiResponse<unknown>>(`/tasks/${taskId}/control`, { action })
}

// Inference
export function runInference(data: Record<string, unknown>) {
  return api.post<ApiResponse<unknown>>('/inference/run', data)
}

// Data Pipeline
export function registerDataset(data: Record<string, unknown>) {
  return api.post<ApiResponse<{ datasetId: string }>>('/data/datasets/register', data)
}

export function runPipeline(data: Record<string, unknown>) {
  return api.post<ApiResponse<unknown>>('/data/pipelines/run', data)
}

// Monitor
export function getMetrics() {
  return api.get<ApiResponse<unknown>>('/monitor/metrics')
}

export function getAlerts() {
  return api.get<ApiResponse<unknown>>('/monitor/alerts')
}

export function updateThresholds(data: Record<string, unknown>) {
  return api.post<ApiResponse<unknown>>('/monitor/thresholds', data)
}

// Reports
export function getReport(taskId: string) {
  return api.get<ApiResponse<unknown>>(`/reports/${taskId}`)
}

export function exportReport(taskId: string, format: string) {
  return api.get(`/reports/${taskId}/export?format=${format}`, { responseType: 'blob' })
}

// Users
export function mockLogin(data: Record<string, unknown>) {
  return api.post<ApiResponse<unknown>>('/users/mock-login', data)
}

export function getLoginStats() {
  return api.get<ApiResponse<unknown>>('/users/login-stats')
}

export function getLoginSummary() {
  return api.get<ApiResponse<unknown>>('/users/login-summary')
}

export default api
