import { useAppStore } from '../stores/app'
import { useTaskStore } from '../stores/task'

export interface WsMessage {
  type: string
  timestamp: number
  requestId: string
  payload: unknown
}

export interface WsSubscribePayload {
  taskId: string
}

class WebSocketService {
  private socket: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private baseReconnectDelay = 1000
  private subscribedTaskId: string | null = null
  private listeners = new Map<string, Set<(payload: unknown) => void>>()

  connect(taskId?: string) {
    if (this.socket) {
      this.socket.close()
    }

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${location.host}/ws/progress`

    this.socket = new WebSocket(wsUrl)

    this.socket.onopen = () => {
      this.reconnectAttempts = 0
      const appStore = useAppStore()
      appStore.setWsConnected(true)

      if (taskId) {
        this.subscribedTaskId = taskId
        this.subscribe(taskId)
      } else if (this.subscribedTaskId) {
        this.subscribe(this.subscribedTaskId)
      }
    }

    this.socket.onmessage = (event) => {
      try {
        const message: WsMessage = JSON.parse(event.data)
        this.handleMessage(message)
      } catch {
        console.warn('收到非 JSON WebSocket 消息:', event.data)
      }
    }

    this.socket.onclose = () => {
      const appStore = useAppStore()
      appStore.setWsConnected(false)
      this.scheduleReconnect()
    }

    this.socket.onerror = (error) => {
      console.error('WebSocket 错误:', error)
    }
  }

  private handleMessage(message: WsMessage) {
    const appStore = useAppStore()
    const taskStore = useTaskStore()
    this.listeners.get(message.type)?.forEach(listener => listener(message.payload))

    switch (message.type) {
      case 'SERVER_PING': {
        this.send({
          type: 'CLIENT_PONG',
          requestId: message.requestId,
          payload: {}
        })
        break
      }
      case 'SERVER_ACK': {
        console.log('WebSocket 订阅成功:', message.payload)
        break
      }
      case 'TASK_PROGRESS': {
        taskStore.updateTaskProgress(message.payload as any)
        break
      }
      case 'MODEL_RESULT': {
        console.log('模型推理结果:', message.payload)
        break
      }
      case 'DATA_PIPELINE': {
        console.log('数据流水线结果:', message.payload)
        break
      }
      case 'DATA_CATALOG_CHANGED': {
        console.log('服务器数据目录已更新:', message.payload)
        break
      }
      case 'RESOURCE_METRIC': {
        appStore.setLatestMetric(message.payload as any)
        break
      }
      case 'RESOURCE_ALERT': {
        appStore.addAlert(message.payload as any)
        break
      }
      default: {
        console.log('收到未知消息类型:', message.type, message.payload)
      }
    }
  }

  subscribe(taskId: string) {
    this.subscribedTaskId = taskId
    this.send({
      type: 'CLIENT_SUBSCRIBE',
      requestId: `sub-${Date.now()}`,
      payload: { taskId }
    })
  }

  on(type: string, listener: (payload: unknown) => void) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type)!.add(listener)
    return () => this.listeners.get(type)?.delete(listener)
  }

  send(message: Record<string, unknown>) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message))
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('WebSocket 重连次数已达上限')
      return
    }
    const delay = this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts)
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.subscribedTaskId = null
    const appStore = useAppStore()
    appStore.setWsConnected(false)
  }
}

export const wsService = new WebSocketService()
