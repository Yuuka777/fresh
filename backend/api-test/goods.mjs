/**
 * 商品模块接口测试（《接口文档》2.6，共 7 个接口）。
 *
 * 除 HTTP 响应外，每个用例都直接查库核对：
 * 1. 新增后状态必须是「下架」、规格是否按 JSON 文本落库；
 * 2. 列表不含 specs、详情才带 specs；
 * 3. 修改是整体替换语义（规格全量覆盖、可空字段传 null 即清空、status 不被改动）；
 * 4. 两条状态规则的边界：「上架中 ⇒ 所属分类启用」「下架 ⇒ 不被起售中的组合箱引用」；
 * 5. 删除的三道校验与连带清理（goods / goods_spec / cart）。
 */
import { execFileSync } from 'node:child_process'
import { call, eq, ok, section, sql } from './lib.mjs'

const CAT_NAME = 'xh联调商品分类'
const CAT_NAME_B = 'xh联调商品分类乙'
const GOODS_A = 'xh联调商品甲'
const GOODS_B = 'xh联调商品乙'
const BADGE = 'xh联调挂件箱' // 用于「被组合箱引用」的夹具组合箱

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

/** 取商品一行的关键字段 */
function goodsRow(name) {
  return sql(
    `SELECT id, category_id, price, IFNULL(image,'<null>'), status, storage_zone
     FROM goods WHERE name = '${name}'`
  ).split('\t')
}

/** 清理本模块可能留下的数据（按名称前缀，避免碰到种子数据） */
function cleanup() {
  sql(`DELETE FROM bundle WHERE name = '${BADGE}'`)
  sql(`DELETE FROM cart WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调商品%')`)
  sql(`DELETE FROM cart WHERE bundle_id IN (SELECT id FROM bundle WHERE name LIKE 'xh联调%')`)
  sql(`DELETE FROM goods_spec WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调商品%')`)
  sql(`DELETE FROM bundle_goods WHERE goods_id IN (SELECT id FROM goods WHERE name LIKE 'xh联调商品%')`)
  sql(`DELETE FROM goods WHERE name LIKE 'xh联调商品%'`)
  sql(`DELETE FROM goods_category WHERE name LIKE 'xh联调商品分类%'`)
}

