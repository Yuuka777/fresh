/**
 * 用户端商品浏览测试（《接口文档-用户端》4、5、6）。
 *
 * 这一块是用户端最热的读路径，除了「返回什么」还要验证缓存行为：
 * 命中、空值哨兵（防穿透）、以及管理端改价/上下架后缓存是否真的失效。
 */
import { execFileSync } from 'node:child_process'
import { call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const CAT = 'xh联调用户端分类' // type = 1，有商品
const CAT_EMPTY = 'xh联调用户端空分类' // type = 1，启用但无商品
const CAT_BUNDLE = 'xh联调用户端组合分类' // type = 2
const GOODS_A = 'xh联调用户端商品甲' // 有规格
const GOODS_B = 'xh联调用户端商品乙' // 无规格
const BUNDLE = 'xh联调用户端组合箱'

const REDIS_CLI = process.env.XH_REDIS_CLI || 'D:/tool/Redis/Redis-x64-3.2.100/redis-cli.exe'
const redis = (...args) =>
  execFileSync(REDIS_CLI, ['-a', '123456', '-n', '11', ...args], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore']
  }).trim()

let token // 用户端令牌（自签）
let admin // 管理端令牌
let catId = 0
let catEmptyId = 0
let catBundleId = 0

async function getToken() {
  const res = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  return res.json?.data?.token
}

function cleanup() {
  sql(`DELETE FROM cart WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调用户端%')`)
  sql(`DELETE FROM cart WHERE bundle_id IN (SELECT id FROM bundle WHERE name LIKE 'xh联调用户端%')`)
  sql(`DELETE FROM bundle_goods WHERE bundle_id IN (SELECT id FROM bundle WHERE name LIKE 'xh联调用户端%')`)
  sql(`DELETE FROM bundle WHERE name LIKE 'xh联调用户端%'`)
  sql(`DELETE FROM goods_spec WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调用户端%')`)
  sql(`DELETE FROM goods WHERE name LIKE 'xh联调用户端%'`)
  sql(`DELETE FROM goods_category WHERE name LIKE 'xh联调用户端%'`)
  // 缓存键按分类 id 清（分类名不是键的一部分）
  for (const id of [catId, catEmptyId, catBundleId]) {
    if (id) {
      redis('del', `xh:goods:category:${id}`)
      redis('del', `xh:bundle:category:${id}`)
    }
  }
}

const gidOf = (name) => Number(sql(`SELECT id FROM goods WHERE name = '${name}'`))
const catIdOf = (name) => Number(sql(`SELECT id FROM goods_category WHERE name = '${name}'`))

