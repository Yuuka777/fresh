/**
 * 用户端订单测试（《接口文档-用户端》9.1 提交订单、9.2 支付）。
 *
 * 重点：
 *   1. 下单**不采信请求体**：金额、明细、收货信息都由服务端算或按地址回查 —— 前端塞假数据也不生效；
 *   2. 地址归属：别人的地址、不存在的地址一律「收货地址为空」；
 *   3. 有下架 / 停售的项整单拒绝（与购物车列表同一个口径）；
 *   4. 下单与支付分离：提交只到「待付款」，支付才推进到「待接单」并写 checkout_time；
 *   5. 支付只能打自己的订单、且只能打一次。
 * 用两个非种子会员 id（9997 / 9998）造数据，结束时清理；商品状态有改动则复原。
 */
import { call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const ME = 9997
const OTHER = 9998
const GOODS = 1 // 种子商品「有机小油菜」，含规格
const BUNDLE = 1 // 种子组合箱「一周蔬菜箱」

let me
let other
let addrId
let otherAddrId
let goodsStatusOld

const COUNT = (table, memberId) => sql(`SELECT COUNT(*) FROM ${table} WHERE member_id = ${memberId}`)

/** 某会员的订单关键字段：[id, number, status, pay_status, amount, delivery_fee, checkout_time, phone, consignee, user_name] */
const orderOf = (memberId) =>
  sql(`SELECT id, number, status, pay_status, amount, delivery_fee, IFNULL(checkout_time,'<null>'),
              IFNULL(phone,'<null>'), IFNULL(consignee,'<null>'), IFNULL(user_name,'<null>')
       FROM order_main WHERE member_id = ${memberId} ORDER BY id`).split('\t')

const itemsOf = (orderId) =>
  sql(`SELECT IFNULL(goods_id,'<null>'), IFNULL(bundle_id,'<null>'), IFNULL(spec_text,'<null>'), number, amount
       FROM order_item WHERE order_id = ${orderId} ORDER BY id`).split('\n').filter(Boolean)

/** 单条订单的关键字段：[id, number, status, pay_status, cancel_reason, cancel_time] */
const orderById = (id) =>
  sql(`SELECT id, number, status, pay_status, IFNULL(cancel_reason,'<null>'), IFNULL(cancel_time,'<null>')
       FROM order_main WHERE id = ${id}`).split('\t')

/** 购物车行：[goods_id, bundle_id, spec_text, number, amount] */
const cartOf = (memberId) =>
  sql(`SELECT IFNULL(goods_id,'<null>'), IFNULL(bundle_id,'<null>'), IFNULL(spec_text,'<null>'), number, amount
       FROM cart WHERE member_id = ${memberId} ORDER BY id`).split('\n').filter(Boolean)

const cartAdd = (body, token = me) => call('POST', '/user/cart/add', { userToken: token, body })

const submitBody = (over = {}) => ({
  addressBookId: addrId,
  estimatedDeliveryTime: '2030-01-01 12:00:00',
  deliveryStatus: 1,
  ...over
})

function cleanup() {
  sql(`DELETE FROM order_item WHERE order_id IN (SELECT id FROM order_main WHERE member_id IN (${ME}, ${OTHER}))`)
  sql(`DELETE FROM order_main WHERE member_id IN (${ME}, ${OTHER})`)
  sql(`DELETE FROM cart WHERE member_id IN (${ME}, ${OTHER})`)
  sql(`DELETE FROM address WHERE member_id IN (${ME}, ${OTHER})`)
}

export default async function run() {
  me = mintUserToken(ME)
  other = mintUserToken(OTHER)
  ok('自签两个会员的令牌', Boolean(me) && Boolean(other))

  cleanup()
  // 自愈：上一次异常退出可能把测试商品留在下架状态
  goodsStatusOld = sql(`SELECT status FROM goods WHERE id = ${GOODS}`)
  sql(`UPDATE goods SET status = 1 WHERE id = ${GOODS}`)

  try {
    /* ---------------- 鉴权 ---------------- */
    section('9.0 鉴权')

    const noToken = await call('POST', '/user/order/submit', { body: submitBody() })
    eq('不带令牌 → 登录状态已失效', noToken.json?.msg, '登录状态已失效')

    /* ---------------- 前置：各造一条地址 ---------------- */
    section('前置：两个会员各新增一条收货地址')

    const addrBody = (over = {}) => ({
      consignee: '下单收货人',
      sex: '男',
      phone: '13600001234',
      provinceName: '北京市',
      cityName: '市辖区',
      districtName: '朝阳区',
      detail: '下单测试路 1 号院 2 号楼 303',
      label: '家',
      ...over
    })

    const a1 = await call('POST', '/user/address', { userToken: me, body: addrBody() })
    eq('会员 A 新增地址成功', a1.json?.code, 1)
    addrId = Number(sql(`SELECT id FROM address WHERE member_id = ${ME} ORDER BY id LIMIT 1`))

    const a2 = await call('POST', '/user/address', { userToken: other, body: addrBody({ consignee: '别人' }) })
    eq('会员 B 新增地址成功', a2.json?.code, 1)
    otherAddrId = Number(sql(`SELECT id FROM address WHERE member_id = ${OTHER} ORDER BY id LIMIT 1`))

    /* ---------------- 9.1 失败场景 ---------------- */
    section('9.1 提交订单的失败场景')

    const emptyCart = await call('POST', '/user/order/submit', { userToken: me, body: submitBody() })
    eq('购物车为空 → 购物车为空', emptyCart.json?.msg, '购物车为空')

    await cartAdd({ goodsId: GOODS })

    const noAddr = await call('POST', '/user/order/submit', { userToken: me, body: submitBody({ addressBookId: null }) })
    eq('不传地址 → 收货地址为空', noAddr.json?.msg, '收货地址为空')

    const ghostAddr = await call('POST', '/user/order/submit', {
      userToken: me,
      body: submitBody({ addressBookId: 99999999 })
    })
    eq('地址不存在 → 收货地址为空', ghostAddr.json?.msg, '收货地址为空')

    const stealAddr = await call('POST', '/user/order/submit', {
      userToken: me,
      body: submitBody({ addressBookId: otherAddrId })
    })
    eq('用别人的地址下单 → 收货地址为空', stealAddr.json?.msg, '收货地址为空')

    sql(`UPDATE goods SET status = 0 WHERE id = ${GOODS}`)
    const offShelf = await call('POST', '/user/order/submit', { userToken: me, body: submitBody() })
    eq('车里有下架的项 → 购物车中有商品已下架，请先处理',
      offShelf.json?.msg, '购物车中有商品已下架，请先处理')
    eq('以上失败都没有建单', COUNT('order_main', ME), '0')
    sql(`UPDATE goods SET status = 1 WHERE id = ${GOODS}`)

    /* ---------------- 9.1 成功：金额与明细都由服务端算 ---------------- */
    section('9.1 提交订单（不采信请求体）')

    await cartAdd({ bundleId: BUNDLE })
    eq('车里两行（商品 + 组合箱）', COUNT('cart', ME), '2')

    const goodsPrice = sql(`SELECT price FROM goods WHERE id = ${GOODS}`)
    const bundlePrice = sql(`SELECT price FROM bundle WHERE id = ${BUNDLE}`)
    const expectGoods = (Number(goodsPrice) + Number(bundlePrice)).toFixed(2)
    const expectTotal = (Number(expectGoods) + 5).toFixed(2)

    // 前端确实会带上这些字段（confirm.vue 就是这么提交的），但服务端一个都不采信
    const submitted = await call('POST', '/user/order/submit', {
      userToken: me,
      body: submitBody({
        amount: 0.01,
        deliveryFee: 0.01,
        phone: '13800000000',
        address: '假地址',
        consignee: '假收货人',
        userName: '假昵称',
        orderItems: [{ goodsId: GOODS, name: '假商品', number: 99, amount: 0.01 }]
      })
    })
    eq('下单成功 code = 1', submitted.json?.code, 1)
    ok('返回订单号', Boolean(submitted.json?.data?.orderNumber), String(submitted.json?.data?.orderNumber))
    eq('返回的订单总金额 = 明细合计 + 运送费', Number(submitted.json?.data?.orderAmount), Number(expectTotal))
    ok('返回下单时间', Boolean(submitted.json?.data?.orderTime), String(submitted.json?.data?.orderTime))

    const order = orderOf(ME)
    const orderId = Number(order[0])
    eq('库中只建了 1 单', COUNT('order_main', ME), '1')
    eq('初始状态 = 1 待付款', order[2], '1')
    eq('支付状态 = 0 未支付', order[3], '0')
    eq('checkout_time 为空（尚未支付）', order[6], '<null>')
    eq('实收金额按服务端算，不是请求体的 0.01', Number(order[4]), Number(expectTotal))
    eq('运送费落的是服务端的 5.00，不是请求体的 0.01', Number(order[5]), 5)
    eq('收货手机号取自地址，不是请求体的 13800000000', order[7], '13600001234')
    eq('收货人取自地址，不是请求体的「假收货人」', order[8], '下单收货人')
    /* 自签令牌的测试会员在 member 表里没有记录（真实会员由微信登录自动注册），
       所以这里断言「跟随会员表」而不是硬编码一个昵称 */
    const memberName = sql(`SELECT name FROM member WHERE id = ${ME}`) || '<null>'
    eq('会员昵称取自会员表，不是请求体的「假昵称」', order[9], memberName)

    const items = itemsOf(orderId)
    eq('明细 2 条（来自购物车，不是请求体的 1 条假明细）', items.length, 2)
    const itemText = items.join(' | ')
    ok('明细里没有「假商品」', !itemText.includes('假商品'), itemText)
    eq('明细金额 = 现值单价 × 数量',
      items.map((r) => Number(r.split('\t')[4])).reduce((a, b) => a + b, 0).toFixed(2),
      expectGoods)

    eq('下单后购物车已清空', COUNT('cart', ME), '0')

    /* ---------------- 9.2 支付 ---------------- */
    section('9.2 订单支付')

    const noTokenPay = await call('PUT', '/user/order/payment', { body: { orderNumber: order[1], payMethod: 1 } })
    eq('不带令牌 → 登录状态已失效', noTokenPay.json?.msg, '登录状态已失效')

    const ghostPay = await call('PUT', '/user/order/payment', {
      userToken: me,
      body: { orderNumber: '不存在的订单号', payMethod: 1 }
    })
    eq('订单号不存在 → 订单不存在', ghostPay.json?.msg, '订单不存在')

    // 会员 B 自己下一单，会员 A 拿它的订单号去支付
    await cartAdd({ goodsId: GOODS }, other)
    await call('POST', '/user/order/submit', { userToken: other, body: { addressBookId: otherAddrId } })
    const otherOrderNumber = orderOf(OTHER)[1]

    const stealPay = await call('PUT', '/user/order/payment', {
      userToken: me,
      body: { orderNumber: otherOrderNumber, payMethod: 1 }
    })
    eq('支付别人的订单 → 订单不存在', stealPay.json?.msg, '订单不存在')
    eq('别人的订单状态没被动', orderOf(OTHER)[2], '1')

    const paid = await call('PUT', '/user/order/payment', {
      userToken: me,
      body: { orderNumber: order[1], payMethod: 1 }
    })
    eq('支付成功 code = 1', paid.json?.code, 1)
    const afterPay = orderOf(ME)
    eq('状态推进为 2 待接单', afterPay[2], '2')
    eq('支付状态变为 1 已支付', afterPay[3], '1')
    ok('写入了 checkout_time', afterPay[6] !== '<null>', afterPay[6])

    const payAgain = await call('PUT', '/user/order/payment', {
      userToken: me,
      body: { orderNumber: order[1], payMethod: 1 }
    })
    eq('重复支付 → 订单状态错误', payAgain.json?.msg, '订单状态错误')
    eq('状态没有被重复推进（仍为 2）', orderOf(ME)[2], '2')

    /* ---------------- 下单与支付分离 ---------------- */
    section('9.1 ↔ 9.2 的分工')

    // 会员 B 那一单还没支付：它应该停在待付款
    eq('未支付的订单停在待付款', orderOf(OTHER)[2], '1')
    eq('未支付的订单 pay_status 仍为 0', orderOf(OTHER)[3], '0')

    eq('再下单（车已清空）→ 购物车为空',
      (await call('POST', '/user/order/submit', { userToken: me, body: submitBody() })).json?.msg,
      '购物车为空')

    /* ---------------- 9.3 历史订单分页查询 ---------------- */
    section('9.3 历史订单分页查询')

    // 给会员 A 再造一单（未支付），两个状态都有才好验筛选与排序
    await cartAdd({ bundleId: BUNDLE })
    await call('POST', '/user/order/submit', { userToken: me, body: submitBody() })

    const mine = sql(`SELECT id, number, status FROM order_main WHERE member_id = ${ME}
                      ORDER BY order_time DESC, id DESC`).split('\n').filter(Boolean).map((l) => l.split('\t'))
    eq('会员 A 有 2 单', mine.length, 2)
    const unpaidId = Number(mine[0][0])
    const paidId = Number(mine[1][0])
    eq('较新那一单是待付款', mine[0][2], '1')
    eq('较早那一单是待接单', mine[1][2], '2')

    const otherOrderId = Number(orderOf(OTHER)[0])

    const noTokenHistory = await call('GET', '/user/order/historyOrders', { query: { page: 1, pageSize: 10 } })
    eq('不带令牌 → 登录状态已失效', noTokenHistory.json?.msg, '登录状态已失效')

    const all = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 1, pageSize: 10 } })
    eq('查询成功', all.json?.code, 1)
    eq('只返回自己的 2 单', all.json?.data?.total, 2)

    const rec0 = all.json?.data?.records?.[0]
    const rec1 = all.json?.data?.records?.[1]
    eq('按下单时间倒序，最新的一单在第一条', rec0?.number, mine[0][1])
    eq('第二条是较早那一单', rec1?.number, mine[1][1])
    eq('列表带明细（组合箱那单 1 条）', rec0?.orderItems?.length, 1)
    ok('明细名称不为空', Boolean(rec0?.orderItems?.[0]?.name), String(rec0?.orderItems?.[0]?.name))
    eq('明细数量为 1', rec0?.orderItems?.[0]?.number, 1)
    eq('明细金额 = 单价 × 数量', Number(rec0?.orderItems?.[0]?.amount), Number(bundlePrice))
    eq('带收货人快照', rec0?.consignee, '下单收货人')
    eq('带运送费', Number(rec0?.deliveryFee), 5)

    const byStatus1 = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 1, pageSize: 10, status: 1 } })
    eq('按待付款筛选 → 1 条', byStatus1.json?.data?.total, 1)
    eq('筛选出的订单号对得上', byStatus1.json?.data?.records?.[0]?.number, mine[0][1])

    const byStatus2 = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 1, pageSize: 10, status: 2 } })
    eq('按待接单筛选 → 1 条', byStatus2.json?.data?.total, 1)

    const byStatus5 = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 1, pageSize: 10, status: 5 } })
    eq('按已完成筛选 → 0 条', byStatus5.json?.data?.total, 0)

    const paged = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 1, pageSize: 1 } })
    eq('每页 1 条时只返回 1 条', paged.json?.data?.records?.length, 1)
    eq('total 仍是 2（不是当前页条数）', paged.json?.data?.total, 2)
    const paged2 = await call('GET', '/user/order/historyOrders', { userToken: me, query: { page: 2, pageSize: 1 } })
    eq('第二页拿到另一单', paged2.json?.data?.records?.[0]?.number, mine[1][1])

    const otherList = await call('GET', '/user/order/historyOrders', { userToken: other, query: { page: 1, pageSize: 10 } })
    eq('会员 B 只看到自己的 1 单', otherList.json?.data?.total, 1)
    ok('会员 B 看不到会员 A 的订单',
      (otherList.json?.data?.records || []).every((r) => r.number !== mine[0][1] && r.number !== mine[1][1]))

    /* ---------------- 9.4 订单详情 ---------------- */
    section('9.4 订单详情')

    const noTokenDetail = await call('GET', `/user/order/orderDetail/${paidId}`)
    eq('不带令牌 → 登录状态已失效', noTokenDetail.json?.msg, '登录状态已失效')

    const detail = await call('GET', `/user/order/orderDetail/${paidId}`, { userToken: me })
    eq('查询成功', detail.json?.code, 1)
    eq('订单号对得上', detail.json?.data?.number, mine[1][1])
    eq('状态对得上', detail.json?.data?.status, 2)
    eq('明细 2 条', detail.json?.data?.orderItems?.length, 2)
    ok('带完整收货地址快照', Boolean(detail.json?.data?.address), String(detail.json?.data?.address))
    eq('未填的备注为 null', detail.json?.data?.remark, null)

    const ghostDetail = await call('GET', '/user/order/orderDetail/99999999', { userToken: me })
    eq('订单不存在 → 订单不存在', ghostDetail.json?.msg, '订单不存在')

    const stealDetail = await call('GET', `/user/order/orderDetail/${otherOrderId}`, { userToken: me })
    eq('查别人的订单 → 订单不存在', stealDetail.json?.msg, '订单不存在')

    /* ---------------- 9.5 用户取消订单 ---------------- */
    section('9.5 用户取消订单')

    const noTokenCancel = await call('PUT', `/user/order/cancel/${unpaidId}`)
    eq('不带令牌 → 登录状态已失效', noTokenCancel.json?.msg, '登录状态已失效')

    const stealCancel = await call('PUT', `/user/order/cancel/${otherOrderId}`, { userToken: me })
    eq('取消别人的订单 → 订单不存在', stealCancel.json?.msg, '订单不存在')
    eq('别人的订单没被动', orderById(otherOrderId)[2], '1')

    const ghostCancel = await call('PUT', '/user/order/cancel/99999999', { userToken: me })
    eq('取消不存在的订单 → 订单不存在', ghostCancel.json?.msg, '订单不存在')

    // 未支付的单：状态转 6，支付状态本来就没变过
    const cancelUnpaid = await call('PUT', `/user/order/cancel/${unpaidId}`, { userToken: me })
    eq('未支付的单可以取消', cancelUnpaid.json?.code, 1)
    const afterCancel = orderById(unpaidId)
    eq('状态置为 6 已取消', afterCancel[2], '6')
    eq('取消原因落库', afterCancel[4], '用户取消')
    ok('写入取消时间', afterCancel[5] !== '<null>', afterCancel[5])
    eq('未支付的单取消后支付状态仍是 0', afterCancel[3], '0')

    const cancelAgain = await call('PUT', `/user/order/cancel/${unpaidId}`, { userToken: me })
    eq('已取消的单再取消 → 订单状态错误', cancelAgain.json?.msg, '订单状态错误')

    // 已支付的单（待接单）：取消要退款
    await cartAdd({ goodsId: GOODS }, other)
    await call('POST', '/user/order/submit', { userToken: other, body: { addressBookId: otherAddrId } })
    const bOrderId = Number(sql(`SELECT id FROM order_main WHERE member_id = ${OTHER} ORDER BY id DESC LIMIT 1`))
    await call('PUT', '/user/order/payment', {
      userToken: other,
      body: { orderNumber: orderById(bOrderId)[1], payMethod: 1 }
    })
    eq('会员 B 这一单已支付', orderById(bOrderId)[3], '1')

    const cancelPaid = await call('PUT', `/user/order/cancel/${bOrderId}`, { userToken: other })
    eq('待接单（已支付）的单可以取消', cancelPaid.json?.code, 1)
    eq('状态置为 6', orderById(bOrderId)[2], '6')
    eq('已支付的单取消后支付状态变为 2 已退款', orderById(bOrderId)[3], '2')

    // 已进入履约的订单不能由用户取消
    for (const st of [3, 4, 5]) {
      sql(`UPDATE order_main SET status = ${st} WHERE id = ${bOrderId}`)
      const rejected = await call('PUT', `/user/order/cancel/${bOrderId}`, { userToken: other })
      eq(`状态 ${st} 时取消 → 订单状态错误`, rejected.json?.msg, '订单状态错误')
      eq(`状态 ${st} 没被改动`, orderById(bOrderId)[2], String(st))
    }
    sql(`UPDATE order_main SET status = 6 WHERE id = ${bOrderId}`)

    /* ---------------- 9.6 再来一单 ---------------- */
    section('9.6 再来一单')

    const noTokenRepeat = await call('POST', `/user/order/repetition/${paidId}`)
    eq('不带令牌 → 登录状态已失效', noTokenRepeat.json?.msg, '登录状态已失效')

    sql(`DELETE FROM cart WHERE member_id = ${ME}`)
    const stealRepeat = await call('POST', `/user/order/repetition/${bOrderId}`, { userToken: me })
    eq('复购别人的订单 → 订单不存在', stealRepeat.json?.msg, '订单不存在')
    eq('复购别人的订单没写进自己的车', COUNT('cart', ME), '0')

    const ghostRepeat = await call('POST', '/user/order/repetition/99999999', { userToken: me })
    eq('复购不存在的订单 → 订单不存在', ghostRepeat.json?.msg, '订单不存在')

    const goodsPriceNow = Number(sql(`SELECT price FROM goods WHERE id = ${GOODS}`))
    const bundlePriceNow = Number(sql(`SELECT price FROM bundle WHERE id = ${BUNDLE}`))

    const repeat = await call('POST', `/user/order/repetition/${paidId}`, { userToken: me })
    eq('再来一单成功', repeat.json?.code, 1)

    let cart = cartOf(ME)
    eq('明细逐条写回购物车（2 行）', cart.length, 2)
    const goodsLine = cart.find((r) => r.split('\t')[0] !== '<null>')
    const bundleLine = cart.find((r) => r.split('\t')[1] !== '<null>')
    eq('商品行数量与订单一致', goodsLine?.split('\t')[3], '1')
    eq('商品行按现值写快照', Number(goodsLine?.split('\t')[4]).toFixed(2), goodsPriceNow.toFixed(2))
    eq('组合箱行按现值写快照', Number(bundleLine?.split('\t')[4]).toFixed(2), bundlePriceNow.toFixed(2))

    // 同款累加：再点一次不该多出行
    await call('POST', `/user/order/repetition/${paidId}`, { userToken: me })
    cart = cartOf(ME)
    eq('再点一次仍是 2 行（同款累加而不是新增行）', cart.length, 2)
    const goods2 = cart.find((r) => r.split('\t')[0] !== '<null>')
    eq('商品数量翻倍', goods2?.split('\t')[3], '2')
    eq('小计跟着翻倍', Number(goods2?.split('\t')[4]).toFixed(2), (goodsPriceNow * 2).toFixed(2))

    // 商品下架后复购：仍写回，购物车会把它标成不可结算，用户看得见
    sql(`UPDATE goods SET status = 0 WHERE id = ${GOODS}`)
    sql(`DELETE FROM cart WHERE member_id = ${ME}`)
    const repeatOffShelf = await call('POST', `/user/order/repetition/${paidId}`, { userToken: me })
    eq('商品下架后仍可复购写回', repeatOffShelf.json?.code, 1)
    const cartAfterRepeat = await call('GET', '/user/cart/list', { userToken: me })
    const offLine = (cartAfterRepeat.json?.data || []).find((l) => l.goodsId === GOODS)
    eq('写回的那行被标成不可结算 status = 0', offLine?.status, 0)
    sql(`UPDATE goods SET status = 1 WHERE id = ${GOODS}`)
    sql(`DELETE FROM cart WHERE member_id = ${ME}`)

    /* ---------------- 9.7 客户催单 ---------------- */
    section('9.7 客户催单')

    const noTokenRemind = await call('GET', `/user/order/reminder/${paidId}`)
    eq('不带令牌 → 登录状态已失效', noTokenRemind.json?.msg, '登录状态已失效')

    const stealRemind = await call('GET', `/user/order/reminder/${bOrderId}`, { userToken: me })
    eq('催别人的订单 → 订单不存在', stealRemind.json?.msg, '订单不存在')

    // 可催的三个状态：待接单（催接单）、已接单（催出餐）、派送中（催配送）
    for (const st of [2, 3, 4]) {
      sql(`UPDATE order_main SET status = ${st} WHERE id = ${paidId}`)
      const r = await call('GET', `/user/order/reminder/${paidId}`, { userToken: me })
      eq(`状态 ${st} 时可以催单`, r.json?.code, 1)
    }

    // 待付款还没有履约对象、已完成 / 已取消没必要催
    for (const st of [1, 5, 6]) {
      sql(`UPDATE order_main SET status = ${st} WHERE id = ${paidId}`)
      const r = await call('GET', `/user/order/reminder/${paidId}`, { userToken: me })
      eq(`状态 ${st} 时催单 → 订单状态错误`, r.json?.msg, '订单状态错误')
    }
    sql(`UPDATE order_main SET status = 2 WHERE id = ${paidId}`)
  } finally {
    sql(`UPDATE goods SET status = ${goodsStatusOld} WHERE id = ${GOODS}`)
    cleanup()
    console.log('\n（已清理测试会员的订单 / 购物车 / 地址，并复原商品状态）')
  }
}
