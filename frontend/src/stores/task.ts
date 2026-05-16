import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TaskNode {
  nodeId: string
  nodeName: string
  nodeType: string
  priority: number
  resourceRatio: number
  parameters: Record<string, unknown>
}

export interface Task {
  taskId: string
  taskName: string
  scenarioDescription: string
  status: string
  nodes: TaskNode[]
  createdAt?: number
  updatedAt?: number
}

export interface TaskProgress {
  taskId: string
  taskStatus: string
  nodeId: string
  nodeStatus: string
  progress: number
  message: string
}

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const taskProgress = ref<Record<string, TaskProgress>>({})

  function setTasks(list: Task[]) {
    tasks.value = list
  }

  function setCurrentTask(task: Task | null) {
    currentTask.value = task
  }

  function updateTaskProgress(progress: TaskProgress) {
    taskProgress.value[progress.taskId] = progress
  }

  function getTaskProgress(taskId: string): TaskProgress | undefined {
    return taskProgress.value[taskId]
  }

  return {
    tasks,
    currentTask,
    taskProgress,
    setTasks,
    setCurrentTask,
    updateTaskProgress,
    getTaskProgress
  }
})
