/**
 * 组合箱模块接口测试（《接口文档》2.7，共 6 个接口）。
 *
 * 重点验证四件容易写错的事：
 * 1. 新增后状态必须是「停售」，整箱温区由内含商品**最高**温区推导；
 * 2. 内含商品的 name / price 由服务端按 goodsId 回查覆盖，不采信前端提交；
 * 3. 「起售中」这个条件在校验后必须一直成立 —— 因此修改一个起售中的组合箱也要重新校验；
 * 4. 删除的两道校验与连带清理（bundle / bundle_goods / cart）。
 */
import { execFileSync } from 'node:child_process'
import { call, eq, ok, section, sql } from './lib.mjs'

const CAT_GOODS = 'xh联调箱商品分类' // type = 1，供测试商品使用
const CAT_BUNDLE = 'xh联调组合分类' // type = 2，供测试组合箱使用
const G1 = 'xh联调箱商品一' // 冷藏（2），上架
const G2 = 'xh联调箱商品二' // 冷冻（3），下架
const G3 = 'xh联调箱商品三' // 常温（1），上架
const G4 = 'xh联调箱商品四' // 冷藏（2），始终下架：用于构造「起售中的组合箱被改出下架商品」
const BX = 'xh联调一周箱'

const REDIS_CLI = process.env.XH_REDIS_CLI || 'D:/tool/Redis/Redis-x64-3.2.100/redis-cli.exe'

function redis(...args) {
  return execFileSync(REDIS_CLI, ['-a', '123456', '-n', '11', ...args], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore']
  }).trim()
}

async function getToken() {
  const res = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  return res.json?.data?.token
}

function bundleRow(name) {
  return sql(
    `SELECT id, category_id, price, IFNULL(image,'<null>'), status, storage_zone, IFNULL(description,'<null>')
     FROM bundle WHERE name = '${name}'`
  ).split('\t')
}

/** 内含商品，按 id 升序 */
function lines(bundleId) {
  return sql(
    `SELECT goods_id, name, price, copies FROM bundle_goods WHERE bundle_id = ${bundleId} ORDER BY id`
  )
    .split('\n')
    .filter(Boolean)
    .map((l) => l.split('\t'))
}

function cleanup() {
  sql(`DELETE FROM cart WHERE bundle_id IN (SELECT id FROM bundle WHERE name LIKE 'xh联调%')`)
  sql(`DELETE FROM cart WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调箱商品%')`)
  sql(`DELETE FROM bundle_goods WHERE bundle_id IN (SELECT id FROM bundle WHERE name LIKE 'xh联调%')`)
  sql(`DELETE FROM bundle WHERE name LIKE 'xh联调%'`)
  sql(`DELETE FROM goods_spec WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调箱商品%')`)
  sql(`DELETE FROM goods WHERE name LIKE 'xh联调箱商品%'`)
  sql(`DELETE FROM goods_category WHERE name LIKE 'xh联调箱商品分类%' OR name LIKE 'xh联调组合分类%'`)
}

