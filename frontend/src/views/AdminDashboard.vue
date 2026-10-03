<template>
  <div class="layout">
    <aside class="sidebar"><div class="brand"><span class="brand-mark">禾</span><b>青禾计划</b></div>
      <div class="side-caption">管理后台</div>
      <button v-for="tab in tabs" :key="tab.key" class="side-item" :class="{ active: selected === tab.key }" @click="selected = tab.key">
        <span>{{ tab.icon }}</span> {{ tab.label }}
      </button>
      <div class="side-bottom"><span class="help-mark">i</span><span><b>只读管理视图</b><small>仅供演示查看</small></span></div>
    </aside>
    <main class="workspace"><header class="topbar"><div class="crumb">管理后台 <span>/</span> {{ active.label }}</div>
      <div class="profile"><span class="avatar admin">管</span><div><b>{{ currentUser?.phone }}</b><small>管理员</small></div><button class="link-button" @click="logout">退出管理</button></div>
    </header>
    <div class="page"><div class="page-title"><div><span class="overline">ADMINISTRATION</span><h1>{{ active.label }}</h1><p>{{ active.description }}</p></div><button class="btn outline" @click="load" :disabled="loading">↻ 刷新数据</button></div>
      <p class="notice neutral">ⓘ 当前为只读管理视图，管理员不参与邀请和返利。</p>
      <p v-if="error" class="notice error">{{ error }}</p>
      <section class="panel admin-panel"><div class="panel-title compact"><div><h2>{{ active.label }}列表</h2><p>共 {{ data.total || 0 }} 条记录</p></div>
        <label v-if="selected === 'recharges'" class="search"><input v-model.trim="keyword" placeholder="搜索手机号或订单号" @keyup.enter="load"><button @click="load">查询</button></label>
      </div>
      <div v-if="loading" class="empty">正在读取数据…</div>
      <div v-else-if="data.list?.length" class="table-scroll"><table><thead><tr><th v-for="column in active.columns" :key="column.key">{{ column.label }}</th></tr></thead>
        <tbody><tr v-for="(row, i) in data.list" :key="row.id || row.inviteeUserId || row.outTradeNo || i">
          <td v-for="column in active.columns" :key="column.key">{{ format(row[column.key], column.key) }}</td>
        </tr></tbody></table></div>
      <div v-else class="empty"><span>⌕</span><b>暂无数据</b><small>新增数据后会显示在这里</small></div>
      <div class="pager"><span>第 1 页 · 每页 20 条</span><span>{{ data.total || 0 }} 条结果</span></div>
      </section><footer class="footer">青禾计划 · 邀请返利演示 <span>管理权限仅限只读查询</span></footer>
    </div></main>
  </div>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'
const router = useRouter()
const currentUser = ref(window.currentUser || null)
const selected = ref('users')
const data = ref({ list: [], total: 0 })
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const tabs = [
  { key: 'users', label: '用户', icon: '◉', description: '查看普通演示用户账户和邀请码。', endpoint: api.admin.users,
    columns: [{ key: 'phone', label: '手机号' }, { key: 'inviteCode', label: '邀请码' }, { key: 'balance', label: '站内余额' }, { key: 'createdAt', label: '注册时间' }] },
  { key: 'relations', label: '邀请关系', icon: '♧', description: '查看直接邀请关系。', endpoint: api.admin.relations,
    columns: [{ key: 'inviterPhone', label: '邀请人' }, { key: 'inviteePhone', label: '被邀请人' }, { key: 'inviteCode', label: '邀请码快照' }, { key: 'boundAt', label: '绑定时间' }] },
  { key: 'recharges', label: '充值订单', icon: '＋', description: '查看模拟成功充值。', endpoint: api.admin.recharges,
    columns: [{ key: 'outTradeNo', label: '订单号' }, { key: 'phone', label: '充值用户' }, { key: 'amount', label: '充值金额' }, { key: 'rebateAmount', label: '返利金额' }, { key: 'paidAt', label: '成功时间' }] },
  { key: 'rebates', label: '返利流水', icon: '↗', description: '查看邀请人收到的返利。', endpoint: api.admin.rebates,
    columns: [{ key: 'ownerPhone', label: '返利归属人' }, { key: 'inviteePhone', label: '充值用户' }, { key: 'sourceOrderNo', label: '来源订单' }, { key: 'amount', label: '返利金额' }, { key: 'createdAt', label: '入账时间' }] },
]
const active = computed(() => tabs.find((tab) => tab.key === selected.value) || tabs[0])
function format(value, key) {
  if (key.endsWith('At') && value) return new Date(value).toLocaleString('zh-CN')
  if (['balance', 'amount', 'rebateAmount'].includes(key) && value !== undefined) return '￥' + value
  return value || '—'
}
async function load() {
  loading.value = true; error.value = ''
  try {
    data.value = (selected.value === 'recharges'
      ? await api.admin.recharges(keyword.value)
      : await active.value.endpoint()).data
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function logout() { api.logout().finally(() => router.replace('/login')) }
watch(selected, load)
onMounted(async () => {
  try { currentUser.value = (await api.me()).data } catch { router.replace('/login'); return }
  await load()
})
</script>
