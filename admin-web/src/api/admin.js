/* ============================================================
   管理端接口封装（与《接口文档.md》46 个接口一一对应）
   命名即契约：函数名 → 方法与路径，便于与文档对照
   ============================================================ */
import request from './request'

/* ---------------- 店员 /admin/staff ---------------- */
export const staffApi = {
  login: (data) => request.post('/staff/login', data),
  logout: () => request.post('/staff/logout'),
  add: (data) => request.post('/staff', data),
  page: (params) => request.get('/staff/page', { params }),
  setStatus: (status, id) => request.post(`/staff/status/${status}`, null, { params: { id } }),
  getById: (id) => request.get(`/staff/${id}`),
  update: (data) => request.put('/staff', data),
  editPassword: (data) => request.put('/staff/editPassword', data),
  remove: (id) => request.delete('/staff', { params: { id } })
}

/* ---------------- 商品分类 /admin/goodsCategory ---------------- */
export const categoryApi = {
  add: (data) => request.post('/goodsCategory', data),
  page: (params) => request.get('/goodsCategory/page', { params }),
  remove: (id) => request.delete('/goodsCategory', { params: { id } }),
  update: (data) => request.put('/goodsCategory', data),
  setStatus: (status, id) => request.post(`/goodsCategory/status/${status}`, null, { params: { id } }),
  list: (type) => request.get('/goodsCategory/list', { params: { type } })
}

/* ---------------- 通用 /admin/common ---------------- */
export const commonApi = {
  upload: (file, onProgress) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/common/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  }
}

/* ---------------- 店铺 /admin/shop ---------------- */
export const shopApi = {
  setStatus: (status) => request.put(`/shop/${status}`),
  getStatus: () => request.get('/shop/status')
}

/* ---------------- 工作台 /admin/workspace ---------------- */
export const workspaceApi = {
  businessData: () => request.get('/workspace/businessData'),
  overviewOrders: () => request.get('/workspace/overviewOrders'),
  overviewGoods: () => request.get('/workspace/overviewGoods'),
  overviewBundles: () => request.get('/workspace/overviewBundles')
}

/* ---------------- 商品 /admin/goods ---------------- */
export const goodsApi = {
  add: (data) => request.post('/goods', data),
  page: (params) => request.get('/goods/page', { params }),
  remove: (ids) => request.delete('/goods', { params: { ids: ids.join(',') } }),
  getById: (id) => request.get(`/goods/${id}`),
  update: (data) => request.put('/goods', data),
  list: (categoryId) => request.get('/goods/list', { params: { categoryId } }),
  setStatus: (status, id) => request.post(`/goods/status/${status}`, null, { params: { id } })
}

/* ---------------- 组合箱 /admin/bundle ---------------- */
export const bundleApi = {
  add: (data) => request.post('/bundle', data),
  page: (params) => request.get('/bundle/page', { params }),
  remove: (ids) => request.delete('/bundle', { params: { ids: ids.join(',') } }),
  getById: (id) => request.get(`/bundle/${id}`),
  update: (data) => request.put('/bundle', data),
  setStatus: (status, id) => request.post(`/bundle/status/${status}`, null, { params: { id } })
}

/* ---------------- 订单 /admin/order ---------------- */
export const orderApi = {
  conditionSearch: (params) => request.get('/order/conditionSearch', { params }),
  /*
    顶栏角标每 60 秒轮询一次，属于后台行为：失败时静默（silent），
    不要把提示弹给使用者 —— 数据不显示已经足够说明问题。
  */
  statistics: () => request.get('/order/statistics', { silent: true }),
  details: (id) => request.get(`/order/details/${id}`),
  confirm: (id) => request.put('/order/confirm', { id }),
  rejection: (data) => request.put('/order/rejection', data),
  cancel: (data) => request.put('/order/cancel', data),
  delivery: (id) => request.put(`/order/delivery/${id}`),
  complete: (id) => request.put(`/order/complete/${id}`)
}

/* ---------------- 报表 /admin/report ---------------- */
export const reportApi = {
  turnover: (params) => request.get('/report/turnoverStatistics', { params }),
  members: (params) => request.get('/report/userStatistics', { params }),
  orders: (params) => request.get('/report/ordersStatistics', { params }),
  top10: (params) => request.get('/report/top10', { params })
}
