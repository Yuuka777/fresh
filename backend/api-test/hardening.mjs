/**
 * 本轮加固项的针对性测试（只覆盖改动点，不做全量回归）
 *
 * A. 订单状态并发：同一单并发支付 / 并发接单 / 支付与取消并发
 * B. 超时任务：批量条件更新，且不误伤未超时单
 * C. 角标统计：一次 GROUP BY 的结果与库内 COUNT 一致
 * D. 上传：文件头校验（真 PNG 放行、文本伪装拒绝）
 * E. WebSocket：无令牌 / 错误令牌被拒，有效令牌可连
 * F. 口令：新账号 PBKDF2、旧 MD5 登录后自动升级、改密与错误密码
 * G. 联合索引：EXPLAIN 命中 idx_status_time / idx_member_time 且无 filesort
 */
import { BASE, call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const PREFIX = 'XHHARD'
const MEMBER = 9995
const PHONE = '13500007777'

let token

/** 一单：[id, status, pay_status, cancel_reason, cancel_time] */
const rowOf = (number) => {
  const r = sql(`SELECT id, status, pay_status, IFNULL(cancel_reason,''), IFNULL(cancel_time,'')
                 FROM order_main WHERE number = '${number}'`).split('\t')
  return {
    id: Number(r[0]),
    status: Number(r[1]),
    payStatus: Number(r[2]),
    cancelReason: r[3],
    cancelTime: r[4]
  }
}

function cleanup() {
  sql(`DELETE FROM order_item WHERE order_id IN (SELECT id FROM order_main WHERE number LIKE '${PREFIX}%')`)
  sql(`DELETE FROM order_main WHERE number LIKE '${PREFIX}%'`)
  sql(`DELETE FROM staff WHERE username LIKE 'hard_%'`)
}

function seed(suffix, status, payStatus, minutesAgo = 0) {
  const number = `${PREFIX}${suffix}`
  const timeExpr = minutesAgo > 0 ? `NOW() - INTERVAL ${minutesAgo} MINUTE` : 'NOW()'
  sql(`INSERT INTO order_main (number, status, member_id, address_id, order_time, checkout_time,
                               pay_method, pay_status, amount, phone, address, consignee,
                               delivery_status, delivery_fee)
       VALUES ('${number}', ${status}, ${MEMBER}, 1, ${timeExpr},
               ${payStatus === 1 ? timeExpr : 'NULL'}, 1, ${payStatus}, 48.80, '${PHONE}',
               '北京市朝阳区加固测试路 1 号', '加固测试', 1, 5.00)`)
  return number
}

export default async function run() {
  const login = await call('POST', '/admin/staff/login', { body: { username: 'admin', password: '123456' } })
  token = login.json?.data?.token
  ok('管理员登录', Boolean(token))

  cleanup()

  /* ---------------- A. 订单状态并发 ---------------- */
  section('A. 订单状态并发（条件更新）')

  const nPay = seed('PAY', 1, 0)
  const userToken = mintUserToken(MEMBER)
  const paidTwice = await Promise.all([
    call('PUT', '/user/order/payment', { userToken, body: { orderNumber: nPay, payMethod: 1 } }),
    call('PUT', '/user/order/payment', { userToken, body: { orderNumber: nPay, payMethod: 1 } })
  ])
  eq('同一单并发支付：成功数 = 1', paidTwice.filter((r) => r.json?.code === 1).length, 1)
  eq('另一个请求返回订单状态错误', paidTwice.filter((r) => r.json?.msg === '订单状态错误').length, 1)
  const paid = rowOf(nPay)
  eq('最终状态为待接单', paid.status, 2)
  eq('最终支付状态为已支付', paid.payStatus, 1)

  // 支付与「管理端取消」同时发生：最容易产出「已取消 + 已付款未退款」的脏单
  const nRace = seed('RACE', 1, 0)
  const raceId = rowOf(nRace).id
  await Promise.all([
    call('PUT', '/user/order/payment', { userToken: mintUserToken(MEMBER), body: { orderNumber: nRace, payMethod: 1 } }),
    call('PUT', '/admin/order/cancel', { token, body: { id: raceId, cancelReason: '并发测试取消' } })
  ])
  const race = rowOf(nRace)
  ok('并发后状态只可能是待接单或已取消', [2, 6].includes(race.status), `status=${race.status}`)
  ok('不出现「已取消 + 已付款未退款」', !(race.status === 6 && race.payStatus === 1),
    `status=${race.status} pay_status=${race.payStatus}`)

  const nConfirm = seed('CONFIRM', 2, 1)
  const confirmId = rowOf(nConfirm).id
  const confirmedTwice = await Promise.all([
    call('PUT', '/admin/order/confirm', { token, body: { id: confirmId } }),
    call('PUT', '/admin/order/confirm', { token, body: { id: confirmId } })
  ])
  eq('同一单并发接单：成功数 = 1', confirmedTwice.filter((r) => r.json?.code === 1).length, 1)
  eq('最终状态为已接单', rowOf(nConfirm).status, 3)

  const nIllegal = seed('ILLEGAL', 1, 0)
  const illegal = await call('PUT', '/admin/order/confirm', { token, body: { id: rowOf(nIllegal).id } })
  eq('待付款单接单仍被拒', illegal.json?.msg, '订单状态错误')

  // 条件更新不能挡住正常链路
  const nFlow = seed('FLOW', 2, 1)
  const flowId = rowOf(nFlow).id
  eq('正常接单', (await call('PUT', '/admin/order/confirm', { token, body: { id: flowId } })).json?.code, 1)
  eq('正常派送', (await call('PUT', `/admin/order/delivery/${flowId}`, { token })).json?.code, 1)
  eq('正常完成', (await call('PUT', `/admin/order/complete/${flowId}`, { token })).json?.code, 1)
  eq('最终状态为已完成', rowOf(nFlow).status, 5)

  /* ---------------- B. 超时任务 ---------------- */
  section('B. 超时任务（批量条件更新）')

  const nExpired = seed('EXPIRED', 1, 0, 16)
  const nFresh = seed('FRESH', 1, 0, 1)
  const wait = 62000 - (Date.now() % 60000)
  console.log(`  …等待定时任务执行一次（${Math.ceil(wait / 1000)} 秒）`)
  await new Promise((r) => setTimeout(r, wait))

  const expired = rowOf(nExpired)
  eq('超时单被置为已取消', expired.status, 6)
  eq('取消原因正确', expired.cancelReason, '订单超时未支付，系统自动取消')
  ok('写入取消时间', expired.cancelTime !== '', expired.cancelTime)
  eq('未超时的待付款单不受影响', rowOf(nFresh).status, 1)

  /* ---------------- C. 角标统计 ---------------- */
  section('C. 角标统计（一次 GROUP BY）')

  const stats = await call('GET', '/admin/order/statistics', { token })
  const dbCount = (status) => Number(sql(`SELECT COUNT(*) FROM order_main WHERE status = ${status}`))
  eq('待付款数与库一致', stats.json?.data?.toBePaid, dbCount(1))
  eq('待接单数与库一致', stats.json?.data?.toBeConfirmed, dbCount(2))
  eq('待派送数与库一致', stats.json?.data?.confirmed, dbCount(3))
  eq('派送中数与库一致', stats.json?.data?.deliveryInProgress, dbCount(4))

  /* ---------------- D. 上传文件头校验 ---------------- */
  section('D. 上传：文件头校验')

  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64'
  )
  const goodForm = new FormData()
  goodForm.append('file', new Blob([png], { type: 'image/png' }), 'pixel.png')
  const uploadOk = await fetch(`${BASE}/admin/common/upload`, {
    method: 'POST',
    headers: { token },
    body: goodForm
  }).then((r) => r.json())
  ok('真 PNG 放行', uploadOk.code === 1, JSON.stringify(uploadOk).slice(0, 100))
  ok('返回 OSS 地址', String(uploadOk.data || '').includes('aliyuncs.com'), uploadOk.data)

  const fake = Buffer.from('<?php echo 1; ?>' + 'A'.repeat(64))
  const badForm = new FormData()
  badForm.append('file', new Blob([fake], { type: 'image/png' }), 'evil.png')
  const uploadBad = await fetch(`${BASE}/admin/common/upload`, {
    method: 'POST',
    headers: { token },
    body: badForm
  }).then((r) => r.json())
  eq('伪装成 PNG 的文本被拒', uploadBad.msg, '文件上传失败')

  /* ---------------- E. WebSocket 鉴权 ---------------- */
  section('E. WebSocket 鉴权')

  const wsBase = BASE.replace(/^http/, 'ws')
  const waitClose = (ws) =>
    new Promise((resolve) => {
      const timer = setTimeout(() => resolve('timeout'), 6000)
      ws.onclose = (e) => {
        clearTimeout(timer)
        resolve(e.code)
      }
      ws.onerror = () => {}
    })

  const noToken = new WebSocket(`${wsBase}/ws/hard-no-token`)
  const codeNoToken = await waitClose(noToken)
  eq('不带令牌被服务端关闭（1008）', codeNoToken, 1008)

  const badToken = new WebSocket(`${wsBase}/ws/hard-bad-token?token=abc.def.ghi`)
  const codeBadToken = await waitClose(badToken)
  eq('错误令牌被服务端关闭（1008）', codeBadToken, 1008)

  const goodWs = new WebSocket(`${wsBase}/ws/hard-good-token?token=${encodeURIComponent(token)}`)
  const stayed = await new Promise((resolve) => {
    const timer = setTimeout(() => resolve(false), 6000)
    goodWs.onopen = () => {
      // 服务端验签失败会在握手成功后立刻关闭，只等 onopen 会得到假绿：再等 1.5 秒确认连接仍开着
      setTimeout(() => resolve(goodWs.readyState === 1), 1500)
    }
    goodWs.onclose = () => {
      clearTimeout(timer)
      resolve(false)
    }
    goodWs.onerror = () => {}
  })
  ok('有效令牌可以连接并保持', stayed === true)
  goodWs.close()

  /* ---------------- F. 口令哈希 ---------------- */
  section('F. 口令哈希（PBKDF2）')

  const adminStored = sql(`SELECT password FROM staff WHERE username = 'admin'`)
  if (adminStored.startsWith('pbkdf2$')) {
    ok('admin 已是 PBKDF2（此前已完成升级）', true)
  } else {
    const oldLogin = await call('POST', '/admin/staff/login', {
      body: { username: 'admin', password: '123456' }
    })
    eq('旧 MD5 账号仍可登录', oldLogin.json?.code, 1)
    const upgraded = sql(`SELECT password FROM staff WHERE username = 'admin'`)
    ok('登录后自动升级为 PBKDF2', upgraded.startsWith('pbkdf2$'), upgraded.slice(0, 18))
  }

  const uname = `hard_${Date.now().toString().slice(-8)}`
  const added = await call('POST', '/admin/staff', {
    token,
    body: {
      name: '加固测试',
      username: uname,
      phone: '13500006666',
      sex: '男',
      idNumber: '110101199001011234'
    }
  })
  eq('新增店员成功', added.json?.code, 1)
  const staffId = Number(sql(`SELECT id FROM staff WHERE username = '${uname}'`))
  const newStored = sql(`SELECT password FROM staff WHERE id = ${staffId}`)
  ok('新账号口令是 PBKDF2 格式', newStored.startsWith('pbkdf2$'), newStored.slice(0, 18))

  await call('POST', '/admin/staff/status/1', { token, query: { id: staffId } })
  eq('初始密码可登录', (await call('POST', '/admin/staff/login', {
    body: { username: uname, password: '123456' }
  })).json?.code, 1)

  eq('错误密码被拒', (await call('POST', '/admin/staff/login', {
    body: { username: uname, password: '1234567' }
  })).json?.msg, '密码错误')

  eq('改密成功', (await call('PUT', '/admin/staff/editPassword', {
    token,
    body: { empId: staffId, oldPassword: '123456', newPassword: 'Abc123456' }
  })).json?.code, 1)

  eq('新密码可登录', (await call('POST', '/admin/staff/login', {
    body: { username: uname, password: 'Abc123456' }
  })).json?.code, 1)

  eq('旧密码已失效', (await call('POST', '/admin/staff/login', {
    body: { username: uname, password: '123456' }
  })).json?.msg, '密码错误')

  /* ---------------- G. 联合索引 ---------------- */
  section('G. 联合索引生效')

  const planStatus = sql(
    'EXPLAIN SELECT id FROM order_main WHERE status = 2 ORDER BY order_time DESC, id DESC LIMIT 10'
  ).split('\t')
  eq('状态 + 排序查询命中 idx_status_time', planStatus[6], 'idx_status_time')
  ok('不再额外 filesort', !String(planStatus[11] || '').includes('filesort'), planStatus[11])

  const planMember = sql(
    `EXPLAIN SELECT id FROM order_main WHERE member_id = ${MEMBER} ORDER BY order_time DESC, id DESC LIMIT 10`
  ).split('\t')
  eq('会员订单命中 idx_member_time', planMember[6], 'idx_member_time')
  ok('不再额外 filesort', !String(planMember[11] || '').includes('filesort'), planMember[11])

  cleanup()
}
