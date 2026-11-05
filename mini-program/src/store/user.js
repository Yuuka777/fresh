/* ============================================================
   登录态（会员）
   · token 存本地存储并放入请求头 authentication
   · 微信登录只传 code，openid 由服务端换取（见接口文档 2.1）
   · 不做任何本地账号兜底：登录失败如实提示
   ============================================================ */
import { defineStore } from 'pinia'
import { memberApi } from '@/api/user'
import { STORAGE_KEYS } from '@/common/config'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    member: null,
    logging: false
  }),

  getters: {
    logged: (s) => !!s.token,
    nickname: (s) => (s.member && s.member.name) || '鲜禾用户',
    openid: (s) => (s.member && s.member.openid) || '',
    memberId: (s) => (s.member && s.member.id) || null
  },

  actions: {
    /* 冷启动时恢复本地登录态 */
    restore() {
      this.token = uni.getStorageSync(STORAGE_KEYS.token) || ''
      this.member = uni.getStorageSync(STORAGE_KEYS.member) || null
    },

    /* 微信登录：uni.login 拿 code → 后端 jscode2session 换 openid 并签发 token */
    async login() {
      if (this.logging) return null
      this.logging = true
      try {
        const code = await new Promise((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: (res) => (res && res.code ? resolve(res.code) : reject(new Error('未获取到登录凭证'))),
            fail: () => reject(new Error('微信登录失败'))
          })
        })
        const data = await memberApi.login(code)
        this.token = data.token
        this.member = { id: data.id, openid: data.openid }
        uni.setStorageSync(STORAGE_KEYS.token, data.token)
        uni.setStorageSync(STORAGE_KEYS.member, this.member)
        return data
      } catch (e) {
        /* 常见于 H5 预览或开发者工具未配置 AppID */
        uni.showToast({
          title: e.message === '微信登录失败' ? '微信登录失败，请在微信开发者工具中运行' : e.message,
          icon: 'none'
        })
        throw e
      } finally {
        this.logging = false
      }
    },

    /* 需要登录的页面统一入口：已登录直接放行，否则静默登录 */
    async ensureLogin() {
      if (this.logged) return true
      const data = await this.login()
      return !!data
    },

    logout() {
      this.token = ''
      this.member = null
      uni.removeStorageSync(STORAGE_KEYS.token)
      uni.removeStorageSync(STORAGE_KEYS.member)
    }
  }
})
