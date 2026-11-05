/**
 * 管理端订单测试（《接口文档》2.8.1 ~ 2.8.9）。
 *
 * 造数用 SQL 直接插（下单链路已由 user-order.mjs 覆盖）—— 这里要验的是状态机与条件查询。
 * 覆盖：
 *   2.8.1 条件搜索（订单号 / 手机号 / 状态 / 时间区间 / 分页）
 *   2.8.2 四个角标数字（与库内 COUNT 对齐）
 *   2.8.3 详情（带明细、不存在）
 *   2.8.4 ~ 2.8.8 状态流转，每步都验「状态不对必须拒绝且数据不变」
 *   2.8.9 超时未支付自动取消（造一条 16 分钟前的待付款单，等定时任务跑一次，最多 75 秒）
 *
 * 超时那条放在最后单独造：定时任务每分钟扫一次，早造会被它先取消，前面的断言就不准了。
 */
import { call, eq, ok, section, sql } from './lib.mjs'

const PREFIX = 'XHTEST'
const MEMBER = 9994
const PHONE = '13500009999'
const ADDRESS_ID = 1

let token

/** 一单：[id, number, status, pay_status, cancel_reason, rejection_reason, cancel_time, delivery_time] */
const orderOf = (number) =>
  sql(`SELECT id, number, status, pay_status, IFNULL(cancel_reason,'<null>'),
              IFNULL(rejection_reason,'<null>'), IFNULL(cancel_time,'<null>'), IFNULL(delivery_time,'<null>')
       FROM order_main WHERE number = '${number}'`).split('\t')

const idOf = (number) => Number(sql(`SELECT id FROM order_main WHERE number = '${number}'`))

function cleanup() {
  sql(`DELETE FROM order_item WHERE order_id IN (SELECT id FROM order_main WHERE number LIKE '${PREFIX}%')`)
  sql(`DELETE FROM order_main WHERE number LIKE '${PREFIX}%'`)
}

/** 造一单，只给状态机需要的字段；明细另行插入 */
function seed(suffix, status, payStatus, minutesAgo = 0) {
  const number = `${PREFIX}${suffix}`
  const timeExpr = minutesAgo > 0 ? `NOW() - INTERVAL ${minutesAgo} MINUTE` : 'NOW()'
  sql(`INSERT INTO order_main (number, status, member_id, address_id, order_time, checkout_time,
                               pay_method, pay_status, amount, phone, address, consignee,
                               delivery_status, delivery_fee)
       VALUES ('${number}', ${status}, ${MEMBER}, ${ADDRESS_ID}, ${timeExpr},
               ${payStatus === 1 ? timeExpr : 'NULL'}, 1, ${payStatus}, 48.80, '${PHONE}',
               '北京市朝阳区管理端测试路 1 号', '管理端收货人', 1, 5.00)`)
  return number
}

function addItem(number, name, amount = '43.80') {
  sql(`INSERT INTO order_item (name, image, order_id, goods_id, bundle_id, spec_text, number, amount)
       VALUES ('${name}', NULL, ${idOf(number)}, 1, NULL, '500g/份', 1, ${amount})`)
}

const search = (params) => call('GET', '/admin/order/conditionSearch', { token, query: params })
const detail = (id) => call('GET', `/admin/order/details/${id}`, { token })

