/* ============================================================
   用户端接口封装（与《接口文档-用户端.md》24 个接口一一对应）
   命名即契约：函数名对应 模块.动作，便于与文档逐条对照
   ============================================================ */
import { get, post, put, del } from '@/common/request'

/* 1 会员 /user/member —— code 换 token，openid 由服务端换取 */
export const memberApi = {
  login: (code) => post('/user/member/login', { code }, { auth: false })
}

/* 2 店铺 /user/shop —— 白名单，未登录也能查营业状态 */
export const shopApi = {
  status: () => get('/user/shop/status', undefined, { auth: false })
}

/* 3 商品分类 /user/goodsCategory —— 只返回启用中的分类 */
export const categoryApi = {
  list: (type) => get('/user/goodsCategory/list', type === undefined ? {} : { type })
}

/* 4 商品 /user/goods —— 只返回上架商品，按分类查或按关键词搜 */
export const goodsApi = {
  listByCategory: (categoryId) => get('/user/goods/list', { categoryId }),
  search: (name) => get('/user/goods/search', { name })
}

/* 5 组合箱 /user/bundle */
export const bundleApi = {
  listByCategory: (categoryId) => get('/user/bundle/list', { categoryId }),
  goods: (id) => get(`/user/bundle/goods/${id}`)
}

/* 6 购物车 /user/cart —— memberId 由服务端从 token 解析，前端不传 */
export const cartApi = {
  add: (data) => post('/user/cart/add', data),
  sub: (data) => post('/user/cart/sub', data),
  list: () => get('/user/cart/list'),
  clean: () => del('/user/cart/clean'),
  /* 移除一行：id 走 query（契约 7.5），用于清掉已下架、不想要的项 */
  removeLine: (id) => del(`/user/cart?id=${id}`)
}

/* 7 收货地址 /user/address */
export const addressApi = {
  list: () => get('/user/address/list'),
  detail: (id) => get(`/user/address/${id}`),
  add: (data) => post('/user/address', data),
  update: (data) => put('/user/address', data),
  setDefault: (id) => put('/user/address/default', { id }),
  /* 删除的 id 走 query：契约 8.6 要求 query 传递，后端用 @RequestParam 接收 */
  remove: (id) => del(`/user/address?id=${id}`),
  getDefault: () => get('/user/address/default')
}

/* 8 订单 /user/order —— 注意：提交与支付是两个独立动作（见接口文档 9.1 说明） */
export const orderApi = {
  submit: (data) => post('/user/order/submit', data),
  /* 支付：本项目不接真实微信支付，点击即调用本接口推进状态 */
  payment: (orderNumber) => put('/user/order/payment', { orderNumber, payMethod: 1 }),
  history: (params) => get('/user/order/historyOrders', params),
  detail: (id) => get(`/user/order/orderDetail/${id}`),
  cancel: (id) => put(`/user/order/cancel/${id}`),
  repetition: (id) => post(`/user/order/repetition/${id}`),
  reminder: (id) => get(`/user/order/reminder/${id}`)
}