export default async function run() {
  const token = await getToken()
  ok('取得管理端令牌', Boolean(token))

  cleanup()

  /* ---------------- 前置：分类与三个商品 ---------------- */
  section('前置：建测试分类与商品')

  for (const [name, type] of [
    [CAT_GOODS, 1],
    [CAT_BUNDLE, 2]
  ]) {
    const res = await call('POST', '/admin/goodsCategory', {
      token,
      body: { type, name, sort: 92 }
    })
    eq(`建分类「${name}」`, res.json?.code, 1)
  }
  const catGoods = Number(sql(`SELECT id FROM goods_category WHERE name = '${CAT_GOODS}'`))
  const catBundle = Number(sql(`SELECT id FROM goods_category WHERE name = '${CAT_BUNDLE}'`))
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catGoods } })
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catBundle } })
  eq('两个测试分类均已启用',
    sql(`SELECT COUNT(*) FROM goods_category WHERE id IN (${catGoods}, ${catBundle}) AND status = 1`), '2')

  const seeds = [
    [G1, 8.8, 2],
    [G2, 15.5, 3],
    [G3, 3.3, 1],
    [G4, 5.5, 2]
  ]
  for (const [name, price, zone] of seeds) {
    const res = await call('POST', '/admin/goods', {
      token,
      body: { name, categoryId: catGoods, price, storageZone: zone }
    })
    eq(`建商品「${name}」`, res.json?.code, 1)
  }
  const id1 = Number(sql(`SELECT id FROM goods WHERE name = '${G1}'`))
  const id2 = Number(sql(`SELECT id FROM goods WHERE name = '${G2}'`))
  const id3 = Number(sql(`SELECT id FROM goods WHERE name = '${G3}'`))
  const id4 = Number(sql(`SELECT id FROM goods WHERE name = '${G4}'`))
  // 商品一、三上架，商品二保持下架（用于「内含商品未上架」的用例）
  for (const id of [id1, id3]) {
    await call('POST', '/admin/goods/status/1', { token, query: { id } })
  }
  eq('商品一、三已上架、商品二为下架',
    sql(`SELECT status FROM goods WHERE id = ${id1}`) +
      sql(`SELECT status FROM goods WHERE id = ${id2}`) +
      sql(`SELECT status FROM goods WHERE id = ${id3}`),
    '101')

  /* ---------------- 1. 新增 ---------------- */
  section('2.7.1 新增组合箱  POST /admin/bundle')

  const addRes = await call('POST', '/admin/bundle', {
    token,
    body: {
      name: BX,
      categoryId: catBundle,
      price: 49.9,
      description: '三口之家一周蔬菜',
      image: null,
      bundleGoods: [
        // 故意提交错误的名字与价格，验证服务端不采信
        { goodsId: id1, name: '假名字', price: 0.01, copies: 2 },
        { goodsId: id2, name: '假名字', price: 0.01, copies: 1 }
      ]
    }
  })
  eq('新增 code = 1', addRes.json?.code, 1)

  const row = bundleRow(BX)
  const bxId = Number(row[0])
  ok('数据已落库且 id 有效', bxId > 0, `id = ${bxId}`)
  eq('分类落库正确', row[1], String(catBundle))
  eq('金额按 decimal 落库', row[2], '49.90')
  eq('未传图片时 image 为 NULL', row[3], '<null>')
  eq('新增后状态为「停售」', row[4], '0')
  eq('整箱温区取内含商品最高温区（2 与 3 → 3）', row[5], '3')

  const lineRows = lines(bxId)
  eq('内含商品落库 2 行', lineRows.length, 2)
  eq('名称由服务端回查覆盖（不是前端提交的「假名字」）', lineRows[0][1], G1)
  eq('单价由服务端回查覆盖（不是前端提交的 0.01）', lineRows[0][2], '8.80')
  eq('份数按提交值落库', lineRows[0][3], '2')
  eq('第二行的商品与单价正确', lineRows[1][1] + '/' + lineRows[1][2], `${G2}/15.50`)

  const dupName = await call('POST', '/admin/bundle', {
    token,
    body: { name: BX, categoryId: catBundle, price: 1, bundleGoods: [{ goodsId: id1, copies: 1 }] }
  })
  eq('组合箱名重复 → code = 0', dupName.json?.code, 0)
  eq('组合箱名重复 → msg', dupName.json?.msg, `${BX}已存在`)

  const emptyLines = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}空`, categoryId: catBundle, price: 1, bundleGoods: [] }
  })
  eq('内含商品为空 → msg', emptyLines.json?.msg, '组合箱至少需要一个内含商品')

  const nullLines = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}空`, categoryId: catBundle, price: 1 }
  })
  eq('未传 bundleGoods → msg', nullLines.json?.msg, '组合箱至少需要一个内含商品')

  const dupGoods = await call('POST', '/admin/bundle', {
    token,
    body: {
      name: `${BX}重`,
      categoryId: catBundle,
      price: 1,
      bundleGoods: [
        { goodsId: id1, copies: 1 },
        { goodsId: id1, copies: 2 }
      ]
    }
  })
  eq('同一商品重复 → msg', dupGoods.json?.msg, '组合箱内含商品重复')

  const badCopies = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}份`, categoryId: catBundle, price: 1, bundleGoods: [{ goodsId: id1, copies: 0 }] }
  })
  eq('份数为 0 → msg', badCopies.json?.msg, '内含商品的份数必须大于 0')

  const badGoods = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}鬼`, categoryId: catBundle, price: 1, bundleGoods: [{ goodsId: 999999, copies: 1 }] }
  })
  eq('商品不存在 → msg', badGoods.json?.msg, '商品不存在')

  const wrongType = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}型`, categoryId: catGoods, price: 1, bundleGoods: [{ goodsId: id1, copies: 1 }] }
  })
  eq('挂到商品分类（type = 1）→ msg', wrongType.json?.msg, '分类类型不匹配')

  const badCat = await call('POST', '/admin/bundle', {
    token,
    body: { name: `${BX}分类`, categoryId: 999999, price: 1, bundleGoods: [{ goodsId: id1, copies: 1 }] }
  })
  eq('分类不存在 → msg', badCat.json?.msg, '分类不存在')

  eq('校验失败时未产生多余数据',
    sql(`SELECT COUNT(*) FROM bundle WHERE name LIKE 'xh联调一周箱%'`), '1')

  /* ---------------- 2. 分页查询 ---------------- */
  section('2.7.2 组合箱分页查询  GET /admin/bundle/page')

  const dbTotal = Number(sql('SELECT COUNT(*) FROM bundle'))
  const page = await call('GET', '/admin/bundle/page', {
    token,
    query: { page: 1, pageSize: 200 }
  })
  eq('分页 code = 1', page.json?.code, 1)
  eq('total 与库中记录数一致', page.json?.data?.total, dbTotal)
  const listed = page.json?.data?.records?.find((r) => r.id === bxId)
  eq('联表带出分类名称', listed?.categoryName, CAT_BUNDLE)
  ok('列表含温区与更新时间',
    listed && 'storageZone' in listed && 'updateTime' in listed)
  ok('列表不返回 bundleGoods', listed && !('bundleGoods' in listed), JSON.stringify(Object.keys(listed || {})))

  const byName = await call('GET', '/admin/bundle/page', {
    token,
    query: { name: '联调一周箱', page: 1, pageSize: 200 }
  })
  eq('名称模糊查询命中 1 条', byName.json?.data?.total, 1)

  const byStatus = await call('GET', '/admin/bundle/page', {
    token,
    query: { status: 1, page: 1, pageSize: 200 }
  })
  eq('按状态筛选 total 与库一致',
    byStatus.json?.data?.total, Number(sql('SELECT COUNT(*) FROM bundle WHERE status = 1')))
  ok('筛选结果全部为起售',
    byStatus.json?.data?.records?.every((r) => r.status === 1))

  const wrongCat = await call('GET', '/admin/bundle/page', {
    token,
    query: { categoryId: catGoods, page: 1, pageSize: 200 }
  })
  eq('按商品分类筛选 → 无结果', wrongCat.json?.data?.total, 0)

  /* ---------------- 3. 详情 ---------------- */
  section('2.7.4 根据 id 查询组合箱  GET /admin/bundle/{id}')

  const detail = await call('GET', `/admin/bundle/${bxId}`, { token })
  eq('详情 code = 1', detail.json?.code, 1)
  eq('详情带出分类名称', detail.json?.data?.categoryName, CAT_BUNDLE)
  eq('详情返回内含商品 2 项', detail.json?.data?.bundleGoods?.length, 2)
  const d0 = detail.json?.data?.bundleGoods?.[0]
  eq('内含商品带商品 id', d0?.goodsId, id1)
  eq('内含商品带快照名称', d0?.name, G1)
  eq('内含商品带快照单价', d0?.price, 8.8)
  eq('内含商品带份数', d0?.copies, 2)

  const noBundle = await call('GET', '/admin/bundle/999999', { token })
  eq('组合箱不存在 → code = 0', noBundle.json?.code, 0)
  eq('组合箱不存在 → msg', noBundle.json?.msg, '组合箱不存在')

  /* ---------------- 4. 起售 / 停售 ---------------- */
  section('2.7.6 组合箱起售/停售  POST /admin/bundle/status/{status}')

  // 4.1 内含商品（商品二）处于下架 → 不能起售
  const enableBadGoods = await call('POST', '/admin/bundle/status/1', {
    token,
    query: { id: bxId }
  })
  eq('内含商品未上架 → code = 0', enableBadGoods.json?.code, 0)
  eq('内含商品未上架 → msg', enableBadGoods.json?.msg, '组合箱内含商品未上架，无法起售')
  eq('状态未被改动', bundleRow(BX)[4], '0')

  // 4.2 所属分类被禁用 → 不能起售
  await call('POST', '/admin/goods/status/1', { token, query: { id: id2 } })
  await call('POST', '/admin/goodsCategory/status/0', { token, query: { id: catBundle } })
  const enableDisabledCat = await call('POST', '/admin/bundle/status/1', {
    token,
    query: { id: bxId }
  })
  eq('分类禁用 → code = 0', enableDisabledCat.json?.code, 0)
  eq('分类禁用 → msg', enableDisabledCat.json?.msg, '所属分类已禁用，无法起售')
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catBundle } })

  // 4.3 起售成功，并清空该分类的组合箱缓存（按分类分片，不波及其它分类）
  const cacheKey = `xh:bundle:category:${catBundle}`
  const otherKey = `xh:bundle:category:${catGoods}`
  redis('set', cacheKey, 'stale')
  redis('set', otherKey, 'keep')
  const enable = await call('POST', '/admin/bundle/status/1', { token, query: { id: bxId } })
  eq('起售 code = 1', enable.json?.code, 1)
  eq('库中状态为 1', bundleRow(BX)[4], '1')
  eq(`已清空 ${cacheKey}`, redis('exists', cacheKey), '0')
  eq('其它分类的缓存未被波及', redis('exists', otherKey), '1')
  redis('del', otherKey)

  // 4.4 起售中组合箱里的商品不允许被下架（商品侧的对称规则，两条互为保险）
  const disableInside = await call('POST', '/admin/goods/status/0', { token, query: { id: id2 } })
  eq('起售中组合箱里的商品不能下架',
    disableInside.json?.msg, '商品被起售中的组合箱引用，无法下架')

  // 4.5 起售中的组合箱改成含下架商品 → 必须被拒（否则等于绕过起售校验）
  const patchInSale = await call('PUT', '/admin/bundle', {
    token,
    body: {
      id: bxId,
      name: BX,
      categoryId: catBundle,
      price: 49.9,
      bundleGoods: [
        { goodsId: id3, copies: 1 },
        { goodsId: id4, copies: 1 }
      ]
    }
  })
  eq('起售中改出下架商品 → code = 0', patchInSale.json?.code, 0)
  eq('起售中改出下架商品 → msg', patchInSale.json?.msg, '组合箱内含商品未上架，无法起售')
  eq('改动被整体回滚（内含商品仍为原来的 2 项）', lines(bxId).length, 2)
  eq('回滚后第一项仍是商品一', lines(bxId)[0][1], G1)

  /* ---------------- 5. 修改 ---------------- */
  section('2.7.5 修改组合箱  PUT /admin/bundle')

  const upd = await call('PUT', '/admin/bundle', {
    token,
    body: {
      id: bxId,
      name: BX,
      categoryId: catBundle,
      price: 39.9,
      description: '改后的描述',
      image: 'https://example.com/xh-bx.png',
      status: 0,
      bundleGoods: [{ goodsId: id3, name: '假名字', price: 0.01, copies: 3 }]
    }
  })
  eq('修改 code = 1', upd.json?.code, 1)

  const afterUpd = bundleRow(BX)
  eq('售价已更新', afterUpd[2], '39.90')
  eq('描述已更新', afterUpd[6], '改后的描述')
  eq('图片已写入', afterUpd[3], 'https://example.com/xh-bx.png')
  eq('提交 status 被忽略', afterUpd[4], '1')
  eq('温区按新列表重新推导（商品三为常温 → 1）', afterUpd[5], '1')
  const newLines = lines(bxId)
  eq('内含商品全量覆盖后只剩 1 项', newLines.length, 1)
  eq('覆盖后商品为商品三', newLines[0][1], G3)
  eq('冗余单价同样由服务端回填', newLines[0][2], '3.30')
  eq('份数按提交值落库', newLines[0][3], '3')

  const noId = await call('PUT', '/admin/bundle', {
    token,
    body: { name: BX, categoryId: catBundle, price: 1, bundleGoods: [{ goodsId: id3, copies: 1 }] }
  })
  eq('漏传 id → msg', noId.json?.msg, '组合箱不存在')

  // 动态更新：未提交的可空字段保持原值
  const untouched = await call('PUT', '/admin/bundle', {
    token,
    body: { id: bxId, name: BX, categoryId: catBundle, price: 39.9, bundleGoods: [{ goodsId: id3, copies: 3 }] }
  })
  eq('未提交可空字段 code = 1', untouched.json?.code, 1)
  eq('未提交 description → 保持原值', bundleRow(BX)[6], '改后的描述')
  eq('未提交 image → 保持原值', bundleRow(BX)[3], 'https://example.com/xh-bx.png')

  /* ---------------- 6. 批量删除 ---------------- */
  section('2.7.3 批量删除组合箱  DELETE /admin/bundle')

  // 6.1 起售中的不能删
  const delOnSale = await call('DELETE', '/admin/bundle', { token, query: { ids: String(bxId) } })
  eq('起售中 → code = 0', delOnSale.json?.code, 0)
  eq('起售中 → msg', delOnSale.json?.msg, '组合箱处于起售状态，不能删除')
  eq('组合箱未被删除', sql(`SELECT COUNT(*) FROM bundle WHERE id = ${bxId}`), '1')

  // 6.2 空 ids 不报错
  const emptyIds = await call('DELETE', '/admin/bundle', { token, query: { ids: '' } })
  eq('ids 为空 → code = 1（幂等）', emptyIds.json?.code, 1)

  // 6.3 停售后可删，bundle_goods 与 cart 一并清理
  await call('POST', '/admin/bundle/status/0', { token, query: { id: bxId } })
  eq('停售成功', bundleRow(BX)[4], '0')
  sql(`INSERT INTO cart (name, member_id, bundle_id, number, amount)
       VALUES ('${BX}', 1, ${bxId}, 1, 39.90)`)
  eq('夹具购物车行已写入', sql(`SELECT COUNT(*) FROM cart WHERE bundle_id = ${bxId}`), '1')

  const delOk = await call('DELETE', '/admin/bundle', { token, query: { ids: String(bxId) } })
  eq('可删除组合箱', delOk.json?.code, 1)
  eq('bundle 记录已删除', sql(`SELECT COUNT(*) FROM bundle WHERE id = ${bxId}`), '0')
  eq('bundle_goods 已连带删除', sql(`SELECT COUNT(*) FROM bundle_goods WHERE bundle_id = ${bxId}`), '0')
  eq('购物车中的对应行已连带删除', sql(`SELECT COUNT(*) FROM cart WHERE bundle_id = ${bxId}`), '0')

  const notFound = await call('POST', '/admin/bundle/status/1', { token, query: { id: 999999 } })
  eq('组合箱不存在 → msg', notFound.json?.msg, '组合箱不存在')

  /* ---------------- 后置：清理 ---------------- */
  cleanup()
  redis('del', 'xh:bundle:list')
  console.log(
    `\n（已清理测试数据，组合箱 ${sql('SELECT COUNT(*) FROM bundle')} 个、内含商品 ${
      sql('SELECT COUNT(*) FROM bundle_goods')
    } 行、商品 ${sql('SELECT COUNT(*) FROM goods')} 个）`
  )
}