export default async function run() {
  admin = await getToken()
  ok('取得管理端令牌', Boolean(admin))
  token = mintUserToken(1)
  ok('自签用户端令牌（同一密钥，等价于登录签发的）', Boolean(token))

  cleanup()

  /* ---------------- 前置：三个分类 + 两个商品 ---------------- */
  section('前置：建测试数据')

  for (const [name, type, sort] of [
    [CAT, 1, 93],
    [CAT_EMPTY, 1, 94],
    [CAT_BUNDLE, 2, 95]
  ]) {
    const res = await call('POST', '/admin/goodsCategory', { token: admin, body: { type, name, sort } })
    eq(`建分类「${name}」`, res.json?.code, 1)
  }
  const cat = catIdOf(CAT)
  const catEmpty = catIdOf(CAT_EMPTY)
  const catBundle = catIdOf(CAT_BUNDLE)
  catId = cat
  catEmptyId = catEmpty
  catBundleId = catBundle
  for (const id of [cat, catEmpty, catBundle]) {
    await call('POST', '/admin/goodsCategory/status/1', { token: admin, query: { id } })
  }

  await call('POST', '/admin/goods', {
    token: admin,
    body: {
      name: GOODS_A,
      categoryId: cat,
      price: 12.5,
      storageZone: 2,
      specs: [{ name: '净重', value: ['约500g', '约1kg'] }]
    }
  })
  await call('POST', '/admin/goods', {
    token: admin,
    body: { name: GOODS_B, categoryId: cat, price: 6.6, storageZone: 1 }
  })
  const gidA = gidOf(GOODS_A)
  const gidB = gidOf(GOODS_B)
  for (const id of [gidA, gidB]) {
    await call('POST', '/admin/goods/status/1', { token: admin, query: { id } })
  }
  eq('两个测试商品都已上架',
    sql(`SELECT COUNT(*) FROM goods WHERE id IN (${gidA}, ${gidB}) AND status = 1`), '2')

  /* ---------------- 分类列表 ---------------- */
  section('4.1 查询分类列表  GET /user/goodsCategory/list')

  const catNoToken = await call('GET', '/user/goodsCategory/list')
  eq('不带令牌 → code = 0', catNoToken.json?.code, 0)

  const catList = await call('GET', '/user/goodsCategory/list', { userToken: token })
  eq('code = 1', catList.json?.code, 1)
  const enabledDb = Number(sql('SELECT COUNT(*) FROM goods_category WHERE status = 1'))
  eq('条数与库中「启用中」的分类一致（禁用项不返回）', catList.json?.data?.length, enabledDb)
  ok('全部是启用状态', catList.json?.data?.every((c) => c.status === 1))
  const sorts = (catList.json?.data || []).map((c) => c.sort)
  ok('按 sort 升序',
    JSON.stringify(sorts) === JSON.stringify([...sorts].sort((a, b) => a - b)), sorts.join(','))

  const catType1 = await call('GET', '/user/goodsCategory/list', { userToken: token, query: { type: 1 } })
  ok('type = 1 只返回商品分类', catType1.json?.data?.every((c) => c.type === 1))
  const catType2 = await call('GET', '/user/goodsCategory/list', { userToken: token, query: { type: 2 } })
  ok('type = 2 只返回组合分类', catType2.json?.data?.every((c) => c.type === 2))

  await call('POST', '/admin/goodsCategory/status/0', { token: admin, query: { id: catEmpty } })
  const afterDisable = await call('GET', '/user/goodsCategory/list', { userToken: token })
  ok('被禁用的分类不再返回',
    !afterDisable.json?.data?.some((c) => c.id === catEmpty))
  await call('POST', '/admin/goodsCategory/status/1', { token: admin, query: { id: catEmpty } })
  const afterEnable = await call('GET', '/user/goodsCategory/list', { userToken: token })
  ok('重新启用后回来', afterEnable.json?.data?.some((c) => c.id === catEmpty))

  /* ---------------- 商品列表 ---------------- */
  section('5.1 按分类查询商品  GET /user/goods/list')

  const noToken = await call('GET', '/user/goods/list', { query: { categoryId: cat } })
  eq('不带令牌 → code = 0', noToken.json?.code, 0)

  redis('del', `xh:goods:category:${cat}`)
  const list = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: cat } })
  eq('code = 1', list.json?.code, 1)
  const onSaleDb = Number(sql(`SELECT COUNT(*) FROM goods WHERE category_id = ${cat} AND status = 1`))
  eq('条数与库中「上架」商品一致', list.json?.data?.length, onSaleDb)
  ok('全部是上架商品', list.json?.data?.every((g) => g.status === 1))

  const itemA = list.json?.data?.find((g) => g.id === gidA)
  const itemB = list.json?.data?.find((g) => g.id === gidB)
  ok('带出规格列表', Array.isArray(itemA?.specs) && itemA.specs.length === 1, JSON.stringify(itemA?.specs))
  eq('规格名称正确', itemA?.specs?.[0]?.name, '净重')
  ok('规格候选值是数组（不是 JSON 文本）', Array.isArray(itemA?.specs?.[0]?.value))
  eq('候选值内容正确', JSON.stringify(itemA?.specs?.[0]?.value), '["约500g","约1kg"]')
  ok('无规格商品的 specs 不返回（前端据此直接加购）', itemB && !('specs' in itemB), JSON.stringify(Object.keys(itemB || {})))
  eq('温区带出正确', itemB?.storageZone, 1)

  /* ---------------- 缓存三防 ---------------- */
  section('缓存：命中 / 失效 / 空值哨兵')

  const cacheKey = `xh:goods:category:${cat}`
  eq(`请求后缓存已写入 ${cacheKey}`, redis('exists', cacheKey), '1')

  // 绕过接口直接改库：缓存若命中，用户端应仍看到旧价
  sql(`UPDATE goods SET price = 99.90 WHERE id = ${gidA}`)
  const cachedHit = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: cat } })
  eq('缓存命中：直改库后用户端仍返回旧价',
    cachedHit.json?.data?.find((g) => g.id === gidA)?.price, 12.5)

  // 走管理端改价：接口会清缓存，用户端应立刻看到新价
  await call('PUT', '/admin/goods', {
    token: admin,
    body: { id: gidA, name: GOODS_A, categoryId: cat, price: 15.5, storageZone: 2, specs: [{ name: '净重', value: ['约500g'] }] }
  })
  const afterUpdate = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: cat } })
  eq('管理端改价后缓存被清，用户端立刻是新价',
    afterUpdate.json?.data?.find((g) => g.id === gidA)?.price, 15.5)

  // 上下架同样要清缓存
  await call('POST', '/admin/goods/status/0', { token: admin, query: { id: gidB } })
  const afterOff = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: cat } })
  ok('下架后立刻从列表消失', !afterOff.json?.data?.some((g) => g.id === gidB))
  await call('POST', '/admin/goods/status/1', { token: admin, query: { id: gidB } })
  const afterOn = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: cat } })
  ok('重新上架后回来', afterOn.json?.data?.some((g) => g.id === gidB))

  // 空分类：写入空值哨兵，后续请求不再穿透到库
  redis('del', `xh:goods:category:${catEmpty}`)
  const empty1 = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: catEmpty } })
  eq('空分类返回空数组', empty1.json?.data?.length, 0)
  eq('空分类写入空值哨兵', redis('get', `xh:goods:category:${catEmpty}`), '[]')
  const empty2 = await call('GET', '/user/goods/list', { userToken: token, query: { categoryId: catEmpty } })
  eq('再查一次仍是空数组（哨兵被当作命中）', empty2.json?.data?.length, 0)

  /* ---------------- 搜索 ---------------- */
  section('5.2 按关键词搜索商品  GET /user/goods/search')

  const kw = '用户端商品'
  const search = await call('GET', '/user/goods/search', { userToken: token, query: { name: kw } })
  eq('code = 1', search.json?.code, 1)
  eq('模糊命中 2 条', search.json?.data?.length, 2)
  ok('只返回上架商品', search.json?.data?.every((g) => g.status === 1))
  const ids = (search.json?.data || []).map((g) => g.id)
  ok('按 id 升序', JSON.stringify(ids) === JSON.stringify([...ids].sort((a, b) => a - b)), ids.join(','))
  ok('结果与分类无关（命中的是本分类的商品）',
    search.json?.data?.some((g) => g.id === gidA))

  const byName = await call('GET', '/user/goods/search', { userToken: token, query: { name: '商品甲' } })
  eq('精确一点的关键词 → 1 条', byName.json?.data?.length, 1)

  const emptyKw = await call('GET', '/user/goods/search', { userToken: token, query: { name: '' } })
  eq('空关键词 → 空数组（不返回全量）', emptyKw.json?.data?.length, 0)
  const blankKw = await call('GET', '/user/goods/search', { userToken: token, query: { name: '   ' } })
  eq('全空格关键词 → 空数组', blankKw.json?.data?.length, 0)
  const noKw = await call('GET', '/user/goods/search', { userToken: token })
  eq('不传 name → 空数组', noKw.json?.data?.length, 0)

  await call('POST', '/admin/goods/status/0', { token: admin, query: { id: gidA } })
  const searchOff = await call('GET', '/user/goods/search', { userToken: token, query: { name: kw } })
  ok('下架商品不出现在搜索结果里', !searchOff.json?.data?.some((g) => g.id === gidA))
  await call('POST', '/admin/goods/status/1', { token: admin, query: { id: gidA } })

  /* ---------------- 组合箱 ---------------- */
  section('6.1 / 6.2 组合箱浏览')

  const bundleNoToken = await call('GET', '/user/bundle/list', { query: { categoryId: catBundle } })
  eq('不带令牌 → code = 0', bundleNoToken.json?.code, 0)

  const addBundle = await call('POST', '/admin/bundle', {
    token: admin,
    body: {
      name: BUNDLE,
      categoryId: catBundle,
      price: 29.9,
      bundleGoods: [
        { goodsId: gidA, copies: 2 },
        { goodsId: gidB, copies: 1 }
      ]
    }
  })
  eq('建组合箱', addBundle.json?.code, 1)
  const bundleId = Number(sql(`SELECT id FROM bundle WHERE name = '${BUNDLE}'`))

  redis('del', `xh:bundle:category:${catBundle}`)
  const bundleList = await call('GET', '/user/bundle/list', { userToken: token, query: { categoryId: catBundle } })
  eq('未起售的组合箱不返回（只返回起售中）', bundleList.json?.data?.length, 0)

  await call('POST', '/admin/bundle/status/1', { token: admin, query: { id: bundleId } })
  const afterSale = await call('GET', '/user/bundle/list', { userToken: token, query: { categoryId: catBundle } })
  eq('起售后返回 1 条', afterSale.json?.data?.length, 1)
  const bundleVO = afterSale.json?.data?.[0]
  eq('带出名称与售价', bundleVO?.name + '/' + bundleVO?.price, `${BUNDLE}/29.9`)
  eq('列表已带内含商品', bundleVO?.bundleGoods?.length, 2)
  eq('内含商品份数正确', bundleVO?.bundleGoods?.[0]?.copies, 2)
  eq('组合箱列表缓存已写入', redis('exists', `xh:bundle:category:${catBundle}`), '1')

  const bundleGoods = await call('GET', `/user/bundle/goods/${bundleId}`, { userToken: token })
  eq('查询组合箱内商品 code = 1', bundleGoods.json?.code, 1)
  eq('返回 2 项', bundleGoods.json?.data?.length, 2)
  ok('每项带商品 id / 名称 / 单价 / 份数',
    bundleGoods.json?.data?.every((g) => 'goodsId' in g && 'name' in g && 'price' in g && 'copies' in g))

  const ghost = await call('GET', '/user/bundle/goods/999999', { userToken: token })
  eq('不存在的组合箱 → 返回空数组（不是报错）', ghost.json?.data?.length, 0)

  await call('POST', '/admin/bundle/status/0', { token: admin, query: { id: bundleId } })
  const afterStop = await call('GET', '/user/bundle/list', { userToken: token, query: { categoryId: catBundle } })
  eq('停售后立刻不再返回', afterStop.json?.data?.length, 0)

  /* ---------------- 后置清理 ---------------- */
  cleanup()
  console.log('\n（已清理测试数据与缓存键）')
}
