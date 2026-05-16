<template>
  <div class="app-layout">
    <nav class="sidebar">
      <h1 class="logo">多模态联调</h1>
      <ul class="nav-menu">
        <li><router-link to="/">总览</router-link></li>
        <li><router-link to="/tasks">任务管理</router-link></li>
        <li><router-link to="/models">模型列表</router-link></li>
        <li><router-link to="/inference">模型推理</router-link></li>
        <li><router-link to="/data-pipeline">数据流水线</router-link></li>
        <li><router-link to="/monitor">资源监控</router-link></li>
        <li><router-link to="/reports">报告导出</router-link></li>
        <li><router-link to="/users">用户统计</router-link></li>
      </ul>
      <div class="ws-status">
        <span :class="['dot', appStore.wsConnected ? 'connected' : 'disconnected']"></span>
        {{ appStore.wsConnected ? 'WS 已连接' : 'WS 未连接' }}
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAppStore } from './stores/app'
import { wsService } from './services/websocket'

const appStore = useAppStore()

onMounted(() => {
  wsService.connect()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
  color: #333;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  background: #1a1a2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
  position: fixed;
  height: 100vh;
}

.logo {
  font-size: 18px;
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  margin-bottom: 10px;
}

.nav-menu {
  list-style: none;
  flex: 1;
}

.nav-menu li a {
  display: block;
  padding: 12px 24px;
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  transition: all 0.2s;
}

.nav-menu li a:hover,
.nav-menu li a.router-link-active {
  color: #fff;
  background: rgba(255,255,255,0.1);
}

.ws-status {
  padding: 12px 24px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.connected {
  background: #52c41a;
}

.dot.disconnected {
  background: #ff4d4f;
}

.main-content {
  flex: 1;
  margin-left: 220px;
  padding: 24px;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #1a1a2e;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: #1890ff;
  color: #fff;
}

.btn-primary:hover {
  background: #40a9ff;
}

.btn-success {
  background: #52c41a;
  color: #fff;
}

.btn-success:hover {
  background: #73d13d;
}

.btn-warning {
  background: #faad14;
  color: #fff;
}

.btn-danger {
  background: #ff4d4f;
  color: #fff;
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
  font-size: 14px;
  font-weight: 500;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

th {
  background: #fafafa;
  font-weight: 600;
}

tr:hover {
  background: #fafafa;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.badge-success {
  background: #f6ffed;
  color: #52c41a;
}

.badge-warning {
  background: #fffbe6;
  color: #faad14;
}

.badge-error {
  background: #fff2f0;
  color: #ff4d4f;
}

.badge-info {
  background: #e6f7ff;
  color: #1890ff;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1890ff;
  margin: 8px 0;
}

.metric-label {
  font-size: 14px;
  color: #666;
}
</style>
