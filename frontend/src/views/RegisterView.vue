<template>
  <main class="auth-layout">
    <section class="auth-visual">
      <div class="brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
      <div class="visual-copy"><span class="overline">JOIN THE CIRCLE</span><h1>从一次邀请<br>开始连接。</h1><p>注册后自动获得专属邀请码，填写朋友的邀请码即可建立邀请关系。</p></div>
      <div class="visual-foot"><i></i> 人人可邀请 · 返利规则简单清晰</div>
      <div class="visual-ring ring-one"></div><div class="visual-ring ring-two"></div>
    </section>
    <section class="auth-content">
      <div class="auth-card">
        <div class="brand mobile-brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
        <span class="overline">创建账户</span><h2>加入青禾计划</h2><p class="muted">手机号注册，开始你的邀请旅程</p>
        <form class="stack" @submit.prevent="submit">
          <label>手机号<input v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" placeholder="11 位手机号" required></label>
          <label>设置密码<input v-model="form.password" type="password" autocomplete="new-password" placeholder="至少 8 位" required></label>
          <label>邀请码 <em>选填</em><input v-model.trim="form.inviteCode" maxlength="32" placeholder="填写朋友的邀请码"></label>
          <p v-if="error" class="notice error">{{ error }}</p>
          <button class="btn primary full" :disabled="loading">{{ loading ? '正在创建…' : '创建账户' }} <span>→</span></button>
        </form>
        <p class="hint register-hint">邀请码绑定后不可更改；不填写也可以正常注册。</p>
        <div class="auth-switch">已经有账户？ <RouterLink to="/login">返回登录 →</RouterLink></div>
      </div>
    </section>
  </main>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { api } from '../services/api'
const router = useRouter()
const route = useRoute()
const form = reactive({ phone: '', password: '', inviteCode: String(route.query.inviteCode || '') })
const loading = ref(false)
const error = ref('')
async function submit() {
  error.value = ''; loading.value = true
  try {
    const result = await api.register(form)
    await router.replace(result.data.role === 'ADMIN' ? '/admin' : '/user')
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
</script>