export default async function run() {
  const token = await getToken()
  ok('取得管理端令牌', Boolean(token))

  cleanup()

  /* ---------------- 前置：建两个商品分类（一启用一禁用） ---------------- */
  section('前置：建测试分类')

  for (const name of [CAT_NAME, CAT_NAME_B]) {
    const res = await call('POST', '/admin/goodsCategory', {
      token,
      body: { type: 1, name, sort: 91 }
    })
    eq(`建分类「${name}」`, res.json?.code, 1)
  }
  const catA = Number(sql(`SELECT id FROM goods_category WHERE name = '${CAT_NAME}'`))
  const catB = Number(sql(`SELECT id FROM goods_category WHERE name = '${CAT_NAME_B}'`))
  eq('新分类初始状态为禁用', sql(`SELECT status FROM goods_category WHERE id = ${catA}`), '0')

  // 本模块的下架/删除用例都要求分类可用，这里先启用
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catA } })
  eq('分类甲已启用', sql(`SELECT status FROM goods_category WHERE id = ${catA}`), '1')
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catB } })
  eq('分类乙已启用', sql(`SELECT status FROM goods_category WHERE id = ${catB}`), '1')

  /* ---------------- 1. 新增 ---------------- */
  section('2.6.1 新增商品  POST /admin/goods')

  const addRes = await call('POST', '/admin/goods', {
    token,
    body: {
      name: GOODS_A,
      categoryId: catA,
      price: 9.9,
      description: '产地直采，当日采摘当日达',
      origin: '山东寿光',
      shelfLife: '冷藏 5 天',
      storageZone: 2,
      specs: [
        { name: '净重', value: ['约500g', '约1kg'] },
        { name: '包装', value: ['散装', '礼盒装'] }
      ]
    }
  })
  eq('新增 code = 1', addRes.json?.code, 1)

  const rowA = goodsRow(GOODS_A)
  const idA = Number(rowA[0])
  ok('数据已落库且 id 有效', idA > 0, `id = ${idA}`)
  eq('分类落库正确', rowA[1], String(catA))
  eq('金额按 decimal 落库', rowA[2], '9.90')
  eq('未传图片时 image 为 NULL', rowA[3], '<null>')
  eq('新增后状态为「下架」', rowA[4], '0')
  eq('温区落库正确', rowA[5], '2')
  ok('公共字段被自动填充',
    Number(sql(`SELECT create_user FROM goods WHERE id = ${idA}`)) > 0 &&
    sql(`SELECT create_time FROM goods WHERE id = ${idA}`) !== 'NULL')

  const specRows = sql(
    `SELECT name, \`value\` FROM goods_spec WHERE goods_id = ${idA} ORDER BY id`
  ).split('\n').map((l) => l.split('\t'))
  eq('规格落库 2 组', specRows.length, 2)
  eq('规格名称落库正确', specRows[0][0], '净重')
  eq('候选值以 JSON 文本落库', specRows[0][1], '["约500g","约1kg"]')
  eq('第二组候选值落库正确', specRows[1][1], '["散装","礼盒装"]')

  const dup = await call('POST', '/admin/goods', {
    token,
    body: { name: GOODS_A, categoryId: catA, price: 1, storageZone: 1 }
  })
  eq('商品名重复 → code = 0', dup.json?.code, 0)
  eq('商品名重复 → msg', dup.json?.msg, `${GOODS_A}已存在`)

  const badCat = await call('POST', '/admin/goods', {
    token,
    body: { name: `${GOODS_B}临时`, categoryId: 999999, price: 1, storageZone: 1 }
  })
  eq('分类不存在 → msg', badCat.json?.msg, '分类不存在')

  const wrongType = await call('POST', '/admin/goods', {
    token,
    body: { name: `${GOODS_B}临时`, categoryId: 9, price: 1, storageZone: 1 }
  })
  eq('挂到组合分类（type = 2）→ msg', wrongType.json?.msg, '分类类型不匹配')

  const emptySpecName = await call('POST', '/admin/goods', {
    token,
    body: {
      name: `${GOODS_B}临时`,
      categoryId: catA,
      price: 1,
      storageZone: 1,
      specs: [{ name: '  ', value: ['约500g'] }]
    }
  })
  eq('规格名称为空 → msg', emptySpecName.json?.msg, '商品规格的名称与候选值不能为空')
  eq('校验失败时商品未被写入',
    sql(`SELECT COUNT(*) FROM goods WHERE name = '${GOODS_B}临时'`), '0')

  // 空白候选值应被丢弃，只剩有效值
  const addB = await call('POST', '/admin/goods', {
    token,
    body: {
      name: GOODS_B,
      categoryId: catA,
      price: 12.5,
      storageZone: 1,
      specs: [{ name: ' 等级 ', value: [' 特级 ', '  ', ''] }]
    }
  })
  eq('新增第二个商品 code = 1', addB.json?.code, 1)
  const idB = Number(sql(`SELECT id FROM goods WHERE name = '${GOODS_B}'`))
  eq('空白候选值被丢弃、有效值被 trim',
    sql(`SELECT \`value\` FROM goods_spec WHERE goods_id = ${idB}`), '["特级"]')
  eq('规格名称被 trim',
    sql(`SELECT name FROM goods_spec WHERE goods_id = ${idB}`), '等级')

  /* ---------------- 2. 分页查询 ---------------- */
  section('2.6.2 商品分页查询  GET /admin/goods/page')

  const dbTotal = Number(sql('SELECT COUNT(*) FROM goods'))
  const page = await call('GET', '/admin/goods/page', {
    token,
    query: { page: 1, pageSize: 200 }
  })
  eq('分页 code = 1', page.json?.code, 1)
  eq('total 与库中记录数一致', page.json?.data?.total, dbTotal)
  ok('records 含列表需要的字段',
    page.json?.data?.records?.every(
      (r) => 'id' in r && 'categoryId' in r && 'price' in r && 'status' in r && 'storageZone' in r && 'updateTime' in r
    ))
  const listed = page.json?.data?.records?.find((r) => r.id === idA)
  eq('联表带出分类名称', listed?.categoryName, CAT_NAME)
  ok('列表不返回 specs', listed && !('specs' in listed), JSON.stringify(Object.keys(listed || {})))

  const byName = await call('GET', '/admin/goods/page', {
    token,
    query: { name: '联调商品', page: 1, pageSize: 200 }
  })
  eq('名称模糊查询命中 2 条', byName.json?.data?.total, 2)

  const byCat = await call('GET', '/admin/goods/page', {
    token,
    query: { categoryId: catB, page: 1, pageSize: 200 }
  })
  eq('按分类筛选（该分类下暂无商品）', byCat.json?.data?.total, 0)

  const byStatus = await call('GET', '/admin/goods/page', {
    token,
    query: { status: 0, page: 1, pageSize: 200 }
  })
  const offShelfDb = Number(sql('SELECT COUNT(*) FROM goods WHERE status = 0'))
  eq('按状态筛选 total 与库一致', byStatus.json?.data?.total, offShelfDb)
  ok('筛选结果全部为下架',
    byStatus.json?.data?.records?.every((r) => r.status === 0))

  const combo = await call('GET', '/admin/goods/page', {
    token,
    query: { name: '联调商品甲', categoryId: catA, status: 0, page: 1, pageSize: 200 }
  })
  eq('三条件 AND 组合命中 1 条', combo.json?.data?.total, 1)

  const times = (page.json?.data?.records || []).map((r) => r.updateTime)
  ok('按 update_time 倒序',
    JSON.stringify(times) === JSON.stringify([...times].sort().reverse()), times.slice(0, 3).join(' | '))

  /* ---------------- 3. 详情 ---------------- */
  section('2.6.4 根据 id 查询商品  GET /admin/goods/{id}')

  const detail = await call('GET', `/admin/goods/${idA}`, { token })
  eq('详情 code = 1', detail.json?.code, 1)
  eq('详情带出分类名称', detail.json?.data?.categoryName, CAT_NAME)
  eq('详情返回规格 2 组', detail.json?.data?.specs?.length, 2)
  ok('规格的 value 是数组（JSON 已还原）',
    Array.isArray(detail.json?.data?.specs?.[0]?.value),
    JSON.stringify(detail.json?.data?.specs?.[0]?.value))
  eq('候选值内容正确',
    JSON.stringify(detail.json?.data?.specs?.[0]?.value), '["约500g","约1kg"]')
  ok('规格带 id（修改时可回填）', Number(detail.json?.data?.specs?.[0]?.id) > 0)

  const noGoods = await call('GET', '/admin/goods/999999', { token })
  eq('商品不存在 → code = 0', noGoods.json?.code, 0)
  eq('商品不存在 → msg', noGoods.json?.msg, '商品不存在')

  /* ---------------- 4. 修改 ---------------- */
  section('2.6.5 修改商品  PUT /admin/goods')

  // 4.1 改名称/价格/规格：规格应全量覆盖（旧的两组消失）
  const upd = await call('PUT', '/admin/goods', {
    token,
    body: {
      id: idA,
      name: GOODS_A,
      categoryId: catA,
      price: 19.9,
      image: 'https://example.com/xh-a.png',
      description: '改后的描述',
      origin: '云南昆明',
      shelfLife: '常温 7 天',
      storageZone: 3,
      status: 1,
      specs: [{ name: '规格', value: ['单份'] }]
    }
  })
  eq('修改 code = 1', upd.json?.code, 1)

  const afterUpd = goodsRow(GOODS_A)
  eq('价格已更新', afterUpd[2], '19.90')
  eq('温区已更新', afterUpd[5], '3')
  eq('图片已写入', afterUpd[3], 'https://example.com/xh-a.png')
  eq('提交 status 被忽略（状态不受本接口影响）', afterUpd[4], '0')
  eq('规格全量覆盖后只剩 1 组',
    sql(`SELECT COUNT(*) FROM goods_spec WHERE goods_id = ${idA}`), '1')
  eq('覆盖后的规格内容正确',
    sql(`SELECT \`value\` FROM goods_spec WHERE goods_id = ${idA}`), '["单份"]')

  // 4.2 动态更新：未提交的字段保持原值；空串表示清空（前端「移除图片」走的就是空串）
  const untouched = await call('PUT', '/admin/goods', {
    token,
    body: { id: idA, name: GOODS_A, categoryId: catA, price: 19.9, storageZone: 3, specs: [] }
  })
  eq('未提交可空字段 code = 1', untouched.json?.code, 1)
  eq('未提交 image → 保持原值', goodsRow(GOODS_A)[3], 'https://example.com/xh-a.png')
  eq('未提交 origin → 保持原值', sql(`SELECT origin FROM goods WHERE id = ${idA}`), '云南昆明')
  eq('specs 传空数组会清空规格（规格是全量覆盖，与单字段语义不同）',
    sql(`SELECT COUNT(*) FROM goods_spec WHERE goods_id = ${idA}`), '0')

  const clearImg = await call('PUT', '/admin/goods', {
    token,
    body: { id: idA, name: GOODS_A, categoryId: catA, price: 19.9, storageZone: 3, image: '' }
  })
  eq('image 传空串 code = 1', clearImg.json?.code, 1)
  eq('image 传空串 → 已清空', goodsRow(GOODS_A)[3], '')

  // 4.3 上架中的商品不能改到已禁用的分类
  await call('POST', '/admin/goodsCategory/status/0', { token, query: { id: catB } })
  await call('POST', '/admin/goods/status/1', { token, query: { id: idA } })
  eq('商品甲已上架', goodsRow(GOODS_A)[4], '1')

  const moveToDisabled = await call('PUT', '/admin/goods', {
    token,
    body: { id: idA, name: GOODS_A, categoryId: catB, price: 19.9, storageZone: 3, specs: [] }
  })
  eq('上架中改到禁用分类 → code = 0', moveToDisabled.json?.code, 0)
  eq('上架中改到禁用分类 → msg', moveToDisabled.json?.msg, '所属分类已禁用，无法上架')
  eq('改动被整体回滚（分类未变）', goodsRow(GOODS_A)[1], String(catA))

  // 4.4 下架，供后续用例使用
  await call('POST', '/admin/goods/status/0', { token, query: { id: idA } })
  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catB } })

  const noId = await call('PUT', '/admin/goods', {
    token,
    body: { name: GOODS_A, categoryId: catA, price: 1, storageZone: 1 }
  })
  eq('漏传 id → msg', noId.json?.msg, '商品不存在')

  /* ---------------- 5. 按分类查列表 ---------------- */
  section('2.6.6 按分类查询商品列表  GET /admin/goods/list')

  const listAll = await call('GET', '/admin/goods/list', { token })
  eq('不传 categoryId 返回全部商品',
    listAll.json?.data?.length, Number(sql('SELECT COUNT(*) FROM goods')))

  const listCat = await call('GET', '/admin/goods/list', { token, query: { categoryId: catA } })
  const catDb = Number(sql(`SELECT COUNT(*) FROM goods WHERE category_id = ${catA}`))
  eq('按分类返回条数与库一致', listCat.json?.data?.length, catDb)
  ok('不过滤 status（下架商品也在列表里）',
    listCat.json?.data?.some((g) => g.id === idA && g.status === 0))
  ok('列表元素是商品实体（带 storageZone，供弹层标注温区）',
    listCat.json?.data?.every((g) => 'storageZone' in g && 'status' in g))

  /* ---------------- 6. 上架 / 下架 ---------------- */
  section('2.6.7 商品上架/下架  POST /admin/goods/status/{status}')

  // 6.1 分类禁用时不允许上架
  await call('POST', '/admin/goodsCategory/status/0', { token, query: { id: catA } })
  const enableWithDisabledCat = await call('POST', '/admin/goods/status/1', {
    token,
    query: { id: idA }
  })
  eq('分类禁用时上架 → code = 0', enableWithDisabledCat.json?.code, 0)
  eq('分类禁用时上架 → msg', enableWithDisabledCat.json?.msg, '所属分类已禁用，无法上架')
  eq('状态未被改动', goodsRow(GOODS_A)[4], '0')

  await call('POST', '/admin/goodsCategory/status/1', { token, query: { id: catA } })

  // 6.2 上架成功并精准清除该分类下的缓存
  const cacheKey = `xh:goods:category:${catA}`
  const otherKey = `xh:goods:category:${catB}`
  redis('set', cacheKey, 'stale')
  redis('set', otherKey, 'keep')
  const enable = await call('POST', '/admin/goods/status/1', { token, query: { id: idA } })
  eq('上架 code = 1', enable.json?.code, 1)
  eq('库中状态为 1', goodsRow(GOODS_A)[4], '1')
  eq(`精准清除了 ${cacheKey}`, redis('exists', cacheKey), '0')
  eq('其它分类的缓存未被波及（只清该商品所属分类）', redis('exists', otherKey), '1')
  redis('del', otherKey)

  // 6.3 被起售中的组合箱引用时不能下架
  sql(`INSERT INTO bundle (category_id, name, price, status, description, storage_zone)
       VALUES (9, '${BADGE}', 1.00, 1, '联调夹具', 2)`)
  const badgeId = Number(sql(`SELECT id FROM bundle WHERE name = '${BADGE}'`))
  sql(`INSERT INTO bundle_goods (bundle_id, goods_id, name, price, copies)
       VALUES (${badgeId}, ${idA}, '${GOODS_A}', 19.90, 1)`)

  const disableInSale = await call('POST', '/admin/goods/status/0', {
    token,
    query: { id: idA }
  })
  eq('被起售中组合箱引用时下架 → code = 0', disableInSale.json?.code, 0)
  eq('被起售中组合箱引用时下架 → msg',
    disableInSale.json?.msg, '商品被起售中的组合箱引用，无法下架')
  eq('状态仍为上架', goodsRow(GOODS_A)[4], '1')

  // 6.4 组合箱停售后即可下架
  sql(`UPDATE bundle SET status = 0 WHERE id = ${badgeId}`)
  const disable = await call('POST', '/admin/goods/status/0', { token, query: { id: idA } })
  eq('组合箱停售后可下架', disable.json?.code, 1)
  eq('库中状态为 0', goodsRow(GOODS_A)[4], '0')

  const notFound = await call('POST', '/admin/goods/status/1', { token, query: { id: 999999 } })
  eq('商品不存在 → msg', notFound.json?.msg, '商品不存在')

  /* ---------------- 7. 批量删除 ---------------- */
  section('2.6.3 批量删除商品  DELETE /admin/goods')

  // 7.1 上架中的不能删
  await call('POST', '/admin/goods/status/1', { token, query: { id: idA } })
  const delOnSale = await call('DELETE', '/admin/goods', { token, query: { ids: String(idA) } })
  eq('上架中 → code = 0', delOnSale.json?.code, 0)
  eq('上架中 → msg', delOnSale.json?.msg, '商品处于上架状态，不能删除')
  eq('商品未被删除', sql(`SELECT COUNT(*) FROM goods WHERE id = ${idA}`), '1')
  await call('POST', '/admin/goods/status/0', { token, query: { id: idA } })

  // 7.2 被组合箱引用的不能删（夹具组合箱此时是停售，仍应拒绝）
  const delRelated = await call('DELETE', '/admin/goods', { token, query: { ids: String(idA) } })
  eq('被组合箱引用（停售中）→ code = 0', delRelated.json?.code, 0)
  eq('被组合箱引用 → msg', delRelated.json?.msg, '商品关联组合箱，不能删除')
  eq('商品仍未被删除', sql(`SELECT COUNT(*) FROM goods WHERE id = ${idA}`), '1')

  // 7.3 空 ids 不报错也不执行 SQL
  const emptyIds = await call('DELETE', '/admin/goods', { token, query: { ids: '' } })
  eq('ids 为空 → code = 1（幂等）', emptyIds.json?.code, 1)

  // 7.4 正常删除：goods / goods_spec / cart 一并清理
  sql(`DELETE FROM bundle_goods WHERE bundle_id = ${badgeId}`)
  sql(`DELETE FROM bundle WHERE id = ${badgeId}`)
  sql(`INSERT INTO cart (name, member_id, goods_id, number, amount)
       VALUES ('${GOODS_B}', 1, ${idB}, 1, 12.50)`)
  eq('夹具购物车行已写入', sql(`SELECT COUNT(*) FROM cart WHERE goods_id = ${idB}`), '1')

  const delOk = await call('DELETE', '/admin/goods', { token, query: { ids: String(idB) } })
  eq('可删除商品', delOk.json?.code, 1)
  eq('goods 记录已删除', sql(`SELECT COUNT(*) FROM goods WHERE id = ${idB}`), '0')
  eq('goods_spec 已连带删除', sql(`SELECT COUNT(*) FROM goods_spec WHERE goods_id = ${idB}`), '0')
  eq('购物车中的对应行已连带删除', sql(`SELECT COUNT(*) FROM cart WHERE goods_id = ${idB}`), '0')

  /* ---------------- 后置：清理 ---------------- */
  cleanup()
  redis('del', `xh:goods:category:${catA}`)
  console.log(
    `\n（已清理测试数据，商品 ${sql('SELECT COUNT(*) FROM goods')} 个、分类 ${sql(
      'SELECT COUNT(*) FROM goods_category'
    )} 个）`
  )
}
