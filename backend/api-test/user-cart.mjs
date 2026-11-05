/**
 * 用户端购物车测试（《接口文档-用户端》7）。
 *
 * 重点：同款判定（会员 + 商品/组合箱 + 规格文本）、数量由服务端加减、小计口径、会员隔离。
 * 用两个非种子会员 id（9991 / 9992）做测试，不碰种子会员的购物车数据。
 */
import { call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const ME = 9991
const OTHER = 9992
const GOODS = 1 // 种子商品「有机小油菜」，4.90，含规格
const GOODS_OTHER = 26 // 种子商品「冰鲜海鲈鱼」，28.80
const BUNDLE = 1 // 种子组合箱「一周蔬菜箱」，49.90

const SPEC = '净重:约500g'

let me
let other

const cartRows = (memberId = ME) =>
  sql(`SELECT goods_id, bundle_id, IFNULL(spec_text,'<null>'), number, amount
       FROM cart WHERE member_id = ${memberId} ORDER BY id ASC`)
    .split('\n')
    .filter(Boolean)

function cleanup() {
  sql(`DELETE FROM cart WHERE member_id IN (${ME}, ${OTHER})`)
}

export default async function run() {
  me = mintUserToken(ME)
  other = mintUserToken(OTHER)
  ok('自签两个会员的令牌', Boolean(me) && Boolean(other))

  cleanup()
  eq('前置：两个测试会员的购物车已清空',
    sql(`SELECT COUNT(*) FROM cart WHERE member_id IN (${ME}, ${OTHER})`), '0')

  /* ---------------- 查看空车 ---------------- */
  section('7.2 查看购物车（空车）')

  const empty = await call('GET', '/user/cart/list', { userToken: me })
  eq('code = 1', empty.json?.code, 1)
  eq('空车返回空数组', empty.json?.data?.length, 0)

  const noToken = await call('GET', '/user/cart/list')
  eq('不带令牌 → code = 0', noToken.json?.code, 0)

  /* ---------------- 加购 ---------------- */
  section('7.1 添加购物车  POST /user/cart/add')

  const add = await call('POST', '/user/cart/add', {
    userToken: me,
    body: { goodsId: GOODS, specText: SPEC }
  })
  eq('加购 code = 1', add.json?.code, 1)

  const rows1 = cartRows()
  eq('落库 1 行', rows1.length, 1)
  const r1 = rows1[0].split('\t')
  eq('商品 id 正确', r1[0], String(GOODS))
  eq('规格文本落库正确', r1[2], SPEC)
  eq('数量为 1（请求体不带数量）', r1[3], '1')
  eq('小计 = 单价 × 1', r1[4], sql(`SELECT price FROM goods WHERE id = ${GOODS}`))
  eq('会员 id 取自令牌', sql(`SELECT member_id FROM cart WHERE member_id = ${ME}`), String(ME))
  eq('名称快照取的是商品名',
    sql(`SELECT name FROM cart WHERE member_id = ${ME}`), sql(`SELECT name FROM goods WHERE id = ${GOODS}`))
  eq('图片快照与商品一致',
    sql(`SELECT IFNULL(image,'<null>') FROM cart WHERE member_id = ${ME}`),
    sql(`SELECT IFNULL(image,'<null>') FROM goods WHERE id = ${GOODS}`))

  const again = await call('POST', '/user/cart/add', {
    userToken: me,
    body: { goodsId: GOODS, specText: SPEC }
  })
  eq('再次加购同款 code = 1', again.json?.code, 1)
  const rows2 = cartRows()
  eq('同款仍然只有 1 行（合并）', rows2.length, 1)
  eq('数量变成 2', rows2[0].split('\t')[3], '2')
  const unit = Number(sql(`SELECT price FROM goods WHERE id = ${GOODS}`))
  eq('小计刷新为 单价 × 2', rows2[0].split('\t')[4], (unit * 2).toFixed(2))

  // 同商品换一个规格：必须新开一行，不能并进上一行
  await call('POST', '/user/cart/add', {
    userToken: me,
    body: { goodsId: GOODS, specText: '净重:约1kg' }
  })
  eq('同商品不同规格 → 新开一行', cartRows().length, 2)

  // 同商品不带规格：与「有规格」的行也不能合并（参照工程在这一点上会串行）
  await call('POST', '/user/cart/add', { userToken: me, body: { goodsId: GOODS } })
  eq('同商品无规格 → 再新开一行', cartRows().length, 3)
  eq('无规格行的 spec_text 是 NULL',
    cartRows().filter((l) => l.split('\t')[2] === '<null>').length, 1)

  // 组合箱同样能加，且与商品行互不干扰
  await call('POST', '/user/cart/add', { userToken: me, body: { bundleId: BUNDLE } })
  eq('组合箱新增一行', cartRows().length, 4)
  const bundleRow = cartRows().find((l) => l.split('\t')[1] === String(BUNDLE))
  eq('组合箱小计 = 组合箱售价', bundleRow.split('\t')[4], sql(`SELECT price FROM bundle WHERE id = ${BUNDLE}`))

  /* ---------------- 入参校验 ---------------- */
  section('加购入参校验')

  const noTarget = await call('POST', '/user/cart/add', { userToken: me, body: {} })
  eq('既没给商品也没给组合箱 → msg', noTarget.json?.msg, '商品或组合箱必须二选一')

  const ghostGoods = await call('POST', '/user/cart/add', { userToken: me, body: { goodsId: 999999 } })
  eq('商品不存在 → msg', ghostGoods.json?.msg, '商品不存在')

  const ghostBundle = await call('POST', '/user/cart/add', { userToken: me, body: { bundleId: 999999 } })
  eq('组合箱不存在 → msg', ghostBundle.json?.msg, '组合箱不存在')

  eq('校验失败不会留下脏数据', cartRows().length, 4)

  // 请求体里塞 memberId 不会生效：身份只认令牌
  // （注意要按 member_id 限定：种子会员 1 的购物车里也有同一件商品）
  await call('POST', '/user/cart/add', {
    userToken: me,
    body: { goodsId: GOODS_OTHER, memberId: OTHER, number: 99, amount: 0.01 }
  })
  eq('请求体里的 memberId 被忽略，行仍挂在自己名下',
    sql(`SELECT member_id FROM cart WHERE member_id = ${ME} AND goods_id = ${GOODS_OTHER}`), String(ME))
  eq('塞进来的数量被忽略（仍为 1）',
    sql(`SELECT number FROM cart WHERE member_id = ${ME} AND goods_id = ${GOODS_OTHER}`), '1')
  eq('塞进来的金额被忽略（取商品售价）',
    sql(`SELECT amount FROM cart WHERE member_id = ${ME} AND goods_id = ${GOODS_OTHER}`),
    sql(`SELECT price FROM goods WHERE id = ${GOODS_OTHER}`))
  eq('种子会员 1 的那一行没被碰',
    sql(`SELECT number FROM cart WHERE member_id = 1 AND goods_id = ${GOODS_OTHER}`), '1')

  /* ---------------- 减购 ---------------- */
  section('7.4 减少一件  POST /user/cart/sub')

  const sub = await call('POST', '/user/cart/sub', { userToken: me, body: { goodsId: GOODS, specText: SPEC } })
  eq('减购 code = 1', sub.json?.code, 1)
  const specRow = () => cartRows().find((l) => l.split('\t')[2] === SPEC)
  eq('数量 2 → 1', specRow().split('\t')[3], '1')
  eq('小计随之回到 单价 × 1', specRow().split('\t')[4], unit.toFixed(2))

  await call('POST', '/user/cart/sub', { userToken: me, body: { goodsId: GOODS, specText: SPEC } })
  // 此时车上还剩：商品甲的「约1kg」行、商品甲的无规格行、组合箱行、商品乙行
  eq('减到 0 时该行被删除（剩 4 行）', cartRows().length, 4)
  eq('该规格行确实没了', cartRows().filter((l) => l.split('\t')[2] === SPEC).length, 0)

  const subGhost = await call('POST', '/user/cart/sub', { userToken: me, body: { goodsId: GOODS, specText: SPEC } })
  eq('再减一件已删掉的行 → msg', subGhost.json?.msg, '购物车为空')

  const subNoTarget = await call('POST', '/user/cart/sub', { userToken: me, body: {} })
  eq('减购不带目标 → msg', subNoTarget.json?.msg, '商品或组合箱必须二选一')

  /* ---------------- 会员隔离 ---------------- */
  section('会员隔离与列表')

  const mine = await call('GET', '/user/cart/list', { userToken: me })
  eq('自己的购物车 4 行', mine.json?.data?.length, 4)
  ok('响应不再暴露 memberId（会员维度只由服务端掌握）',
    mine.json?.data?.every((c) => c.memberId === undefined), JSON.stringify(mine.json?.data?.[0]))
  eq('库里这 4 行都属于自己', sql(`SELECT COUNT(*) FROM cart WHERE member_id = ${ME}`), '4')
  const rowIds = (mine.json?.data || []).map((c) => c.id)
  ok('按加入顺序（id 升序）', JSON.stringify(rowIds) === JSON.stringify([...rowIds].sort((a, b) => a - b)))

  const othersCart = await call('GET', '/user/cart/list', { userToken: other })
  eq('另一个会员看不到这些行', othersCart.json?.data?.length, 0)

  /* ---------------- 清空 ---------------- */
  section('7.3 清空购物车  DELETE /user/cart/clean')

  const clean = await call('DELETE', '/user/cart/clean', { userToken: me })
  eq('清空 code = 1', clean.json?.code, 1)
  eq('库中已无该会员的行', sql(`SELECT COUNT(*) FROM cart WHERE member_id = ${ME}`), '0')
  const afterClean = await call('GET', '/user/cart/list', { userToken: me })
  eq('列表变空', afterClean.json?.data?.length, 0)

  /* ---------------- 7.2 按现值返回 ---------------- */
  section('7.2 购物车按现值返回（名称 / 价格 / 上下架状态）')

  // 直接改库来模拟「管理端改价 / 下架」：这里要验的是购物车读路径对库内变化的反应，
  // 走管理端接口会牵扯到「商品下架的连带校验」，那是另一处已经单独测过的逻辑。
  const PRICE_OLD = sql(`SELECT price FROM goods WHERE id = ${GOODS}`)
  const GOODS_STATUS_OLD = sql(`SELECT status FROM goods WHERE id = ${GOODS}`)
  const GOODS_NAME = sql(`SELECT name FROM goods WHERE id = ${GOODS}`)

  const line = async () => {
    const res = await call('GET', '/user/cart/list', { userToken: me })
    return (res.json?.data || []).find((x) => x.goodsId === GOODS && x.specText === SPEC)
  }

  await call('POST', '/user/cart/add', { userToken: me, body: { goodsId: GOODS, specText: SPEC } })
  const fresh = await line()
  eq('未改动时 status = 1（在售）', fresh?.status, 1)
  eq('未改动时 priceChanged = false', fresh?.priceChanged, false)
  /* 金额在 JSON 里是数字（服务端没把 BigDecimal 转成字符串），比数值不比字面量 */
  eq('小计 = 现值 × 数量', Number(fresh?.amount), Number(PRICE_OLD))

  sql(`UPDATE goods SET price = 9.90 WHERE id = ${GOODS}`)
  const repriced = await line()
  eq('管理端改价后，购物车小计按现值重算', Number(repriced?.amount), 9.9)
  eq('并标记 priceChanged = true', repriced?.priceChanged, true)
  eq('库里那一行的快照小计没有被改（留痕）',
    sql(`SELECT amount FROM cart WHERE member_id = ${ME} AND goods_id = ${GOODS} AND spec_text = '${SPEC}'`),
    PRICE_OLD)
  eq('加一件时仍按快照单价重算库中小计（不改用现价）', await (async () => {
    await call('POST', '/user/cart/add', { userToken: me, body: { goodsId: GOODS, specText: SPEC } })
    return sql(`SELECT amount FROM cart WHERE member_id = ${ME} AND goods_id = ${GOODS} AND spec_text = '${SPEC}'`)
  })(), '9.80')

  sql(`UPDATE goods SET name = '改名后的油菜' WHERE id = ${GOODS}`)
  eq('管理端改名后，购物车显示新名称', (await line())?.name, '改名后的油菜')

  sql(`UPDATE goods SET status = 0 WHERE id = ${GOODS}`)
  eq('商品下架后 status = 0（不可结算）', (await line())?.status, 0)

  sql(`UPDATE goods SET price = ${PRICE_OLD}, name = '${GOODS_NAME}', status = ${GOODS_STATUS_OLD} WHERE id = ${GOODS}`)
  eq('恢复后 status 回到 1', (await line())?.status, 1)
  eq('金额仍按现值 × 数量算', Number((await line())?.amount), Number(PRICE_OLD) * 2)

  /* ---------------- 7.5 移除一行 ---------------- */
  section('7.5 移除购物车中的一行  DELETE /user/cart?id=')

  const cartNow = await call('GET', '/user/cart/list', { userToken: me })
  const targetId = (cartNow.json?.data || []).find((x) => x.goodsId === GOODS)?.id
  ok('取到了要移除的行 id', Boolean(targetId))

  // 先给另一个会员也放一行，验证归属隔离
  await call('POST', '/user/cart/add', { userToken: other, body: { goodsId: GOODS } })
  const otherLineId = Number(sql(`SELECT id FROM cart WHERE member_id = ${OTHER} AND goods_id = ${GOODS}`))

  const steal = await call('DELETE', '/user/cart', { userToken: me, query: { id: otherLineId } })
  eq('删别人的行 → 购物车为空', steal.json?.msg, '购物车为空')
  eq('别人的行还在', sql(`SELECT COUNT(*) FROM cart WHERE id = ${otherLineId}`), '1')

  const ghost = await call('DELETE', '/user/cart', { userToken: me, query: { id: 99999999 } })
  eq('删不存在的行 → 购物车为空', ghost.json?.msg, '购物车为空')

  const removed = await call('DELETE', '/user/cart', { userToken: me, query: { id: targetId } })
  eq('删自己的行成功', removed.json?.code, 1)
  eq('库中该行已删', sql(`SELECT COUNT(*) FROM cart WHERE id = ${targetId}`), '0')

  /* ---------------- 后置清理 ---------------- */
  cleanup()
  console.log('\n（已清理测试会员的购物车）')
}
