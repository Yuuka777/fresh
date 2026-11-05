/**
 * 商品分类模块接口测试（《接口文档》2.2，共 6 个接口）。
 *
 * 重点验证三件容易写错的事：
 * 1. 查询类接口不过滤 status（禁用后的分类在管理端必须仍可见）；
 * 2. 删除分类的关联校验（有商品或有组合箱都要拒绝，且库里数据不能被删掉）；
 * 3. 修改分类时不误改 type、不把 status 刷成 null。
 */
import { call, eq, ok, section, sql } from './lib.mjs'

const TEST_NAME = 'xh测试分类'
const TEST_NAME_EDIT = 'xh测试分类改'
const TEST_SORT = 99

/** 登录拿一个管理端令牌 */
async function getToken() {
  const res = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  return res.json?.data?.token
}

export default async function run() {
  const token = await getToken()
  ok('取得管理端令牌', Boolean(token))

  /* ---------------- 前置：清理上次残留 ---------------- */
  sql(`DELETE FROM goods_category WHERE name IN ('${TEST_NAME}', '${TEST_NAME_EDIT}')`)

  /* ---------------- 1. 新增 ---------------- */
  section('2.2.1 新增分类  POST /admin/goodsCategory')

  const addRes = await call('POST', '/admin/goodsCategory', {
    token,
    body: { type: 1, name: TEST_NAME, sort: TEST_SORT }
  })
  eq('新增 code = 1', addRes.json?.code, 1)

  const added = sql(
    `SELECT id, type, name, sort, status, create_user FROM goods_category WHERE name = '${TEST_NAME}'`
  ).split('\t')
  const testId = Number(added[0])
  ok('数据已落库且 id 有效', testId > 0, `id = ${testId}`)
  eq('type 落库正确', added[1], '1')
  eq('sort 落库正确', added[3], String(TEST_SORT))
  eq('新增后默认状态为禁用', added[4], '0')
  ok('create_user 被自动填充', Number(added[5]) > 0, String(added[5]))

  const dup = await call('POST', '/admin/goodsCategory', {
    token,
    body: { type: 1, name: TEST_NAME, sort: 100 }
  })
  eq('分类名重复 → code = 0', dup.json?.code, 0)
  eq('分类名重复 → msg', dup.json?.msg, `${TEST_NAME}已存在`)

  // 契约把 sort 定为必填（库中 NOT NULL 且无默认值），这里确认漏传时的实际表现
  const noSort = await call('POST', '/admin/goodsCategory', {
    token,
    body: { type: 1, name: `${TEST_NAME}-无排序值` }
  })
  ok('漏传 sort 时不会静默写入（返回失败或明确报错）',
    noSort.json?.code !== 1, `code=${noSort.json?.code}，msg=${noSort.json?.msg}`)
  sql(`DELETE FROM goods_category WHERE name = '${TEST_NAME}-无排序值'`)

  /* ---------------- 2. 分页查询 ---------------- */
  section('2.2.2 分类分页查询  GET /admin/goodsCategory/page')

  const dbTotal = Number(sql('SELECT COUNT(*) FROM goods_category'))

  const page = await call('GET', '/admin/goodsCategory/page', {
    token,
    query: { page: 1, pageSize: 20 }
  })
  eq('分页 code = 1', page.json?.code, 1)
  eq('total 与库中记录数一致', page.json?.data?.total, dbTotal)
  eq('records 条数', page.json?.data?.records?.length, dbTotal)
  ok('records 含 type / sort / status / createTime',
    page.json?.data?.records?.every((r) =>
      'type' in r && 'sort' in r && 'status' in r && 'createTime' in r))

  // sort 升序
  const sorts = (page.json?.data?.records || []).map((r) => r.sort)
  const asc = [...sorts].sort((a, b) => a - b)
  ok('按 sort 升序', JSON.stringify(sorts) === JSON.stringify(asc), sorts.join(','))

  const byName = await call('GET', '/admin/goodsCategory/page', {
    token,
    query: { name: TEST_NAME, page: 1, pageSize: 20 }
  })
  eq('名称模糊查询命中', byName.json?.data?.total, 1)

  const byType = await call('GET', '/admin/goodsCategory/page', {
    token,
    query: { type: 2, page: 1, pageSize: 20 }
  })
  const type2Db = Number(sql('SELECT COUNT(*) FROM goods_category WHERE type = 2'))
  eq('按 type = 2 筛选，total 与库一致', byType.json?.data?.total, type2Db)
  ok('筛选结果全部为 type = 2', byType.json?.data?.records?.every((r) => r.type === 2))

  /* ---------------- 3. 按类型查列表（表单下拉框用） ---------------- */
  section('2.2.6 按类型查询分类列表  GET /admin/goodsCategory/list')

  const list1 = await call('GET', '/admin/goodsCategory/list', { token, query: { type: 1 } })
  eq('type = 1 列表 code = 1', list1.json?.code, 1)
  const list1Db = Number(sql('SELECT COUNT(*) FROM goods_category WHERE type = 1'))
  eq('type = 1 返回条数与库一致', list1.json?.data?.length, list1Db)
  ok('列表元素含 status 字段（前端需据此标注禁用项）',
    list1.json?.data?.every((c) => 'status' in c))

  const listSorts = (list1.json?.data || []).map((c) => c.sort)
  ok('列表按 sort 升序',
    JSON.stringify(listSorts) === JSON.stringify([...listSorts].sort((a, b) => a - b)),
    listSorts.join(','))

  const listAll = await call('GET', '/admin/goodsCategory/list', { token })
  eq('不传 type 时返回全部分类', listAll.json?.data?.length, dbTotal)

  /* ---------------- 4. 启用 / 禁用 ---------------- */
  section('2.2.5 启用/禁用分类  POST /admin/goodsCategory/status/{status}')

  const disable = await call('POST', '/admin/goodsCategory/status/0', {
    token,
    query: { id: testId }
  })
  eq('禁用 code = 1', disable.json?.code, 1)
  eq('库中状态已改为 0', sql(`SELECT status FROM goods_category WHERE id = ${testId}`), '0')

  // 关键：管理端禁用后必须仍能查到，否则无法恢复
  const pageAfterDisable = await call('GET', '/admin/goodsCategory/page', {
    token,
    query: { page: 1, pageSize: 20 }
  })
  eq('禁用后分页仍能查到该分类（不过滤 status）', pageAfterDisable.json?.data?.total, dbTotal)
  ok('禁用中的分类出现在分页结果里',
    pageAfterDisable.json?.data?.records?.some((r) => r.id === testId))

  const listAfterDisable = await call('GET', '/admin/goodsCategory/list', {
    token,
    query: { type: 1 }
  })
  ok('禁用中的分类出现在下拉列表里（不过滤 status）',
    listAfterDisable.json?.data?.some((c) => c.id === testId && c.status === 0))

  const enable = await call('POST', '/admin/goodsCategory/status/1', {
    token,
    query: { id: testId }
  })
  eq('启用 code = 1', enable.json?.code, 1)
  eq('库中状态已改回 1', sql(`SELECT status FROM goods_category WHERE id = ${testId}`), '1')

  /* ---------------- 5. 修改 ---------------- */
  section('2.2.4 修改分类  PUT /admin/goodsCategory')

  const updateRes = await call('PUT', '/admin/goodsCategory', {
    token,
    body: { id: testId, name: TEST_NAME_EDIT, sort: 88 }
  })
  eq('修改 code = 1', updateRes.json?.code, 1)

  const afterUpdate = sql(
    `SELECT name, sort, type, status FROM goods_category WHERE id = ${testId}`
  ).split('\t')
  eq('名称已更新', afterUpdate[0], TEST_NAME_EDIT)
  eq('排序值已更新', afterUpdate[1], '88')
  eq('type 未被修改', afterUpdate[2], '1')
  eq('status 未被覆盖为 NULL', afterUpdate[3], '1')

  /* ---------------- 6. 删除 ---------------- */
  section('2.2.3 删除分类  DELETE /admin/goodsCategory')

  // 6.1 有关联商品的分类不能删（动态取一条，避免硬编码 id）
  const catWithGoods = Number(
    sql(`SELECT c.id FROM goods_category c
         WHERE EXISTS (SELECT 1 FROM goods g WHERE g.category_id = c.id) LIMIT 1`)
  )
  const delGoods = await call('DELETE', '/admin/goodsCategory', {
    token,
    query: { id: catWithGoods }
  })
  eq('分类下有商品 → code = 0', delGoods.json?.code, 0)
  eq('分类下有商品 → msg', delGoods.json?.msg, '分类下关联商品，无法删除')
  eq('该分类未被真正删除',
    sql(`SELECT COUNT(*) FROM goods_category WHERE id = ${catWithGoods}`), '1')

  // 6.2 有关联组合箱的分类不能删
  const catWithBundle = Number(
    sql(`SELECT c.id FROM goods_category c
         WHERE EXISTS (SELECT 1 FROM bundle b WHERE b.category_id = c.id) LIMIT 1`)
  )
  ok('种子里存在挂着组合箱的分类', catWithBundle > 0, `id = ${catWithBundle}`)
  const delBundle = await call('DELETE', '/admin/goodsCategory', {
    token,
    query: { id: catWithBundle }
  })
  eq('分类下有组合箱 → msg 同为「分类下关联商品，无法删除」',
    delBundle.json?.msg, '分类下关联商品，无法删除')
  eq('该分类未被真正删除',
    sql(`SELECT COUNT(*) FROM goods_category WHERE id = ${catWithBundle}`), '1')

  // 6.3 无关联的分类可以正常删除
  const delOk = await call('DELETE', '/admin/goodsCategory', {
    token,
    query: { id: testId }
  })
  eq('无关联的分类可删除', delOk.json?.code, 1)
  eq('库中记录已删除',
    sql(`SELECT COUNT(*) FROM goods_category WHERE id = ${testId}`), '0')

  /* ---------------- 后置：清理 ---------------- */
  sql(`DELETE FROM goods_category WHERE name LIKE '${TEST_NAME}%'`)
  console.log(`\n（已清理测试分类，分类总数恢复为 ${sql('SELECT COUNT(*) FROM goods_category')}）`)
}
