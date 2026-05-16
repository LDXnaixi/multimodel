<template>
  <div>
    <h2 style="margin-bottom: 20px;">用户统计</h2>

    <div class="card">
      <h3 style="margin-bottom: 16px;">模拟登录</h3>
      <div class="form-group">
        <label>用户名</label>
        <input v-model="loginForm.username" class="form-control" placeholder="frontend-dev" />
      </div>
      <div class="form-group">
        <label>模块</label>
        <input v-model="loginForm.module" class="form-control" placeholder="dashboard" />
      </div>
      <div class="form-group">
        <label>IP</label>
        <input v-model="loginForm.ip" class="form-control" placeholder="127.0.0.1" />
      </div>
      <button class="btn btn-primary" @click="login">记录登录</button>
    </div>

    <div class="grid-2" style="margin-top: 20px;">
      <div class="card">
        <div class="card-title">登录统计</div>
        <pre v-if="stats" style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(stats, null, 2) }}</pre>
        <div v-else style="color: #999; text-align: center;">暂无数据</div>
      </div>
      <div class="card">
        <div class="card-title">登录摘要</div>
        <pre v-if="summary" style="background: #f6f8fa; padding: 16px; border-radius: 4px; overflow-x: auto;">{{ JSON.stringify(summary, null, 2) }}</pre>
        <div v-else style="color: #999; text-align: center;">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockLogin, getLoginStats, getLoginSummary } from '../services/api'

const loginForm = ref({
  username: 'frontend-dev',
  module: 'dashboard',
  ip: '127.0.0.1'
})

const stats = ref<unknown>(null)
const summary = ref<unknown>(null)

async function login() {
  try {
    await mockLogin(loginForm.value)
    localStorage.setItem('username', loginForm.value.username)
    alert('登录记录成功')
    loadData()
  } catch (e) {
    alert('记录失败')
  }
}

async function loadData() {
  try {
    const [sRes, sumRes] = await Promise.all([getLoginStats(), getLoginSummary()])
    stats.value = sRes.data
    summary.value = sumRes.data
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

onMounted(loadData)
</script>
