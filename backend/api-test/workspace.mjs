/**
 * 工作台模块接口测试（《接口文档》2.5，共 4 个接口）。
 *
 * 统计类接口不能断言「等于某个写死的数字」——库里随时可能有新订单。
 * 这里的口径统一是：**用同一条 SQL 从库里算出期望值，再与接口返回比对**。
 * 今日态的三项（已完成 / 已取消 / 全部）额外做一次「调接口前后各取一次库快照」：
 * 超时未支付任务每分钟扫一次，可能恰好在这一瞬把待付款单置为已取消。
 */
import { call, eq, ok, section, sql } from './lib.mjs'

const pad = (n) => String(n).padStart(2, '0')
const day = (offset) => {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const TODAY = day(0)
const TOMORROW = day(1)

/** 今天的时间窗（左闭右开，与 order_time 的秒级精度等价） */
const TODAY_WINDOW = `order_time >= '${TODAY} 00:00:00' AND order_time < '${TOMORROW} 00:00:00'`

const count = (where) => Number(sql(`SELECT COUNT(*) FROM order_main WHERE ${where}`))

/** 今日三项快照，用于容忍「超时任务恰好在这一瞬干活」 */
function todaySnapshot() {
  return {
    completedOrders: count(`${TODAY_WINDOW} AND status = 5`),
    cancelledOrders: count(`${TODAY_WINDOW} AND status = 6`),
    allOrders: count(TODAY_WINDOW)
  }
}

/** 接口值与两个库快照之一相符即可（统计窗口内的并发变化是正常现象） */
function eqSnapshot(name, actual, before, after) {
  ok(
    name,
    actual === before || actual === after,
    `接口 ${actual}，库内 ${before} → ${after}`
  )
}

export default async function run() {
  const login = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  const token = login.json?.data?.token

  /* ---------------- 鉴权 ---------------- */
  section('1.3 鉴权（工作台与报表同样要 token）')
  const noToken = await call('GET', '/admin/workspace/businessData')
  eq('工作台不带 token → HTTP 200（而非 401）', noToken.status, 200)
  eq('工作台不带 token → code = 0', noToken.json?.code, 0)
  eq('工作台不带 token → msg', noToken.json?.msg, '登录状态已失效')
  const noTokenReport = await call('GET', '/admin/report/turnoverStatistics')
  eq('报表不带 token → msg', noTokenReport.json?.msg, '登录状态已失效')

  /* ---------------- 2.5.1 今日运营数据 ---------------- */
  section('2.5.1 今日运营数据  GET /admin/workspace/businessData')

  const before = todaySnapshot()
  const biz = await call('GET', '/admin/workspace/businessData', { token })
  eq('code = 1', biz.json?.code, 1)

  const dbTurnover = Number(
    sql(`SELECT IFNULL(SUM(amount), 0) FROM order_main WHERE status = 5 AND ${TODAY_WINDOW}`)
  )
  const dbValid = count(`${TODAY_WINDOW} AND status = 5`)
  const dbTotal = before.allOrders
  const dbNewMembers = Number(
    sql(`SELECT COUNT(*) FROM member WHERE create_time >= '${TODAY} 00:00:00' AND create_time < '${TOMORROW} 00:00:00'`)
  )

  const data = biz.json?.data || {}
  ok('五个字段齐全',
    ['turnover', 'validOrderCount', 'orderCompletionRate', 'unitPrice', 'newMembers'].every((k) => k in data),
    JSON.stringify(Object.keys(data)))

  eq('营业额 = 库内今日已完成订单金额合计', Number(data.turnover).toFixed(2), dbTurnover.toFixed(2))
  eq('有效订单数 = 库内今日已完成订单数', data.validOrderCount, dbValid)
  eq('新增会员数 = 库内今日注册会员数', data.newMembers, dbNewMembers)

  // 完成率与客单价都是算出来的，比对到 4 / 2 位小数（服务端的保留位数）
  const expectRate = dbTotal === 0 || dbValid === 0 ? 0 : Math.round((dbValid / dbTotal) * 10000) / 10000
  eq('订单完成率 = 有效订单 / 总订单', Number(data.orderCompletionRate).toFixed(4), expectRate.toFixed(4))
  const expectUnit = dbValid === 0 ? 0 : Math.round((dbTurnover / dbValid) * 100) / 100
  eq('平均客单价 = 营业额 / 有效订单数', Number(data.unitPrice).toFixed(2), expectUnit.toFixed(2))
  ok('完成率落在 0~1 之间', data.orderCompletionRate >= 0 && data.orderCompletionRate <= 1, String(data.orderCompletionRate))

  /* ---------------- 2.5.2 订单总览 ---------------- */
  section('2.5.2 订单总览  GET /admin/workspace/overviewOrders')

  const dbWaiting = count('status = 2')
  const dbDelivered = count('status = 3')
  const ovBefore = todaySnapshot()
  const ov = await call('GET', '/admin/workspace/overviewOrders', { token })
  const ovAfter = todaySnapshot()
  const ovData = ov.json?.data || {}

  eq('code = 1', ov.json?.code, 1)
  ok('五个字段齐全',
    ['waitingOrders', 'deliveredOrders', 'completedOrders', 'cancelledOrders', 'allOrders'].every((k) => k in ovData),
    JSON.stringify(Object.keys(ovData)))
  // 待接单 / 待派送不限时间：与订单页那两个页签口径一致，昨天接单还没派的单也要算进来
  eq('待接单 = 库内全部待接单', ovData.waitingOrders, dbWaiting)
  eq('待派送 = 库内全部已接单未派送', ovData.deliveredOrders, dbDelivered)
  eqSnapshot('今日已完成 = 库内今日已完成', ovData.completedOrders, ovBefore.completedOrders, ovAfter.completedOrders)
  eqSnapshot('今日已取消 = 库内今日已取消', ovData.cancelledOrders, ovBefore.cancelledOrders, ovAfter.cancelledOrders)
  eqSnapshot('今日全部订单 = 库内今日全部订单', ovData.allOrders, ovBefore.allOrders, ovAfter.allOrders)
  ok('今日全部 ≥ 今日已完成 + 今日已取消', ovData.allOrders >= ovData.completedOrders + ovData.cancelledOrders,
    JSON.stringify(ovData))

  // 与订单页角标接口口径交叉验证：待接单数与 2.8.2 的 toBeConfirmed 必须一致
  const stats = await call('GET', '/admin/order/statistics', { token })
  eq('待接单数与订单页角标 2.8.2 一致', ovData.waitingOrders, stats.json?.data?.toBeConfirmed)

  /* ---------------- 2.5.3 商品总览 ---------------- */
  section('2.5.3 商品总览  GET /admin/workspace/overviewGoods')

  const goods = await call('GET', '/admin/workspace/overviewGoods', { token })
  const dbSold = Number(sql('SELECT COUNT(*) FROM goods WHERE status = 1'))
  const dbDiscontinued = Number(sql('SELECT COUNT(*) FROM goods WHERE status = 0'))
  eq('code = 1', goods.json?.code, 1)
  eq('已上架 = 库内上架商品数', goods.json?.data?.sold, dbSold)
  eq('已下架 = 库内下架商品数', goods.json?.data?.discontinued, dbDiscontinued)
  eq('两项之和 = 商品总数', goods.json?.data?.sold + goods.json?.data?.discontinued,
    Number(sql('SELECT COUNT(*) FROM goods')))

  /* ---------------- 2.5.4 组合箱总览 ---------------- */
  section('2.5.4 组合箱总览  GET /admin/workspace/overviewBundles')

  const bundles = await call('GET', '/admin/workspace/overviewBundles', { token })
  const dbBundleSold = Number(sql('SELECT COUNT(*) FROM bundle WHERE status = 1'))
  const dbBundleOff = Number(sql('SELECT COUNT(*) FROM bundle WHERE status = 0'))
  eq('code = 1', bundles.json?.code, 1)
  eq('已起售 = 库内起售组合箱数', bundles.json?.data?.sold, dbBundleSold)
  eq('已停售 = 库内停售组合箱数', bundles.json?.data?.discontinued, dbBundleOff)
  eq('两项之和 = 组合箱总数', bundles.json?.data?.sold + bundles.json?.data?.discontinued,
    Number(sql('SELECT COUNT(*) FROM bundle')))
}
