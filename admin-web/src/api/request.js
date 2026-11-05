/* ============================================================
   请求层
   · baseURL = /api（开发经 Vite 代理、生产经 nginx 转发到后端 /admin/**）
   · 请求头 token，取自 Cookie（key = token）
   · 统一判定 code === 1 为成功，失败交由拦截器提示
   · 不携带任何演示数据或本地数据兜底：后端未就绪时如实提示
   ============================================================ */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getCookie, removeCookie } from '@/utils/cookie'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = getCookie('token')
  if (token) config.headers.token = token
  return config
})

/* 防止并发请求同时弹多条提示 */
let redirecting = false
let lastNotice = { msg: '', at: 0 }

function notice(msg) {
  /* 同一文案 5 秒内只提示一次：一次页面加载会并发数个请求，避免刷屏 */
  const now = Date.now()
  if (lastNotice.msg === msg && now - lastNotice.at < 5000) return
  lastNotice = { msg, at: now }
  ElMessage.error(msg)
}

/* 交付形态下后端尚未就绪，这个文案是前端唯一会主动暴露的运行前提 */
const BACKEND_DOWN = '后端服务未就绪（/api 无响应），请先启动后端服务'

service.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 1) return body.data

    /* 响应不是约定的 { code, msg, data } 结构：多为网关/代理返回的错误页 */
    if (!body || typeof body !== 'object' || !('code' in body)) {
      notice(BACKEND_DOWN)
      return Promise.reject(new Error(BACKEND_DOWN))
    }

    const msg = body.msg || '请求失败'
    if (msg === '登录状态已失效') {
      removeCookie('token')
      if (!redirecting) {
        redirecting = true
        ElMessage.error(msg)
        import('@/router').then(({ default: router }) => {
          router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
          redirecting = false
        })
      }
      return Promise.reject(new Error(msg))
    }

    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response && error.response.status
    /*
      4xx 也必须给出中文文案：直接用 axios 的 error.message 会把
      「Request failed with status code 404」这类英文技术错误显示给使用者。
    */
    const msg =
      error.code === 'ECONNABORTED'
        ? '请求超时，请检查网络'
        : !error.response || status >= 500
          ? BACKEND_DOWN
          : status === 404
            ? '请求的接口不存在'
            : '请求被拒绝，请检查提交的内容'
    // 后台轮询类请求（如顶栏角标）失败时静默，不打扰使用者
    if (!(error.config && error.config.silent)) {
      notice(msg)
    }
    return Promise.reject(error)
  }
)

export default service
