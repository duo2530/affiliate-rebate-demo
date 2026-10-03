<template>
  <main class="auth-layout">
    <section class="auth-visual">
      <div class="brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
      <div class="visual-copy"><span class="overline">AFFILIATE DEMO</span><h1>让每一次分享<br>都有回响。</h1><p>体验邀请、充值与返利入账的完整流程。</p></div>
      <div class="visual-foot"><i></i> 本地演示环境 · 重启后恢复初始数据</div>
      <div class="visual-ring ring-one"></div><div class="visual-ring ring-two"></div>
    </section>
    <section class="auth-content">
      <div class="auth-card">
        <div class="brand mobile-brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
        <span class="overline">欢迎回来</span><h2>登录你的账户</h2><p class="muted">使用手机号和密码继续</p>
        <form class="stack" @submit.prevent="submit">
          <label>手机号<input v-model.trim="form.phone" inputmode="numeric" autocomplete="username" placeholder="请输入手机号" required></label>
          <label>登录密码<input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" required></label>
          <p v-if="error" class="notice error">{{ error }}</p>
          <button class="btn primary full" :disabled="loading">{{ loading ? '正在登录…' : '登录' }} <span>→</span></button>
        </form>
        <div class="demo-list"><div class="small-title">快速填入演示账号</div>
          <button v-for="account in accounts" :key="account.phone" class="demo-account" type="button" @click="fill(account)">
            <span class="avatar" :class="{ admin: account.role === 'ADMIN' }">{{ account.role === 'ADMIN' ? '管' : account.name.slice(-1) }}</span>
            <span><b>{{ account.name }}</b><small>{{ account.phone }}</small></span><span class="arrow">↗</span>
          </button>
          <p class="hint">点击填入手机号和密码，再点登录。也可以创建新用户。</p>
        </div>
        <div class="auth-switch">还没有账户？ <RouterLink to="/register">创建新账户 →</RouterLink></div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { api } from '../services/api'
const router = useRouter()
const form = reactive({ phone: '', password: '' })
const loading = ref(false)
const error = ref('')
const accounts = [
  { name: '演示用户 A', phone: '13800000001', password: 'DemoPass123', role: 'USER' },
  { name: '演示用户 B', phone: '13800000002', password: 'DemoPass123', role: 'USER' },
  { name: '管理员', phone: '13800000003', password: 'AdminPass123', role: 'ADMIN' },
]
function fill(account) { form.phone = account.phone; form.password = account.password; error.value = '' }
async function submit() {
  error.value = ''; loading.value = true
  try {
    const result = await api.login(form)
    await router.replace(result.data.role === 'ADMIN' ? '/admin' : '/user')
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
</script>
