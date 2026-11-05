/* ============================================================
   商品卡的加减购动作（首页与搜索结果页共用，避免两处逻辑漂移）
   · 数量由服务端 +1 / -1，前端不传数量（与苍穹外卖口径一致）
   · 有规格的商品先弹规格层，选完再入车
   · 不做本地乐观更新：入车后由 cart store 重新拉取，服务端为唯一事实来源
   ============================================================ */
import { ref } from 'vue'
import { useCartStore } from '@/store/cart'

export function useCartActions() {
  const cart = useCartStore()
  const specPopup = ref({ visible: false, goods: {}, submitting: false })

  async function addToCart(payload) {
    try {
      await cart.add(payload)
      uni.showToast({ title: '已加入购物车', icon: 'none' })
    } catch {
      /* 请求层已提示 */
    }
  }

  /* 有规格 → 打开规格弹层；无规格 → 直接加一件 */
  function plusGoods(goods) {
    if ((goods.specs || []).length) {
      specPopup.value = { visible: true, goods, submitting: false }
      return
    }
    addToCart({ goodsId: goods.id })
  }

  function minusGoods(goods) {
    cart.sub({ goodsId: goods.id }).catch(() => {})
  }

  async function confirmSpec({ specText }) {
    specPopup.value.submitting = true
    try {
      await addToCart({ goodsId: specPopup.value.goods.id, specText })
      specPopup.value.visible = false
    } finally {
      specPopup.value.submitting = false
    }
  }

  function closeSpec() {
    specPopup.value.visible = false
  }

  return { cart, specPopup, addToCart, plusGoods, minusGoods, confirmSpec, closeSpec }
}
