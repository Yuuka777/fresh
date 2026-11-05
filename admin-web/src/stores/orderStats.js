/* 订单页签角标（契约 2.8.2 的四个数字）：顶栏与订单页共用一份状态。
   两者原本各存一份，WebSocket 收到来单 / 催单只刷新了顶栏那一份，
   店员停在订单页时页签角标不动 —— 所以提成共享 store，一处刷新两处都变。 */
import { defineStore } from 'pinia'
import { orderApi } from '@/api/admin'

export const useOrderStatsStore = defineStore('orderStats', {
  state: () => ({ toBePaid: 0, toBeConfirmed: 0, confirmed: 0, deliveryInProgress: 0 }),
  actions: {
    async load() {
      try {
        Object.assign(this, await orderApi.statistics())
      } catch {
        /* 角标失败不影响主流程 */
      }
    }
  }
})
