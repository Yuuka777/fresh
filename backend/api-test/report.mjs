/**
 * 报表模块接口测试（《接口文档》2.9，共 4 个接口）。
 *
 * 统计口径一律「用同一条 SQL 从库里算出期望值再比对」，不写死数字。
 * 几个刻意设计的覆盖点：
 *   · 序列长度必须等于区间天数 —— 区间里没有订单的日期要补 0，否则前端两条曲线对不齐；
 *   · 用 90 天长区间强制出现「无订单日」，把补 0 分支真正跑到；
 *   · 累计会员数抽查若干天，直接与库内「截至当日 24 时」的会员数比对；
 *   · TOP10 与库内聚合结果逐条比对（含顺序与条数上限）。
 *
 * 缺省区间是最近 30 天（含今天）——与报表页「统计区间」卡片上的说明一致。
 */
import { call, eq, ok, section, sql } from './lib.mjs'

const pad = (n) => String(n).padStart(2, '0')
const day = (offset) => {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const TODAY = day(0)
const BEGIN30 = day(-29)
const nextDay = (dateStr) => {
  const [y, m, d] = dateStr.split('-').map(Number)
  const next = new Date(y, m - 1, d + 1)
  return `${next.getFullYear()}-${pad(next.getMonth() + 1)}-${pad(next.getDate())}`
}
/** 逗号序列 → 数组；空串得到空数组 */
const seriesOf = (s) => String(s ?? '').split(',').filter((x) => x !== '')
/** 金额序列求和（按分相加，避免浮点误差） */
const centsOf = (list) => list.reduce((sum, v) => sum + Math.round(Number(v) * 100), 0)
/** 把统计序列格式化成「日期=值」的紧凑串：整串比对，失败时也能看出是哪一天差 */
const zip = (dates, values) => dates.map((d, i) => `${d}:${values[i]}`).join(' ')
const money = (list) => list.map((v) => Number(v).toFixed(2))

/** 库里按天聚合出「日期 → 值」的映射 */
function dailyFromDb({ column, table, where, begin, end, dateColumn }) {
  const rows = sql(
    `SELECT DATE(${dateColumn}) AS d, ${column} AS v FROM ${table}
     WHERE ${where} AND ${dateColumn} >= '${begin} 00:00:00' AND ${dateColumn} < '${nextDay(end)} 00:00:00'
     GROUP BY DATE(${dateColumn}) ORDER BY d`
  )
  const map = new Map()
  if (rows) {
    for (const line of rows.split('\n')) {
      const [d, v] = line.split('\t')
      map.set(d, v)
    }
  }
  return map
}

export default async function run() {
  const login = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  const token = login.json?.data?.token

  /* ---------------- 2.9.1 营业额统计 ---------------- */
  section('2.9.1 营业额统计  GET /admin/report/turnoverStatistics')

  const def = await call('GET', '/admin/report/turnoverStatistics', { token })
  eq('不传区间 → code = 1', def.json?.code, 1)
  const defDates = seriesOf(def.json?.data?.dateList)
  const defTurnover = seriesOf(def.json?.data?.turnoverList)
  eq('缺省区间 = 最近 30 天（含今天）', defDates.length, 30)
  eq('缺省区间首位 = 29 天前', defDates[0], BEGIN30)
  eq('缺省区间末位 = 今天', defDates.at(-1), TODAY)
  eq('两个序列等长', defTurnover.length, defDates.length)

  const dbAmount = dailyFromDb({
    column: 'IFNULL(SUM(amount), 0)',
    table: 'order_main',
    where: 'status = 5',
    begin: BEGIN30,
    end: TODAY,
    dateColumn: 'order_time'
  })
  eq(
    '缺省区间营业额序列与库一致（缺订单的日期补 0）',
    zip(defDates, money(defTurnover)),
    zip(defDates, defDates.map((d) => Number(dbAmount.get(d) || 0).toFixed(2)))
  )
  // 再抽查三天，失败时一眼能看出差在哪
  for (const d of [defDates[0], defDates[Math.floor(defDates.length / 2)], TODAY]) {
    const i = defDates.indexOf(d)
    eq(`营业额 ${d} 与库一致`, money(defTurnover)[i], Number(dbAmount.get(d) || 0).toFixed(2))
  }
  const dbSum30 = Number(
    sql(`SELECT IFNULL(SUM(amount), 0) FROM order_main
         WHERE status = 5 AND order_time >= '${BEGIN30} 00:00:00' AND order_time < '${nextDay(TODAY)} 00:00:00'`)
  )
  eq('区间营业额合计与库一致', centsOf(defTurnover), Math.round(dbSum30 * 100))

  // 显式传同一个区间，结果应与缺省完全一致
  const explicit = await call('GET', '/admin/report/turnoverStatistics', {
    token,
    query: { begin: BEGIN30, end: TODAY }
  })
  eq('显式传 30 天区间 → 与缺省结果一致',
    `${explicit.json?.data?.dateList}|${explicit.json?.data?.turnoverList}`,
    `${def.json?.data?.dateList}|${def.json?.data?.turnoverList}`)

  // 90 天长区间：必然出现「库里没有已完成订单」的日期，用来验证补 0
  const begin90 = day(-89)
  const long = await call('GET', '/admin/report/turnoverStatistics', {
    token,
    query: { begin: begin90, end: TODAY }
  })
  const longDates = seriesOf(long.json?.data?.dateList)
  const longTurnover = seriesOf(long.json?.data?.turnoverList)
  eq('90 天区间 → 序列长度 90', longDates.length, 90)
  // 库里有营业额的日期一次查回（不要在天数循环里查库）
  const daysWithOrder = new Set(
    (sql(
      `SELECT DATE(order_time) FROM order_main WHERE status = 5
       AND order_time >= '${begin90} 00:00:00' AND order_time < '${nextDay(TODAY)} 00:00:00'
       GROUP BY DATE(order_time)`
    ) || '')
      .split('\n')
      .filter(Boolean)
  )
  const apiZeroDays = longDates.filter((d, i) => Number(longTurnover[i]) === 0)
  eq('补 0 的日期集合 = 库内没有已完成订单的日期集合',
    JSON.stringify(apiZeroDays),
    JSON.stringify(longDates.filter((d) => !daysWithOrder.has(d))))
  ok('区间内确实存在补 0 的日期（补 0 分支被覆盖）', apiZeroDays.length > 0, `补 0 天数 ${apiZeroDays.length}`)

  // 边界：单日、倒置区间、只传一端、格式非法
  const oneDay = await call('GET', '/admin/report/turnoverStatistics', { token, query: { begin: TODAY, end: TODAY } })
  eq('begin = end → 序列长度 1', seriesOf(oneDay.json?.data?.dateList).length, 1)
  eq('单日营业额与库一致', centsOf(seriesOf(oneDay.json?.data?.turnoverList)), Math.round(
    Number(sql(`SELECT IFNULL(SUM(amount), 0) FROM order_main WHERE status = 5 AND order_time >= '${TODAY} 00:00:00' AND order_time < '${nextDay(TODAY)} 00:00:00'`)) * 100
  ))

  const reversed = await call('GET', '/admin/report/turnoverStatistics', { token, query: { begin: TODAY, end: day(-3) } })
  eq('begin 晚于 end → code 仍为 1', reversed.json?.code, 1)
  eq('begin 晚于 end → dateList 为空串', reversed.json?.data?.dateList, '')
  eq('begin 晚于 end → turnoverList 为空串', reversed.json?.data?.turnoverList, '')

  const onlyEnd = await call('GET', '/admin/report/turnoverStatistics', { token, query: { end: day(-10) } })
  const onlyEndDates = seriesOf(onlyEnd.json?.data?.dateList)
  eq('只传 end → 区间跨度仍是 30 天', onlyEndDates.length, 30)
  eq('只传 end → 末位就是 end', onlyEndDates.at(-1), day(-10))
  eq('只传 end → 首位是 end 往前 29 天', onlyEndDates[0], day(-39))

  const onlyBegin = await call('GET', '/admin/report/turnoverStatistics', { token, query: { begin: day(-2) } })
  eq('只传 begin → end 取今天', seriesOf(onlyBegin.json?.data?.dateList).at(-1), TODAY)

  const badDate = await call('GET', '/admin/report/turnoverStatistics', { token, query: { begin: 'not-a-date' } })
  eq('日期格式非法 → code = 0', badDate.json?.code, 0)
  eq('日期格式非法 → 走全局兜底文案', badDate.json?.msg, '未知错误')

  /* ---------------- 2.9.2 会员统计 ---------------- */
  section('2.9.2 会员统计  GET /admin/report/userStatistics')

  const members = await call('GET', '/admin/report/userStatistics', { token })
  const mDates = seriesOf(members.json?.data?.dateList)
  const newList = seriesOf(members.json?.data?.newMemberList)
  const totalList = seriesOf(members.json?.data?.totalMemberList)
  eq('code = 1', members.json?.code, 1)
  eq('缺省区间 = 最近 30 天', mDates.length, 30)
  eq('区间末位 = 今天', mDates.at(-1), TODAY)
  ok('三个序列等长', newList.length === mDates.length && totalList.length === mDates.length,
    `${mDates.length} / ${newList.length} / ${totalList.length}`)

  const dbNew = dailyFromDb({
    column: 'COUNT(*)',
    table: 'member',
    where: '1 = 1',
    begin: BEGIN30,
    end: TODAY,
    dateColumn: 'create_time'
  })
  eq('每日新增会员序列与库一致', zip(mDates, newList), zip(mDates, mDates.map((d) => String(dbNew.get(d) || 0))))

  // 累计 = 期初（区间开始前的会员数）+ 逐日累加
  let running = Number(sql(`SELECT COUNT(*) FROM member WHERE create_time < '${BEGIN30} 00:00:00'`))
  const expectTotal = mDates.map((d, i) => {
    running += Number(newList[i])
    return String(running)
  })
  eq('累计会员序列 = 期初 + 逐日累加', zip(mDates, totalList), zip(mDates, expectTotal))
  eq('累计末位 = 库内会员总数', Number(totalList.at(-1)), Number(sql('SELECT COUNT(*) FROM member')))
  ok('累计会员数单调不减',
    totalList.every((v, i) => i === 0 || Number(v) >= Number(totalList[i - 1])), JSON.stringify(totalList))
  // 抽查两天，直接与库内「截至当日 24 时」比对，避免只是与自己的算法自洽
  for (const d of [mDates[0], mDates.at(-2)]) {
    eq(`累计会员数 ${d} = 库内截至当日的会员数`,
      Number(totalList[mDates.indexOf(d)]),
      Number(sql(`SELECT COUNT(*) FROM member WHERE create_time < '${nextDay(d)} 00:00:00'`)))
  }

  /* ---------------- 2.9.3 订单统计 ---------------- */
  section('2.9.3 订单统计  GET /admin/report/ordersStatistics')

  const orders = await call('GET', '/admin/report/ordersStatistics', { token })
  const oDates = seriesOf(orders.json?.data?.dateList)
  const orderCounts = seriesOf(orders.json?.data?.orderCountList)
  const validCounts = seriesOf(orders.json?.data?.validOrderCountList)
  eq('code = 1', orders.json?.code, 1)
  eq('缺省区间 = 最近 30 天', oDates.length, 30)
  ok('三个序列等长', orderCounts.length === oDates.length && validCounts.length === oDates.length,
    `${oDates.length} / ${orderCounts.length} / ${validCounts.length}`)

  const dbAll = dailyFromDb({
    column: 'COUNT(*)',
    table: 'order_main',
    where: '1 = 1',
    begin: BEGIN30,
    end: TODAY,
    dateColumn: 'order_time'
  })
  const dbValidDaily = dailyFromDb({
    column: 'COUNT(*)',
    table: 'order_main',
    where: 'status = 5',
    begin: BEGIN30,
    end: TODAY,
    dateColumn: 'order_time'
  })
  eq('每日订单总数序列与库一致', zip(oDates, orderCounts), zip(oDates, oDates.map((d) => String(dbAll.get(d) || 0))))
  eq('每日有效订单序列与库一致', zip(oDates, validCounts), zip(oDates, oDates.map((d) => String(dbValidDaily.get(d) || 0))))

  const win = `order_time >= '${BEGIN30} 00:00:00' AND order_time < '${nextDay(TODAY)} 00:00:00'`
  const dbTotalCount = Number(sql(`SELECT COUNT(*) FROM order_main WHERE ${win}`))
  const dbValidCount = Number(sql(`SELECT COUNT(*) FROM order_main WHERE status = 5 AND ${win}`))
  eq('区间订单总数与库一致', orders.json?.data?.totalOrderCount, dbTotalCount)
  eq('区间有效订单总数与库一致', orders.json?.data?.validOrderCount, dbValidCount)
  eq('totalOrderCount = 每日订单序列之和',
    orders.json?.data?.totalOrderCount, orderCounts.reduce((s, v) => s + Number(v), 0))
  eq('validOrderCount = 每日有效序列之和',
    orders.json?.data?.validOrderCount, validCounts.reduce((s, v) => s + Number(v), 0))
  const expectRate = dbTotalCount === 0 ? 0 : Math.round((dbValidCount / dbTotalCount) * 10000) / 10000
  eq('订单完成率 = 有效 / 总数（保留 4 位）',
    Number(orders.json?.data?.orderCompletionRate).toFixed(4), expectRate.toFixed(4))
  ok('有效订单数不超过订单总数', dbValidCount <= dbTotalCount, `${dbValidCount} / ${dbTotalCount}`)

  /* ---------------- 2.9.4 销量 TOP10 ---------------- */
  section('2.9.4 销量 TOP10  GET /admin/report/top10')

  const top = await call('GET', '/admin/report/top10', { token })
  eq('code = 1', top.json?.code, 1)
  ok('返回的是数组', Array.isArray(top.json?.data), typeof top.json?.data)
  ok('元素只有 name 与 number', top.json?.data?.every((i) => Object.keys(i).sort().join(',') === 'name,number'),
    JSON.stringify(top.json?.data?.[0]))
  ok('条数不超过 10', top.json?.data?.length <= 10, String(top.json?.data?.length))
  ok('销量按降序排列',
    top.json?.data?.every((i, idx) => idx === 0 || i.number <= top.json.data[idx - 1].number),
    JSON.stringify(top.json?.data?.map((i) => i.number)))

  const dbTop = sql(
    `SELECT oi.name, SUM(oi.number) AS n FROM order_item oi
     INNER JOIN order_main om ON om.id = oi.order_id
     WHERE om.status = 5 AND om.order_time >= '${BEGIN30} 00:00:00' AND om.order_time < '${nextDay(TODAY)} 00:00:00'
     GROUP BY oi.name ORDER BY SUM(oi.number) DESC LIMIT 10`
  )
  const expectTop = dbTop
    ? dbTop.split('\n').map((l) => {
        const [name, n] = l.split('\t')
        return { name, number: Number(n) }
      })
    : []
  eq('与库内聚合结果逐条一致（含顺序）', JSON.stringify(top.json?.data), JSON.stringify(expectTop))

  if (top.json?.data?.length) {
    const first = top.json.data[0]
    eq(`榜首「${first.name}」的销量与库一致`, first.number, Number(
      sql(`SELECT IFNULL(SUM(oi.number), 0) FROM order_item oi INNER JOIN order_main om ON om.id = oi.order_id
           WHERE om.status = 5 AND oi.name = '${first.name.replace(/'/g, "''")}'
           AND om.order_time >= '${BEGIN30} 00:00:00' AND om.order_time < '${nextDay(TODAY)} 00:00:00'`)
    ))
  }
  // 只算已完成订单：区间收窄到「今天」时，销量合计不应超过整段区间
  const todayTop = await call('GET', '/admin/report/top10', { token, query: { begin: TODAY, end: TODAY } })
  const todaySum = (todayTop.json?.data || []).reduce((s, i) => s + i.number, 0)
  const monthSum = (top.json?.data || []).reduce((s, i) => s + i.number, 0)
  ok('今天的 TOP10 销量合计不超过近 30 天', todaySum <= monthSum, `${todaySum} / ${monthSum}`)
}
