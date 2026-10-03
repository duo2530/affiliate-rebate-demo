<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
      <div class="side-caption">工作空间</div>
      <a class="side-item active" href="#overview"><span>▦</span> 推广概览</a>
      <a class="side-item" href="#recharge"><span>＋</span> 模拟充值</a>
      <div class="side-bottom"><span class="help-mark">?</span><span><b>Demo 演示环境</b><small>数据重启后恢复</small></span></div>
    </aside>
    <main class="workspace" id="overview">
      <header class="topbar"><div class="crumb">用户中心 <span>/</span> 推广概览</div>
        <div class="profile"><span class="avatar">{{ currentUser?.phone?.slice(-1) || '禾' }}</span><div><b>{{ currentUser?.phone }}</b><small>普通用户</small></div>
          <button class="link-button" @click="logout">切换账号</button></div>
      </header>
      <div class="page">
        <div class="page-title"><div><span class="overline">YOUR AFFILIATE SPACE</span><h1>推广概览</h1><p>邀请好友加入，每次成功充值都能获得 10% 返利。</p></div><button class="btn outline" @click="refreshAll">↻ 刷新数据</button></div>
        <p v-if="error" class="notice error">{{ error }}</p>
        <section class="metrics">
          <article class="metric balance"><span>站内余额</span><b>￥{{ overview.balance || '0.00' }}</b><small>累计模拟充值金额</small><i>￥</i></article>
          <article class="metric"><span>可用返利</span><b>￥{{ overview.rebateBalance || '0.00' }}</b><small><i class="green-dot"></i> 即时入账</small></article>
          <article class="metric"><span>直接邀请</span><b>{{ overview.invitedCount || 0 }} <small>位好友</small></b><small>所有用户都可以邀请</small></article>
        </section>
        <section class="invite-card">
          <div><span class="overline">YOUR INVITATION CODE</span><h2>分享专属邀请码</h2><p>好友注册并充值成功，你将获得每笔充值金额的 10%。</p>
            <div class="code-row"><code>{{ overview.inviteCode || '--------' }}</code><button class="btn light" @click="copyLink">{{ copied ? '已复制链接' : '复制邀请链接' }}</button></div>
          </div>
          <div class="invite-art"><div class="orbit"></div><span>✳</span><small>SHARE<br>GOOD THINGS</small></div>
        </section>
        <section id="recharge" class="panel recharge-panel">
          <div class="panel-title"><div><span class="overline">SIMULATED PAYMENT</span><h2>模拟充值</h2><p>充值成功后增加站内余额，并为邀请人计算返利。</p></div><span class="badge"><i></i> 模拟支付</span></div>
          <form class="recharge-form" @submit.prevent="recharge">
            <label>充值金额 <small>人民币 CNY</small><div class="amount-field"><span>￥</span><input v-model.trim="amount" inputmode="decimal" placeholder="例如 100.00" required></div></label>
            <button class="btn primary" :disabled="busy">{{ busy ? '处理中…' : '确认充值 →' }}</button>
          </form>
          <p v-if="rechargeResult" class="notice success">充值成功：￥{{ rechargeResult.amount }} · 充值流水号 {{ rechargeResult.outTradeNo }} · 充值后余额 ￥{{ rechargeResult.balanceAfter }}</p>
        </section>
        <div class="two-col">
          <section class="panel"><div class="panel-title compact"><div><h2>我的受邀好友</h2><p>通过邀请码注册的直接好友</p></div><span class="count">{{ invitees.total || 0 }}</span></div>
            <div v-if="invitees.list?.length" class="table-scroll"><table><thead><tr><th>好友</th><th>加入时间</th></tr></thead><tbody>
              <tr v-for="row in invitees.list" :key="row.userId"><td>{{ row.phone }}</td><td>{{ date(row.boundAt) }}</td></tr>
            </tbody></table></div><div v-else class="empty"><span>♧</span><b>还没有受邀好友</b><small>复制邀请码，分享给朋友试试</small></div>
          </section>
          <section class="panel"><div class="panel-title compact"><div><h2>最近返利</h2><p>每笔成功充值的 10%</p></div><button class="link-button" @click="showRebates = !showRebates">{{ showRebates ? '收起' : '查看明细' }}</button></div>
            <div v-if="rebates.list?.length" class="table-scroll"><table><thead><tr><th>邀请好友</th><th>返利金额</th></tr></thead><tbody>
              <tr v-for="row in rebates.list.slice(0, 5)" :key="row.id"><td>{{ row.inviteePhone }}</td><td class="positive">+￥{{ row.amount }}</td></tr>
            </tbody></table></div><div v-else class="empty"><span>↗</span><b>返利记录会显示在这里</b><small>好友充值成功后立即入账</small></div>
          </section>
        </div>
        <section v-if="showRebates" class="panel history"><div class="panel-title compact"><div><h2>返利明细</h2><p>所有已入账返利</p></div></div>
          <div v-if="rebates.list?.length" class="table-scroll"><table><thead><tr><th>来源订单</th><th>好友</th><th>时间</th><th>返利</th></tr></thead><tbody>
            <tr v-for="row in rebates.list" :key="row.id"><td>{{ row.sourceOrderNo }}</td><td>{{ row.inviteePhone }}</td><td>{{ date(row.createdAt) }}</td><td class="positive">￥{{ row.amount }}</td></tr>
          </tbody></table></div><div v-else class="empty">暂无返利记录</div>
        </section>
        <section class="panel history"><div class="panel-title compact"><div><h2>最近充值</h2><p>站内余额变动记录</p></div></div>
          <div v-if="recharges.list?.length" class="table-scroll"><table><thead><tr><th>充值流水号</th><th>时间</th><th>充值金额</th></tr></thead><tbody>
            <tr v-for="row in recharges.list.slice(0, 5)" :key="row.id"><td>{{ row.outTradeNo }}</td><td>{{ date(row.paidAt) }}</td><td>￥{{ row.amount }}</td></tr>
          </tbody></table></div><div v-else class="empty">暂无充值记录</div>
        </section>
        <footer class="footer">青禾计划 · 邀请返利演示 <span>仅用于本地功能演示</span></footer>
      </div>
    </main>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'
const router = useRouter()
const currentUser = ref(window.currentUser || null)
const overview = ref({})
const invitees = ref({ list: [], total: 0 })
const rebates = ref({ list: [], total: 0 })
const recharges = ref({ list: [], total: 0 })
const amount = ref('')
const error = ref('')
const copied = ref(false)
const busy = ref(false)
const rechargeResult = ref(null)
const showRebates = ref(false)
function date(value) { return value ? new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '—' }
async function refreshAll() {
  error.value = ''
  try {
    const results = await Promise.all([api.me(), api.overview(), api.invitees(), api.rebates(), api.recharges()])
    currentUser.value = results[0].data; overview.value = results[1].data
    invitees.value = results[2].data; rebates.value = results[3].data; recharges.value = results[4].data
  } catch (e) { error.value = e.message }
}
async function recharge() {
  error.value = ''; rechargeResult.value = null; busy.value = true
  try {
    rechargeResult.value = (await api.simulateRecharge(amount.value, crypto.randomUUID())).data
    amount.value = ''
    await refreshAll()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
async function copyLink() {
  const link = location.origin + '/register?inviteCode=' + encodeURIComponent(overview.value.inviteCode || '')
  await navigator.clipboard.writeText(link); copied.value = true
  setTimeout(() => { copied.value = false }, 1600)
}
async function logout() { await api.logout().catch(() => {}); await router.replace('/login') }
onMounted(refreshAll)
</script>
