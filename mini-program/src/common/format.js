/* ============================================================
   展示格式化（前端只做展示，不做金额运算）
   ============================================================ */

/** 金额：保留两位小数字符串 */
export function money(v) {
  const n = Number(v)
  if (isNaN(n)) return '0.00'
  return n.toFixed(2)
}

/** 拼接带符号的金额，模板里配合 .xh-price 使用 */
export function price(v) {
  return `¥${money(v)}`
}

/** 金额求和（仅用于前端小计展示，正式金额以服务端为准） */
export function sumAmount(list, key = 'amount') {
  return Number(list.reduce((s, i) => s + Number(i[key] || 0), 0).toFixed(2))
}

/** 数量 × 单价（用于购物车小计展示） */
export function lineAmount(price, number) {
  return Number((Number(price || 0) * Number(number || 0)).toFixed(2))
}

/** 相对时间：今天 / 昨天 / 具体日期，用于订单列表 */
export function friendlyTime(timeStr) {
  if (!timeStr) return ''
  const t = String(timeStr).replace(/-/g, '/')
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(timeStr)
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const diff = startOfToday - new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const day = 86400000
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (diff === 0) return `今天 ${hm}`
  if (diff === day) return `昨天 ${hm}`
  if (diff === day * 2) return `前天 ${hm}`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${hm}`
}

/** 只保留 月-日 时:分，用于详情页时间轴 */
export function shortTime(timeStr) {
  if (!timeStr) return ''
  const s = String(timeStr)
  return s.length >= 16 ? s.slice(5, 16).replace('-', '/') : s
}

function pad(n) {
  return String(n).padStart(2, '0')
}

/** 把 Date 输出成后端要求的 'yyyy-MM-dd HH:mm:ss' */
export function toServerTime(date) {
  const d = date instanceof Date ? date : new Date(date)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}:${pad(d.getSeconds())}`
}

/** 倒计时：返回 { mm, ss, text, expired }，用于待付款 15 分钟倒计时 */
export function countdown(deadline) {
  const end = deadline instanceof Date ? deadline.getTime() : new Date(String(deadline).replace(/-/g, '/')).getTime()
  const left = Math.max(0, end - Date.now())
  const total = Math.floor(left / 1000)
  const m = Math.floor(total / 60)
  const s = total % 60
  return {
    expired: left <= 0,
    mm: pad(m),
    ss: pad(s),
    text: `${pad(m)}:${pad(s)}`
  }
}

/** 手机号脱敏（个人中心展示用） */
export function maskPhone(phone) {
  const s = String(phone || '')
  return s.length === 11 ? `${s.slice(0, 3)}****${s.slice(7)}` : s
}
