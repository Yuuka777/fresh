/* ============================================================
   业务字典与展示映射（与后端常量、数据库注释一一对应）
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

/* 订单状态：文案 + 语义色档（文案给用户看，档位决定配色） */
export const ORDER_STATUS_MAP = {
  1: { label: '待付款', tone: 'warn', desc: '请在 15 分钟内完成支付' },
  2: { label: '待接单', tone: 'accent', desc: '门店正在确认订单' },
  3: { label: '已接单', tone: 'info', desc: '门店正在分拣打包' },
  4: { label: '派送中', tone: 'info', desc: '骑手已取货，正在配送' },
  5: { label: '已完成', tone: 'ok', desc: '订单已送达' },
  6: { label: '已取消', tone: 'muted', desc: '订单已取消' },
  7: { label: '退款', tone: 'muted', desc: '退款处理中' }
}

/* 历史订单页签 */
export const ORDER_TABS = [
  { key: 'all', label: '全部', status: undefined },
  { key: 'unpaid', label: '待付款', status: 1 },
  { key: 'doing', label: '进行中', status: 2 },
  { key: 'inProgress', label: '派送中', status: 4 },
  { key: 'done', label: '已完成', status: 5 }
]

export const PAY_STATUS_MAP = {
  0: { label: '未支付', tone: 'warn' },
  1: { label: '已支付', tone: 'ok' },
  2: { label: '已退款', tone: 'muted' }
}

/* 温区：配色同时用于无图商品占位块 */
export const STORAGE_ZONE_MAP = {
  1: { label: '常温', short: '常', bg: '#E7EDE6', fg: '#33503F' },
  2: { label: '冷藏', short: '冷', bg: '#D9EEE9', fg: '#0B5A47' },
  3: { label: '冷冻', short: '冻', bg: '#DCE4F0', fg: '#22456F' }
}

export const DELIVERY_STATUS_MAP = {
  1: '尽快送达',
  0: '预约时段'
}

export function orderStatusLabel(status) {
  return (ORDER_STATUS_MAP[status] || {}).label || '未知状态'
}

export function orderStatusTone(status) {
  return (ORDER_STATUS_MAP[status] || {}).tone || 'muted'
}

export function orderStatusDesc(status) {
  return (ORDER_STATUS_MAP[status] || {}).desc || ''
}

export function zoneTone(zone) {
  return STORAGE_ZONE_MAP[zone] || STORAGE_ZONE_MAP[1]
}

/* 订单是否可由用户取消：仅未进入履约阶段（待付款 / 待接单） */
export function canCancelOrder(status) {
  return status === ORDER_STATUS.TO_BE_PAID || status === ORDER_STATUS.TO_BE_CONFIRMED
}

/* 订单是否可催单：待接单（催门店接单）、已接单（催出餐）、派送中（催配送）三种诉求都成立。
   参照工程只给了待接单，这里放宽到三个状态 */
export function canRemindOrder(status) {
  return (
    status === ORDER_STATUS.TO_BE_CONFIRMED ||
    status === ORDER_STATUS.CONFIRMED ||
    status === ORDER_STATUS.DELIVERY_IN_PROGRESS
  )
}
