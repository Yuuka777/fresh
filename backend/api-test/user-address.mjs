/**
 * 用户端收货地址测试（《接口文档-用户端》8）。
 *
 * 重点：
 *   1. 归属隔离 —— 别人的地址查不到、改不了、删不了、也设不了默认（参照工程这三个接口都不校验归属）；
 *   2. 默认地址互斥 —— 同一会员至多一条 is_default = 1；
 *   3. 第一条自动默认 —— 新会员的第一条地址自动成为默认；
 *   4. 动态更新 —— 只提交要改的字段，其余保持原值；空串表示清空。
 * 用两个非种子会员 id（9994 / 9995）造数据，结束时清理。
 */
import { call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const ME = 9994
const OTHER = 9995

let me
let other

const countOf = (memberId) => sql(`SELECT COUNT(*) FROM address WHERE member_id = ${memberId}`)

/** 某条地址的关键字段：[收货人, 性别, 手机号, 省, 详址, 标签, 是否默认] */
const rowOf = (id) =>
  sql(`SELECT consignee, sex, phone, IFNULL(province_name,'<null>'), IFNULL(detail,'<null>'),
              IFNULL(label,'<null>'), is_default
       FROM address WHERE id = ${id}`).split('\t')

/** 某会员的全部地址（按 id 升序）：「id 收货人 是否默认」 */
const rowsOf = (memberId) =>
  sql(`SELECT id, consignee, is_default FROM address WHERE member_id = ${memberId} ORDER BY id`)
    .split('\n')
    .filter(Boolean)

const defaultIdsOf = (memberId) =>
  sql(`SELECT id FROM address WHERE member_id = ${memberId} AND is_default = 1
       ORDER BY id`).split('\n').filter(Boolean)

/** 完整地址体，供多项复用 */
const body = (over = {}) => ({
  consignee: '联调收货人',
  sex: '男',
  phone: '13800001111',
  provinceCode: '110000',
  provinceName: '北京市',
  cityCode: '110100',
  cityName: '市辖区',
  districtCode: '110105',
  districtName: '朝阳区',
  detail: '联调路 1 号院 2 号楼 303',
  label: '家',
  ...over
})

function cleanup() {
  sql(`DELETE FROM address WHERE member_id IN (${ME}, ${OTHER})`)
}

export default async function run() {
  me = mintUserToken(ME)
  other = mintUserToken(OTHER)
  ok('自签两个会员的令牌', Boolean(me) && Boolean(other))

  cleanup()
  eq('前置：两个测试会员都没有地址', `${countOf(ME)}/${countOf(OTHER)}`, '0/0')

  /* ---------------- 鉴权 ---------------- */
  section('8.0 鉴权')

  const noToken = await call('GET', '/user/address/list')
  eq('不带令牌 → 登录状态已失效', noToken.json?.msg, '登录状态已失效')

  /* ---------------- 8.1 列表 ---------------- */
  section('8.1 查询地址列表  GET /user/address/list')

  const empty = await call('GET', '/user/address/list', { userToken: me })
  eq('code = 1', empty.json?.code, 1)
  eq('新会员列表为空数组', empty.json?.data?.length, 0)

  /* ---------------- 8.2 新增 ---------------- */
  section('8.2 新增地址  POST /user/address')

  const first = await call('POST', '/user/address', {
    userToken: me,
    body: body({ consignee: '甲一', isDefault: 0 })
  })
  eq('第一条新增成功', first.json?.code, 1)
  const id1 = Number(sql(`SELECT id FROM address WHERE member_id = ${ME} ORDER BY id LIMIT 1`))
  eq('即使提交 isDefault = 0，第一条也自动成为默认', rowOf(id1)[6], '1')
  eq('memberId 取自令牌而非请求体', sql(`SELECT member_id FROM address WHERE id = ${id1}`), String(ME))

  const second = await call('POST', '/user/address', {
    userToken: me,
    body: body({ consignee: '乙二', isDefault: 0 })
  })
  eq('第二条新增成功', second.json?.code, 1)
  const id2 = Number(sql(`SELECT id FROM address WHERE member_id = ${ME} AND consignee = '乙二'`))
  eq('第二条不是默认', rowOf(id2)[6], '0')
  eq('默认仍只有第一条', defaultIdsOf(ME).join(','), String(id1))

  const third = await call('POST', '/user/address', {
    userToken: me,
    body: body({ consignee: '丙三', isDefault: 1, label: '公司' })
  })
  eq('第三条（带默认）新增成功', third.json?.code, 1)
  const id3 = Number(sql(`SELECT id FROM address WHERE member_id = ${ME} AND consignee = '丙三'`))
  eq('新增带默认时，只有它自己是默认', defaultIdsOf(ME).join(','), String(id3))
  eq('原先的默认被清掉', rowOf(id1)[6], '0')
  eq('省市区与标签都落库了',
    `${rowOf(id3)[3]}/${rowOf(id3)[5]}`, '北京市/公司')

  const list = await call('GET', '/user/address/list', { userToken: me })
  eq('列表 3 条', list.json?.data?.length, 3)
  eq('默认地址排在最前', list.json?.data?.[0]?.id, id3)

  /* ---------------- 必填校验 ---------------- */
  section('8.2 必填项校验')

  const noConsignee = await call('POST', '/user/address', {
    userToken: me,
    body: body({ consignee: '  ' })
  })
  eq('收货人为空 → 收货人不能为空', noConsignee.json?.msg, '收货人不能为空')

  const badPhone = await call('POST', '/user/address', {
    userToken: me,
    body: body({ phone: '1380000111' })
  })
  eq('手机号不是 11 位 → 手机号格式不正确', badPhone.json?.msg, '手机号格式不正确')

  const noDetail = await call('POST', '/user/address', {
    userToken: me,
    body: body({ detail: '' })
  })
  eq('详细地址为空 → 详细地址不能为空', noDetail.json?.msg, '详细地址不能为空')
  eq('三次校验失败都没有写库', countOf(ME), '3')

  /* ---------------- 8.3 详情与归属隔离 ---------------- */
  section('8.3 根据 id 查询地址  GET /user/address/{id}')

  const detail = await call('GET', `/user/address/${id1}`, { userToken: me })
  eq('查自己的地址 code = 1', detail.json?.code, 1)
  eq('返回的就是那一条', detail.json?.data?.consignee, '甲一')

  const stoleDetail = await call('GET', `/user/address/${id1}`, { userToken: other })
  eq('别人查我的地址 → 地址不存在', stoleDetail.json?.msg, '地址不存在')

  const ghost = await call('GET', '/user/address/99999999', { userToken: me })
  eq('不存在的 id → 地址不存在', ghost.json?.msg, '地址不存在')

  /* ---------------- 8.4 修改 ---------------- */
  section('8.4 修改地址  PUT /user/address')

  const patch = await call('PUT', '/user/address', {
    userToken: me,
    body: { id: id1, detail: '只改详址 99 号' }
  })
  eq('只提交 detail 也能改成功', patch.json?.code, 1)
  const afterPatch = rowOf(id1)
  eq('detail 已更新', afterPatch[4], '只改详址 99 号')
  eq('未提交的收货人保持原值', afterPatch[0], '甲一')
  eq('未提交的手机号保持原值', afterPatch[2], '13800001111')

  const clearLabel = await call('PUT', '/user/address', {
    userToken: me,
    body: { id: id1, label: '' }
  })
  eq('提交空串清除标签', clearLabel.json?.code, 1)
  eq('库中标签变空串', rowOf(id1)[5], '')

  const badPatch = await call('PUT', '/user/address', {
    userToken: me,
    body: { id: id1, phone: '12345' }
  })
  eq('改成非法手机号 → 手机号格式不正确', badPatch.json?.msg, '手机号格式不正确')
  eq('校验失败时库中未被改动', rowOf(id1)[2], '13800001111')

  const stealUpdate = await call('PUT', '/user/address', {
    userToken: other,
    body: { id: id1, consignee: '被改了' }
  })
  eq('别人改我的地址 → 地址不存在', stealUpdate.json?.msg, '地址不存在')
  eq('库中确实没被改', rowOf(id1)[0], '甲一')

  const setDefaultByPatch = await call('PUT', '/user/address', {
    userToken: me,
    body: { id: id1, isDefault: 1 }
  })
  eq('修改时提交 isDefault = 1 也能设默认', setDefaultByPatch.json?.code, 1)
  eq('默认互斥：只剩这一条', defaultIdsOf(ME).join(','), String(id1))
  eq('原先的默认被取消', rowOf(id3)[6], '0')

  /* ---------------- 8.5 设默认 ---------------- */
  section('8.5 设置默认地址  PUT /user/address/default')

  const setDefault = await call('PUT', '/user/address/default', {
    userToken: me,
    body: { id: id2 }
  })
  eq('设置成功', setDefault.json?.code, 1)
  eq('只有 id2 是默认', defaultIdsOf(ME).join(','), String(id2))
  eq('id1 不再是默认', rowOf(id1)[6], '0')

  const stealDefault = await call('PUT', '/user/address/default', {
    userToken: other,
    body: { id: id2 }
  })
  eq('别人设我的地址为默认 → 地址不存在', stealDefault.json?.msg, '地址不存在')
  eq('默认没有被动', defaultIdsOf(ME).join(','), String(id2))

  const setDefaultNoId = await call('PUT', '/user/address/default', {
    userToken: me,
    body: {}
  })
  eq('漏传 id → 地址不存在', setDefaultNoId.json?.msg, '地址不存在')

  /* ---------------- 8.7 默认地址 ---------------- */
  section('8.7 查询默认地址  GET /user/address/default')

  const def = await call('GET', '/user/address/default', { userToken: me })
  eq('有默认时返回该条', def.json?.data?.id, id2)
  eq('路径未被 /{id} 抢走（否则会报数字转换失败）', def.json?.code, 1)

  /* ---------------- 8.6 删除 ---------------- */
  section('8.6 删除地址  DELETE /user/address?id=')

  const stealDelete = await call('DELETE', '/user/address', {
    userToken: other,
    query: { id: id3 }
  })
  eq('别人删我的地址 → 地址不存在', stealDelete.json?.msg, '地址不存在')
  eq('库中该条还在', sql(`SELECT COUNT(*) FROM address WHERE id = ${id3}`), '1')

  const del = await call('DELETE', '/user/address', { userToken: me, query: { id: id3 } })
  eq('删自己的地址成功', del.json?.code, 1)
  eq('库中该条已删', sql(`SELECT COUNT(*) FROM address WHERE id = ${id3}`), '0')

  const delDefault = await call('DELETE', '/user/address', { userToken: me, query: { id: id2 } })
  eq('删除默认地址本身也能成功', delDefault.json?.code, 1)

  const afterDelDefault = await call('GET', '/user/address/default', { userToken: me })
  eq('默认被删后没有默认 → 没有查询到默认地址',
    afterDelDefault.json?.msg, '没有查询到默认地址')
  eq('此时 data 为 null', afterDelDefault.json?.data, null)
  eq('列表还剩 1 条', countOf(ME), '1')

  /* ---------------- 会员隔离 ---------------- */
  section('8.1 会员之间互不可见')

  await call('POST', '/user/address', { userToken: other, body: body({ consignee: '别家会员' }) })
  eq('另一个会员自己新增成功', countOf(OTHER), '1')
  const listOther = await call('GET', '/user/address/list', { userToken: other })
  eq('他的列表只有自己那 1 条', listOther.json?.data?.length, 1)
  eq('看不到我的地址', listOther.json?.data?.[0]?.consignee, '别家会员')

  /* ---------------- 后置清理 ---------------- */
  cleanup()
  eq('后置：测试会员的地址已清空', `${countOf(ME)}/${countOf(OTHER)}`, '0/0')
}
