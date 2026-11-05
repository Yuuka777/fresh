/* ============================================================
   购物车
   · 服务端是唯一事实来源：每次增删后重新拉取列表
   · memberId 由服务端从 token 解析，前端不传
   · 首页的加减按钮据此计算「已加购数量」
   ============================================================ */
import { defineStore } from 'pinia'
import { cartApi } from '@/api/user'

/* 同款判定键：商品/组合箱 + 规格文本（与后端判重口径一致） */
function lineKey({ goodsId, bundleId, specText }) {
  return `${goodsId || 0}-${bundleId || 0}-${specText || ''}`
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    loading: false
  }),

  getters: {
    count: (s) => s.items.reduce((n, i) => n + Number(i.number || 0), 0),
    amount: (s) =>
      Number(s.items.reduce((sum, i) => sum + Number(i.amount || 0), 0).toFixed(2)),
    empty: (s) => s.items.length === 0
  },

  actions: {
    async load(silent = false) {
      this.loading = true
      try {
        const list = await cartApi.list()
        this.items = Array.isArray(list) ? list : []
      } catch {
        /* 请求层已提示；这里保持空车，页面呈现空态 */
        this.items = []
      } finally {
        this.loading = false
      }
      return this.items
    },

    /* 加一件：数量由服务端 +1，前端不传数量（与苍穹外卖口径一致） */
    async add(payload) {
      await cartApi.add(payload)
      await this.load(true)
    },

    async sub(payload) {
      await cartApi.sub(payload)
      await this.load(true)
    },

    async clean() {
      await cartApi.clean()
      this.items = []
    },

    /* 移除一行：已下架的商品不能结算，得有这个出口 */
    async removeLine(id) {
      await cartApi.removeLine(id)
      await this.load(true)
    },

    /* 某商品/规格已加购数量，供首页底部加减控件显示 */
    numberInCart(target) {
      const key = lineKey(target)
      return this.items
        .filter((i) => lineKey(i) === key)
        .reduce((n, i) => n + Number(i.number || 0), 0)
    }
  }
})
