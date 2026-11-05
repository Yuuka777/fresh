/* ============================================================
   业务字典与展示映射
   与后端常量、数据库注释一一对应，避免各处硬编码
   ============================================================ */

export const ORDER_STATUS = {
  TO_BE_PAID: 1,
  TO_BE_CONFIRMED: 2,
  CONFIRMED: 3,
  DELIVERY_IN_PROGRESS: 4,
  COMPLETED: 5,
  CANCELLED: 6,
  REFUND: 7
}

/* 订单状态：文案 + 语义色。色只用三档，避免标签花成调色板 */
export const ORDER_STATUS_MAP = {
  1: { label: '待付款', tone: 'warn' },
  2: { label: '待接单', tone: 'accent' },
  3: { label: '已接单', tone: 'info' },
  4: { label: '派送中', tone: 'info' },
  5: { label: '已完成', tone: 'ok' },
  6: { label: '已取消', tone: 'muted' },
  7: { label: '退款', tone: 'muted' }
}

/* 订单页签：与《接口文档》2.8 页签表严格一致 */
export const ORDER_TABS = [
  { key: 'all', label: '全部订单', status: undefined },
  { key: 'paid', label: '待付款', status: 1, badgeKey: 'toBePaid' },
  { key: 'confirm', label: '待接单', status: 2, badgeKey: 'toBeConfirmed' },
  { key: 'delivery', label: '待派送', status: 3, badgeKey: 'confirmed' },
  { key: 'inProgress', label: '派送中', status: 4, badgeKey: 'deliveryInProgress' },
  { key: 'done', label: '已完成', status: 5 },
  { key: 'cancelled', label: '已取消', status: 6 }
]

export const PAY_STATUS_MAP = {
  0: { label: '未支付', tone: 'warn' },
  1: { label: '已支付', tone: 'ok' },
  2: { label: '已退款', tone: 'muted' }
}

/* 温区：生鲜的冷链语言，配色同时用于无图占位色块 */
export const STORAGE_ZONE = {
  NORMAL: 1,
  CHILLED: 2,
  FROZEN: 3
}

export const STORAGE_ZONE_MAP = {
  1: { label: '常温', short: '常', bg: '#E7EDE6', fg: '#33503F' },
  2: { label: '冷藏', short: '冷', bg: '#D9EEE9', fg: '#0B5A47' },
  3: { label: '冷冻', short: '冻', bg: '#DCE4F0', fg: '#22456F' }
}

/*
  启停状态：数字含义相同（0 停 / 1 启），但说法随对象而变。
  店员是「启用 / 禁用」，商品是「上架 / 下架」，组合箱是「起售 / 停售」——
  用同一套文案套在所有对象上，会说出「店员已下架」这种不通的话。
*/
export const STAFF_STATUS_MAP = {
  0: { label: '已禁用', tone: 'muted' },
  1: { label: '已启用', tone: 'ok' }
}

export const CATEGORY_STATUS_MAP = {
  0: { label: '已禁用', tone: 'muted' },
  1: { label: '已启用', tone: 'ok' }
}

export const GOODS_STATUS_MAP = {
  0: { label: '已下架', tone: 'muted' },
  1: { label: '已上架', tone: 'ok' }
}

export const BUNDLE_STATUS_MAP = {
  0: { label: '已停售', tone: 'muted' },
  1: { label: '已起售', tone: 'ok' }
}

export const CATEGORY_TYPE_MAP = {
  1: { label: '商品分类' },
  2: { label: '组合分类' }
}

export const DELIVERY_STATUS_MAP = {
  0: { label: '预约时段' },
  1: { label: '尽快送达' }
}

/* —— 金额格式化：前端只做展示，不做运算（运算在服务端 BigDecimal） —— */
export function money(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

export function yuan(v) {
  return `¥ ${money(v)}`
}

export function orderStatusLabel(s) {
  return ORDER_STATUS_MAP[s]?.label || '未知'
}

export function orderStatusTone(s) {
  return ORDER_STATUS_MAP[s]?.tone || 'muted'
}

export function zoneLabel(z) {
  return STORAGE_ZONE_MAP[z]?.label || '常温'
}
