/* ============================================================
   请求层（uni.request 封装）
   · baseURL 见 config.js
   · 鉴权：请求头 authentication，token 取自本地存储
   · 统一判定 code === 1 为成功；失败按服务端 msg 提示
   · 取不到数据时静默失败，由页面呈现空态，不向用户弹提示
   ============================================================ */
import { BASE_URL, STORAGE_KEYS, AUTH_WHITE_LIST } from './config'

/* 仅供 Promise 拒绝使用的内部文案，不面向用户展示 */
const UNAVAILABLE = '数据暂时不可用'

let lastNotice = { msg: '', at: 0 }

/* 同一文案 5 秒内只提示一次：一个页面常并发多个请求 */
function notice(msg) {
  const now = Date.now()
  if (lastNotice.msg === msg && now - lastNotice.at < 5000) return
  lastNotice = { msg, at: now }
  uni.showToast({ title: msg, icon: 'none', duration: 2200 })
}

function isWhite(url) {
  return AUTH_WHITE_LIST.some((p) => url.indexOf(p) === 0)
}

let redirectingToLogin = false

function handleAuthExpired() {
  uni.removeStorageSync(STORAGE_KEYS.token)
  uni.removeStorageSync(STORAGE_KEYS.member)
  if (redirectingToLogin) return
  redirectingToLogin = true
  notice('登录状态已失效')
  setTimeout(() => {
    redirectingToLogin = false
  }, 3000)
}

export function request(options) {
  const { url, method = 'GET', data, loading = false, loadingText = '加载中', auth = true } = options
  const token = uni.getStorageSync(STORAGE_KEYS.token)

  if (loading) uni.showLoading({ title: loadingText, mask: true })

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(auth && !isWhite(url) && token ? { authentication: token } : {})
      },
      success: (res) => {
        const body = res.data

        /* 非约定的 { code, msg, data } 结构：多为网关错误页或后端未起，静默失败 */
        if (
          res.statusCode !== 200 ||
          !body ||
          typeof body !== 'object' ||
          !('code' in body)
        ) {
          reject(new Error(UNAVAILABLE))
          return
        }

        if (body.code === 1) {
          resolve(body.data)
          return
        }

        const msg = body.msg || '请求失败'
        if (msg === '登录状态已失效') {
          handleAuthExpired()
          reject(new Error(msg))
          return
        }
        notice(msg)
        reject(new Error(msg))
      },
      fail: () => {
        /* 网络不可达 / 域名未配置 / 后端未启动：静默失败 */
        reject(new Error(UNAVAILABLE))
      },
      complete: () => {
        if (loading) uni.hideLoading()
      }
    })
  })
}

export const get = (url, data, options = {}) => request({ url, method: 'GET', data, ...options })
export const post = (url, data, options = {}) => request({ url, method: 'POST', data, ...options })
export const put = (url, data, options = {}) => request({ url, method: 'PUT', data, ...options })
export const del = (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })
