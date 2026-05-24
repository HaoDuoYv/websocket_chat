import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import './style.css'

// JWT 请求拦截器
axios.interceptors.request.use((config) => {
  const userData = localStorage.getItem('user')
  if (userData) {
    try {
      const user = JSON.parse(userData)
      if (user.token) {
        config.headers.Authorization = `Bearer ${user.token}`
      }
    } catch {
      // ignore
    }
  }
  return config
})

// 401 响应拦截器
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      if (!url.includes('/auth/login') && !url.includes('/auth/register') && !url.includes('/admin/')) {
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')
