/* 登录态与店铺营业状态：读多写少，用 Pinia 全局持有 */
import { defineStore } from 'pinia'
import { staffApi, shopApi } from '@/api/admin'
import { setCookie, removeCookie, getCookie } from '@/utils/cookie'

export const useUserStore = defineStore('user', {
  state: () => ({
    id: Number(localStorage.getItem('xh-staff-id') || 0),
    userName: localStorage.getItem('xh-staff-name-account') || '',
    name: localStorage.getItem('xh-staff-name') || '',
    token: getCookie('token'),
    shopStatus: 1
  }),
  getters: {
    logged: (s) => Boolean(s.token),
    displayName: (s) => s.name || s.userName || '未登录'
  },
  actions: {
    async login(payload) {
      const data = await staffApi.login(payload)
      this.id = data.id
      this.userName = data.userName
      this.name = data.name
      this.token = data.token
      setCookie('token', data.token)
      localStorage.setItem('xh-staff-id', String(data.id))
      localStorage.setItem('xh-staff-name', data.name)
      localStorage.setItem('xh-staff-name-account', data.userName)
      return data
    },
    async logout() {
      try {
        await staffApi.logout()
      } catch {
        /* 退出失败不阻断前端清理 */
      }
      this.clear()
    },
    clear() {
      this.token = ''
      removeCookie('token')
      localStorage.removeItem('xh-staff-id')
      localStorage.removeItem('xh-staff-name')
      localStorage.removeItem('xh-staff-name-account')
    },
    async fetchShopStatus() {
      this.shopStatus = await shopApi.getStatus()
      return this.shopStatus
    },
    async setShopStatus(status) {
      await shopApi.setStatus(status)
      this.shopStatus = status
    }
  }
})