export default async function run() {
  const login = await call('POST', '/admin/staff/login', { body: { username: 'admin', password: '123456' } })
  token = login.json?.data?.token
  ok('管理员登录拿到令牌', Boolean(token))

  cleanup()

  const tPaid = seed('PAID', 1, 0)          // 待付款
  const tConfirm = seed('CONFIRM', 2, 1)    // 待接单（已支付）→ 接单
  const tReject = seed('REJECT', 2, 0)      // 待接单（未支付）→ 拒单
  const tCancel = seed('CANCEL', 2, 1)      // 待接单（已支付）→ 取消，要退款
  const tDelivery = seed('DELIVERY', 3, 1)  // 已接单 → 派送
  const tComplete = seed('COMPLETE', 4, 1)  // 派送中 → 完成
  addItem(tConfirm, '管理端测试小油菜')
  addItem(tPaid, '管理端测试番茄')

  /* ---------------- 鉴权 ---------------- */
  section('2.8 鉴权')
  const noToken = await call('GET', '/admin/order/conditionSearch', { query: { page: 1, pageSize: 10 } })
  eq('不带令牌 → 登录状态已失效', noToken.json?.msg, '登录状态已失效')

  /* ---------------- 2.8.2 角标统计 ---------------- */
  section('2.8.2 各状态订单数量统计')

  const stats = await call('GET', '/admin/order/statistics', { token })
  eq('查询成功', stats.json?.code, 1)
  const countByStatus = (st) => Number(sql(`SELECT COUNT(*) FROM order_main WHERE status = ${st}`))
  eq('待付款数与库一致', stats.json?.data?.toBePaid, countByStatus(1))
  eq('待接单数与库一致', stats.json?.data?.toBeConfirmed, countByStatus(2))
  eq('待派送数与库一致', stats.json?.data?.confirmed, countByStatus(3))
  eq('派送中数与库一致', stats.json?.data?.deliveryInProgress, countByStatus(4))
  ok('四个字段都在（契约 2.8.2 点名 toBePaid）',
    ['toBePaid', 'toBeConfirmed', 'confirmed', 'deliveryInProgress']
      .every((k) => typeof stats.json?.data?.[k] === 'number'),
    JSON.stringify(stats.json?.data))

  /* ---------------- 2.8.1 条件搜索 ---------------- */
  section('2.8.1 条件搜索')

  const byNumber = await search({ number: PREFIX, page: 1, pageSize: 20 })
  eq('按订单号前缀搜到 6 单', byNumber.json?.data?.total, 6)
  ok('结果都带这个前缀',
    (byNumber.json?.data?.records || []).every((r) => r.number.startsWith(PREFIX)),
    JSON.stringify((byNumber.json?.data?.records || []).map((r) => r.number)))

  const byPhone = await search({ phone: PHONE, page: 1, pageSize: 20 })
  eq('按手机号搜到 6 单', byPhone.json?.data?.total, 6)

  const byStatus = await search({ number: PREFIX, status: 2, page: 1, pageSize: 20 })
  eq('叠加状态筛选：待接单 3 单', byStatus.json?.data?.total, 3)
  ok('筛选结果的 status 都是 2',
    (byStatus.json?.data?.records || []).every((r) => r.status === 2))

  const today = sql(`SELECT DATE_FORMAT(NOW(), '%Y-%m-%d')`)
  const byRange = await search({
    number: PREFIX,
    beginTime: `${today} 00:00:00`,
    endTime: `${today} 23:59:59`,
    page: 1,
    pageSize: 20
  })
  eq('时间区间（今天）搜到 6 单', byRange.json?.data?.total, 6)

  const farPast = await search({
    number: PREFIX,
    beginTime: '2000-01-01 00:00:00',
    endTime: '2000-01-02 00:00:00',
    page: 1,
    pageSize: 20
  })
  eq('时间区间（2000 年）搜到 0 单', farPast.json?.data?.total, 0)

  const paged = await search({ number: PREFIX, page: 1, pageSize: 2 })
  eq('每页 2 条时只返回 2 条', paged.json?.data?.records?.length, 2)
  eq('total 仍是 6（不是当前页条数）', paged.json?.data?.total, 6)
  ok('列表按 order_time 倒序，同一时间以 id 倒序兜底',
    (() => {
      const rs = paged.json?.data?.records || []
      return rs.length < 2 || rs[0].id > rs[1].id
    })())

  ok('列表里带明细（orderItems 数组）',
    (paged.json?.data?.records || []).every((r) => Array.isArray(r.orderItems)))

  /* ---------------- 2.8.3 详情 ---------------- */
  section('2.8.3 订单详情')

  const idConfirm = idOf(tConfirm)
  const d = await detail(idConfirm)
  eq('查询成功', d.json?.code, 1)
  eq('订单号对得上', d.json?.data?.number, tConfirm)
  eq('明细 1 条', d.json?.data?.orderItems?.length, 1)
  eq('明细名称', d.json?.data?.orderItems?.[0]?.name, '管理端测试小油菜')
  eq('带收货人快照', d.json?.data?.consignee, '管理端收货人')
  eq('带运送费', Number(d.json?.data?.deliveryFee), 5)

  const ghost = await detail(99999999)
  eq('订单不存在 → 订单不存在', ghost.json?.msg, '订单不存在')

  /* ---------------- 2.8.4 接单 ---------------- */
  section('2.8.4 接单')

  const confirmBad = await call('PUT', '/admin/order/confirm', { token, body: { id: idOf(tPaid) } })
  eq('接单待付款的单 → 订单状态错误', confirmBad.json?.msg, '订单状态错误')
  eq('被拒后状态没变', orderOf(tPaid)[2], '1')

  const confirmGhost = await call('PUT', '/admin/order/confirm', { token, body: { id: 99999999 } })
  eq('接单不存在的单 → 订单不存在', confirmGhost.json?.msg, '订单不存在')

  const confirmOk = await call('PUT', '/admin/order/confirm', { token, body: { id: idConfirm } })
  eq('待接单可以接单', confirmOk.json?.code, 1)
  eq('状态置为 3 已接单', orderOf(tConfirm)[2], '3')

  const confirmAgain = await call('PUT', '/admin/order/confirm', { token, body: { id: idConfirm } })
  eq('重复接单 → 订单状态错误', confirmAgain.json?.msg, '订单状态错误')

  /* ---------------- 2.8.5 拒单 ---------------- */
  section('2.8.5 拒单')

  // 未支付的待接单：拒单后不涉及退款
  const rejectOk = await call('PUT', '/admin/order/rejection', {
    token,
    body: { id: idOf(tReject), rejectionReason: '缺货' }
  })
  eq('待接单可以拒单', rejectOk.json?.code, 1)
  const r1 = orderOf(tReject)
  eq('状态置为 6 已取消', r1[2], '6')
  eq('拒单原因落库', r1[5], '缺货')
  ok('写入取消时间', r1[6] !== '<null>', r1[6])
  eq('未支付的单拒单后支付状态不变', r1[3], '0')

  // 已支付的待接单：拒单要退款（契约 2.8.5）
  const tReject2 = seed('REJECT2', 2, 1)
  await call('PUT', '/admin/order/rejection', {
    token,
    body: { id: idOf(tReject2), rejectionReason: '已过营业时间' }
  })
  const r2 = orderOf(tReject2)
  eq('已支付的单拒单后状态置为 6', r2[2], '6')
  eq('已支付的单拒单后支付状态置为 2 已退款', r2[3], '2')

  // 非待接单不能拒
  const rejectBad = await call('PUT', '/admin/order/rejection', {
    token,
    body: { id: idOf(tDelivery), rejectionReason: '测试' }
  })
  eq('已接单的单不能拒单 → 订单状态错误', rejectBad.json?.msg, '订单状态错误')
  eq('被拒后状态没变', orderOf(tDelivery)[2], '3')

  /* ---------------- 2.8.7 派送 / 2.8.8 完成 ---------------- */
  section('2.8.7 派送订单 / 2.8.8 完成订单')

  const deliverBad = await call('PUT', `/admin/order/delivery/${idOf(tPaid)}`, { token })
  eq('派送待付款的单 → 订单状态错误', deliverBad.json?.msg, '订单状态错误')

  const deliverOk = await call('PUT', `/admin/order/delivery/${idOf(tDelivery)}`, { token })
  eq('已接单可以派送', deliverOk.json?.code, 1)
  eq('状态置为 4 派送中', orderOf(tDelivery)[2], '4')

  // 未派送就完成 → 拒绝（用刚接完单的 tConfirm，此时它是 3）
  const completeTooEarly = await call('PUT', `/admin/order/complete/${idConfirm}`, { token })
  eq('完成已接单（未派送）的单 → 订单状态错误', completeTooEarly.json?.msg, '订单状态错误')
  eq('被拒后状态没变', orderOf(tConfirm)[2], '3')

  // 派送中 → 完成
  const completeOk = await call('PUT', `/admin/order/complete/${idOf(tDelivery)}`, { token })
  eq('派送中可以完成', completeOk.json?.code, 1)
  eq('状态置为 5 已完成', orderOf(tDelivery)[2], '5')
  ok('写入了送达时间', orderOf(tDelivery)[7] !== '<null>', orderOf(tDelivery)[7])

  const completeGhost = await call('PUT', '/admin/order/complete/99999999', { token })
  eq('完成不存在的单 → 订单不存在', completeGhost.json?.msg, '订单不存在')

  /* ---------------- 2.8.6 取消订单 ---------------- */
  section('2.8.6 取消订单')

  const idCancel = idOf(tCancel)
  const cancelOk = await call('PUT', '/admin/order/cancel', {
    token,
    body: { id: idCancel, cancelReason: '门店休息' }
  })
  eq('未完成的单可以取消', cancelOk.json?.code, 1)
  const c1 = orderOf(tCancel)
  eq('状态置为 6 已取消', c1[2], '6')
  eq('取消原因落库', c1[4], '门店休息')
  ok('写入取消时间', c1[6] !== '<null>', c1[6])
  eq('已支付的单取消后支付状态置为 2 已退款', c1[3], '2')

  const cancelAgain = await call('PUT', '/admin/order/cancel', {
    token,
    body: { id: idCancel, cancelReason: '再取消一次' }
  })
  eq('已取消的单不能再取消 → 订单状态错误', cancelAgain.json?.msg, '订单状态错误')

  const cancelDone = await call('PUT', '/admin/order/cancel', {
    token,
    body: { id: idOf(tDelivery), cancelReason: '测试已完成单' }
  })
  eq('已完成的单不能取消 → 订单状态错误', cancelDone.json?.msg, '订单状态错误')
  eq('已完成状态没被改', orderOf(tDelivery)[2], '5')

  /* ---------------- 2.8.9 超时未支付自动取消 ---------------- */
  section('2.8.9 超时未支付自动取消（等定时任务跑一次，最多 75 秒）')

  const timeoutNumber = seed('TIMEOUT', 1, 0, 16)
  const timeoutId = idOf(timeoutNumber)
  console.log('    已造一条 16 分钟前的待付款单，等下一分钟的任务扫描…')

  const deadline = Date.now() + 75000
  let row = null
  while (Date.now() < deadline) {
    row = orderOf(timeoutNumber)
    if (row[2] === '6') break
    await new Promise((r) => setTimeout(r, 3000))
  }

  eq('超时未支付已被自动取消', row[2], '6')
  eq('取消原因', row[4], '订单超时未支付，系统自动取消')
  ok('写入了取消时间', row[6] !== '<null>', row[6])
  eq('支付状态保持未支付', row[3], '0')
  ok('订单仍在（不是被删掉）', Number(orderOf(timeoutNumber)[0]) === timeoutId)

  // 未超时的待付款不能被误伤
  eq('刚下的待付款单没被误取消', orderOf(tPaid)[2], '1')

  cleanup()
  console.log('\n（已清理测试造的订单）')
}
