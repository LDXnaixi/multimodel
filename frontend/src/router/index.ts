import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: Dashboard
    },
    {
      path: '/tasks',
      name: 'Tasks',
      component: () => import('../views/Tasks.vue')
    },
    {
      path: '/tasks/:taskId',
      name: 'TaskDetail',
      component: () => import('../views/TaskDetail.vue')
    },
    {
      path: '/models',
      name: 'Models',
      component: () => import('../views/Models.vue')
    },
    {
      path: '/inference',
      name: 'Inference',
      component: () => import('../views/Inference.vue')
    },
    {
      path: '/data-pipeline',
      name: 'DataPipeline',
      component: () => import('../views/DataPipeline.vue')
    },
    {
      path: '/monitor',
      name: 'Monitor',
      component: () => import('../views/Monitor.vue')
    },
    {
      path: '/reports',
      name: 'Reports',
      component: () => import('../views/Reports.vue')
    },
    {
      path: '/users',
      name: 'Users',
      component: () => import('../views/Users.vue')
    }
  ]
})

export default router
