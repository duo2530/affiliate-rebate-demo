import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import UserDashboard from './views/UserDashboard.vue'
import AdminDashboard from './views/AdminDashboard.vue'
import { api } from './services/api'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/register', component: RegisterView, meta: { public: true } },
    { path: '/user', component: UserDashboard, meta: { role: 'USER' } },
    { path: '/admin', component: AdminDashboard, meta: { role: 'ADMIN' } },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  try {
    const user = (await api.me()).data
    window.currentUser = user
    if (to.meta.role && user.role !== to.meta.role) return user.role === 'ADMIN' ? '/admin' : '/user'
    return true
  } catch {
    return '/login'
  }
})

createApp(App).use(router).mount('#app')
