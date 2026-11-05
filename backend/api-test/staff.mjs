/**
 * 店员模块接口测试（《接口文档》2.1，共 9 个接口）。
 *
 * 每个接口都同时断言两件事：HTTP 响应结构与数据库真实状态。
 * 脚本可重复执行：开始与结束都会清掉测试账号。
 *
 * 2026-09-24 契约调整后新增的覆盖点：
 *  · 新增店员默认状态为「禁用」，且禁用状态下无法登录；
 *  · 分页查询支持 姓名 / 用户名 / 手机号 三个模糊条件；
 *  · 删除店员的两项校验（不能删自己、启用中不能删）与物理删除。
 */
import { call, eq, ok, section, sql } from './lib.mjs'

const TEST_USER = 'xhtest'
const TEST_NAME = '测试店员'
const NEW_PASSWORD = 'xh@2026'

export default async function run() {
  /* ---------------- 前置：清理上次残留 ---------------- */
  sql(`DELETE FROM staff WHERE username = '${TEST_USER}'`)

  let token = ''
  let adminId = 0
  let testId = 0

  /* ---------------- 1. 登录 ---------------- */
  section('2.1.1 店员登录  POST /admin/staff/login')

  const loginOk = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  eq('登录成功返回 code = 1', loginOk.json?.code, 1)
  ok('返回 id', typeof loginOk.json?.data?.id === 'number', JSON.stringify(loginOk.json?.data))
  eq('返回 userName（大写 N，契约字段名）', loginOk.json?.data?.userName, 'admin')
  eq('返回姓名', loginOk.json?.data?.name, '沈青禾')
  ok('返回 token 且长度合理', (loginOk.json?.data?.token || '').length > 20,
    `token 长度 ${(loginOk.json?.data?.token || '').length}`)
  ok('响应中不含 password 字段', !('password' in (loginOk.json?.data || {})))
  token = loginOk.json?.data?.token
  adminId = loginOk.json?.data?.id

  const loginBadPwd = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: 'wrong-password' }
  })
  eq('密码错误 → code = 0', loginBadPwd.json?.code, 0)
  eq('密码错误 → msg', loginBadPwd.json?.msg, '密码错误')

  const loginNoUser = await call('POST', '/admin/staff/login', {
    body: { username: 'no-such-staff', password: '123456' }
  })
  eq('账号不存在 → msg', loginNoUser.json?.msg, '账号不存在')

  // 种子数据中 zhouyan 的 status = 0
  const loginLocked = await call('POST', '/admin/staff/login', {
    body: { username: 'zhouyan', password: '123456' }
  })
  eq('账号被禁用 → msg', loginLocked.json?.msg, '账号被锁定')

  /* ---------------- 2. 鉴权 ---------------- */
  section('1.3 鉴权（拦截器行为）')

  const noToken = await call('GET', '/admin/staff/page', { query: { page: 1, pageSize: 10 } })
  eq('不带 token → HTTP 200（而非 401）', noToken.status, 200)
  eq('不带 token → code = 0', noToken.json?.code, 0)
  eq('不带 token → msg', noToken.json?.msg, '登录状态已失效')

  const badToken = await call('GET', '/admin/staff/page', {
    query: { page: 1, pageSize: 10 },
    token: 'not-a-real-token'
  })
  eq('token 非法 → msg', badToken.json?.msg, '登录状态已失效')

  /* ---------------- 3. 分页查询 ---------------- */
  section('2.1.4 店员分页查询  GET /admin/staff/page')

  const page = await call('GET', '/admin/staff/page', {
    token,
    query: { page: 1, pageSize: 10 }
  })
  eq('分页查询 code = 1', page.json?.code, 1)
  const dbTotal = Number(sql('SELECT COUNT(*) FROM staff'))
  eq('total 与库中记录数一致', page.json?.data?.total, dbTotal)
  eq('records 条数', page.json?.data?.records?.length, dbTotal)
  ok('records 元素不含 password', page.json?.data?.records?.every((r) => !('password' in r)))
  ok('records 含 createTime / updateTime',
    page.json?.data?.records?.every((r) => 'createTime' in r && 'updateTime' in r))

  const times = (page.json?.data?.records || []).map((r) => r.createTime)
  const sortedDesc = [...times].sort().reverse()
  ok('按 createTime 倒序', JSON.stringify(times) === JSON.stringify(sortedDesc), times.join(' | '))

  // 三个筛选条件分别验证（契约 2.1.4）
  const byName = await call('GET', '/admin/staff/page', {
    token,
    query: { name: '陈', page: 1, pageSize: 10 }
  })
  eq('姓名模糊查询命中 1 条', byName.json?.data?.total, 1)
  eq('姓名模糊查询结果正确', byName.json?.data?.records?.[0]?.name, '陈续')

  const byUsername = await call('GET', '/admin/staff/page', {
    token,
    query: { username: 'lin', page: 1, pageSize: 10 }
  })
  eq('用户名模糊查询命中 linwan', byUsername.json?.data?.total, 1)
  eq('用户名模糊查询结果正确', byUsername.json?.data?.records?.[0]?.username, 'linwan')

  const byPhone = await call('GET', '/admin/staff/page', {
    token,
    query: { phone: '13800000003', page: 1, pageSize: 10 }
  })
  eq('手机号模糊查询命中 1 条', byPhone.json?.data?.total, 1)
  eq('手机号模糊查询结果正确', byPhone.json?.data?.records?.[0]?.name, '林晚')

  const byCombo = await call('GET', '/admin/staff/page', {
    token,
    query: { name: '林', username: 'lin', phone: '138000000', page: 1, pageSize: 10 }
  })
  eq('三条件同时满足时命中', byCombo.json?.data?.total, 1)
  const byConflict = await call('GET', '/admin/staff/page', {
    token,
    query: { name: '林', username: 'chenxu', page: 1, pageSize: 10 }
  })
  eq('条件互斥时返回 0 条（AND 关系）', byConflict.json?.data?.total, 0)

  const pageSize2 = await call('GET', '/admin/staff/page', {
    token,
    query: { page: 1, pageSize: 2 }
  })
  eq('pageSize = 2 时 records 为 2 条', pageSize2.json?.data?.records?.length, 2)
  eq('pageSize = 2 时 total 仍为全量', pageSize2.json?.data?.total, dbTotal)

  /* ---------------- 4. 新增 ---------------- */
  section('2.1.3 新增店员  POST /admin/staff')

  const addRes = await call('POST', '/admin/staff', {
    token,
    body: {
      name: TEST_NAME,
      username: TEST_USER,
      phone: '13900000009',
      sex: '女',
      idNumber: '110101199505053333'
    }
  })
  eq('新增 code = 1', addRes.json?.code, 1)

  const added = sql(
    `SELECT id, name, username, password, status, create_time, create_user
     FROM staff WHERE username = '${TEST_USER}'`
  ).split('\t')
  testId = Number(added[0])
  ok('数据已落库且 id 有效', testId > 0, `id = ${testId}`)
  eq('落库姓名', added[1], TEST_NAME)
  ok('初始密码按 PBKDF2 存储（非明文、非旧 MD5）', /^pbkdf2\$\d+\$[^$]+\$[^$]+$/.test(added[3]), String(added[3]).slice(0, 40))
  eq('新增后默认状态为禁用', added[4], '0')
  ok('create_time 被自动填充', Boolean(added[5] && added[5] !== 'NULL'), String(added[5]))
  ok('create_user 被自动填充为当前登录店员 id', Number(added[6]) > 0, String(added[6]))

  const loginWhileDisabled = await call('POST', '/admin/staff/login', {
    body: { username: TEST_USER, password: '123456' }
  })
  eq('禁用状态下无法登录', loginWhileDisabled.json?.msg, '账号被锁定')

  const dup = await call('POST', '/admin/staff', {
    token,
    body: {
      name: '重名测试',
      username: TEST_USER,
      phone: '13900000010',
      sex: '男',
      idNumber: '110101199505054444'
    }
  })
  eq('用户名重复 → code = 0', dup.json?.code, 0)
  eq('用户名重复 → msg 形如「xxx已存在」', dup.json?.msg, `${TEST_USER}已存在`)

  /* ---------------- 5. 按 id 查询 ---------------- */
  section('2.1.6 根据 id 查询店员  GET /admin/staff/{id}')

  const byId = await call('GET', `/admin/staff/${testId}`, { token })
  eq('查询 code = 1', byId.json?.code, 1)
  eq('返回用户名', byId.json?.data?.username, TEST_USER)
  ok('不返回 password', !('password' in (byId.json?.data || {})))

  /* ---------------- 6. 编辑 ---------------- */
  section('2.1.7 编辑店员  PUT /admin/staff')

  const updateRes = await call('PUT', '/admin/staff', {
    token,
    body: {
      id: testId,
      name: '测试店员改',
      username: TEST_USER,
      phone: '13900000011',
      sex: '男',
      idNumber: '110101199505055555'
    }
  })
  eq('编辑 code = 1', updateRes.json?.code, 1)

  const afterUpdate = sql(
    `SELECT name, phone, sex, status FROM staff WHERE id = ${testId}`
  ).split('\t')
  eq('姓名已更新', afterUpdate[0], '测试店员改')
  eq('手机号已更新', afterUpdate[1], '13900000011')
  eq('性别已更新', afterUpdate[2], '男')
  eq('编辑不会改动状态（仍为禁用）', afterUpdate[3], '0')

  /* ---------------- 7. 启用 / 禁用 ---------------- */
  section('2.1.5 启用/禁用店员账号  POST /admin/staff/status/{status}')

  const enable = await call('POST', '/admin/staff/status/1', { token, query: { id: testId } })
  eq('启用 code = 1', enable.json?.code, 1)
  eq('库中状态已改为 1', sql(`SELECT status FROM staff WHERE id = ${testId}`), '1')

  const loginAfterEnable = await call('POST', '/admin/staff/login', {
    body: { username: TEST_USER, password: '123456' }
  })
  eq('启用后可用初始密码登录', loginAfterEnable.json?.code, 1)

  /* ---------------- 8. 修改密码 ---------------- */
  section('2.1.8 修改密码  PUT /admin/staff/editPassword')

  const wrongOld = await call('PUT', '/admin/staff/editPassword', {
    token,
    body: { empId: testId, oldPassword: 'not-the-old-one', newPassword: NEW_PASSWORD }
  })
  eq('原密码错误 → code = 0', wrongOld.json?.code, 0)
  eq('原密码错误 → msg', wrongOld.json?.msg, '原密码错误')

  const editPwd = await call('PUT', '/admin/staff/editPassword', {
    token,
    body: { empId: testId, oldPassword: '123456', newPassword: NEW_PASSWORD }
  })
  eq('修改密码 code = 1', editPwd.json?.code, 1)

  const loginNewPwd = await call('POST', '/admin/staff/login', {
    body: { username: TEST_USER, password: NEW_PASSWORD }
  })
  eq('新密码可登录', loginNewPwd.json?.code, 1)

  const loginOldPwd = await call('POST', '/admin/staff/login', {
    body: { username: TEST_USER, password: '123456' }
  })
  eq('旧密码已失效', loginOldPwd.json?.msg, '密码错误')

  /* ---------------- 9. 删除 ---------------- */
  section('2.1.9 删除店员  DELETE /admin/staff')

  // 9.1 启用中不能删
  const delEnabled = await call('DELETE', '/admin/staff', { token, query: { id: testId } })
  eq('启用中的店员 → code = 0', delEnabled.json?.code, 0)
  eq('启用中的店员 → msg', delEnabled.json?.msg, '启用中的店员不能删除')
  eq('该店员未被删除', sql(`SELECT COUNT(*) FROM staff WHERE id = ${testId}`), '1')

  // 9.2 不能删自己（admin 处于启用状态，仍应优先命中「删自己」这条）
  const delSelf = await call('DELETE', '/admin/staff', { token, query: { id: adminId } })
  eq('删除自己 → code = 0', delSelf.json?.code, 0)
  eq('删除自己 → msg', delSelf.json?.msg, '不能删除当前登录账号')
  eq('当前登录账号未被删除', sql(`SELECT COUNT(*) FROM staff WHERE id = ${adminId}`), '1')

  // 9.3 先禁用再删，应成功且为物理删除
  await call('POST', '/admin/staff/status/0', { token, query: { id: testId } })
  eq('已先禁用该店员', sql(`SELECT status FROM staff WHERE id = ${testId}`), '0')

  const delOk = await call('DELETE', '/admin/staff', { token, query: { id: testId } })
  eq('禁用后可删除 code = 1', delOk.json?.code, 1)
  eq('库中记录已被物理删除',
    sql(`SELECT COUNT(*) FROM staff WHERE id = ${testId}`), '0')

  // 9.4 删除不存在的 id
  const delMissing = await call('DELETE', '/admin/staff', { token, query: { id: 999999 } })
  eq('删除不存在的 id → msg', delMissing.json?.msg, '账号不存在')

  /* ---------------- 10. 退出 ---------------- */
  section('2.1.2 店员退出  POST /admin/staff/logout')

  const logout = await call('POST', '/admin/staff/logout', { token })
  eq('退出 code = 1', logout.json?.code, 1)

  /* ---------------- 后置：清理 ---------------- */
  sql(`DELETE FROM staff WHERE username = '${TEST_USER}'`)
  console.log(`\n（已清理测试账号 ${TEST_USER}，店员总数 ${sql('SELECT COUNT(*) FROM staff')}）`)
}
