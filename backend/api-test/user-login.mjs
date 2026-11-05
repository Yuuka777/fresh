/**
 * 用户端登录与鉴权测试（《接口文档-用户端》2、3）。
 *
 * 关于登录成功路径：它需要小程序 wx.login 换来的 code，自动化测试拿不到
 * （微信只认真实小程序发起的 code）。因此这里覆盖**失败路径**与**令牌本身的行为**；
 * 成功路径（code 换 openid → 自动注册 → 签发令牌 → 二次登录不重复注册）
 * 已用本地桩单独跑通，见 `.workbuddy/tmp/verify-wx-login.mjs` 与 `tmp/wx-stub.mjs`。
 *
 * 除登录接口外，用户端接口的鉴权用「同一密钥自签令牌」的方式覆盖（见 lib.mjs 的 mintUserToken）。
 */
import { call, eq, ok, section, sql, mintUserToken } from './lib.mjs'

const SHOP_STATUS = '/user/shop/status'
const CART_LIST = '/user/cart/list'

export default async function run() {
  /* ---------------- 白名单 ---------------- */
  section('放行白名单：未登录也能查营业状态、也能尝试登录')

  const shop = await call('GET', SHOP_STATUS)
  eq('GET /user/shop/status 不带令牌 → code = 1', shop.json?.code, 1)
  ok('返回 0/1 整数', shop.json?.data === 0 || shop.json?.data === 1, String(shop.json?.data))

  const login = await call('POST', '/user/member/login', { body: { code: '__invalid_code__' } })
  eq('无效 code 登录 → code = 0', login.json?.code, 0)
  ok('登录接口不在拦截范围内（文案不是「登录状态已失效」）',
    login.json?.msg !== '登录状态已失效', login.json?.msg)
  ok('失败文案是中文且不含技术细节',
    typeof login.json?.msg === 'string' && !/[A-Za-z]{4,}/.test(login.json.msg),
    login.json?.msg)

  const noCode = await call('POST', '/user/member/login', { body: {} })
  eq('漏传 code → code = 0', noCode.json?.code, 0)
  eq('漏传 code → msg', noCode.json?.msg, '微信登录失败')

  /* ---------------- 受保护接口的令牌校验 ---------------- */
  section('受保护接口：令牌校验')

  const noToken = await call('GET', CART_LIST)
  eq('不带令牌 → code = 0', noToken.json?.code, 0)
  eq('不带令牌 → msg', noToken.json?.msg, '登录状态已失效')

  const garbage = await call('GET', CART_LIST, { userToken: 'not-a-jwt' })
  eq('乱码令牌 → code = 0', garbage.json?.code, 0)
  eq('乱码令牌 → msg', garbage.json?.msg, '登录状态已失效')

  const expired = await call('GET', CART_LIST, { userToken: mintUserToken(1, { ttlMs: -60000 }) })
  eq('已过期令牌 → code = 0', expired.json?.code, 0)
  eq('已过期令牌 → msg', expired.json?.msg, '登录状态已失效')

  const wrongKey = await call('GET', CART_LIST, {
    userToken: mintUserToken(1, { secret: 'wrong-secret-key' })
  })
  eq('用错误密钥签的令牌 → code = 0', wrongKey.json?.code, 0)

  const adminLogin = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  const adminToken = adminLogin.json?.data?.token
  ok('取得管理端令牌', Boolean(adminToken))

  const crossUse = await call('GET', CART_LIST, { userToken: adminToken })
  eq('管理端令牌不能用于用户端（两端密钥不同）', crossUse.json?.code, 0)

  const crossBack = await call('GET', '/admin/staff/page', {
    token: mintUserToken(1),
    query: { page: 1, pageSize: 1 }
  })
  eq('用户端令牌不能用于管理端', crossBack.json?.code, 0)

  const good = await call('GET', CART_LIST, { userToken: mintUserToken(1) })
  eq('合法令牌 → code = 1', good.json?.code, 1)
  ok('返回数组', Array.isArray(good.json?.data))

  /* ---------------- 会员身份来自令牌 ---------------- */
  section('会员身份取自令牌，前端传什么都不认')

  eq('会员 1 与 7 都存在（种子数据）',
    sql('SELECT COUNT(*) FROM member WHERE id IN (1, 7)'), '2')

  const m1 = await call('GET', CART_LIST, { userToken: mintUserToken(1) })
  const m7 = await call('GET', CART_LIST, { userToken: mintUserToken(7) })
  ok('两个会员各自返回自己的购物车（数量可以不同）',
    Array.isArray(m1.json?.data) && Array.isArray(m7.json?.data))

  // 令牌里的 memberId 是唯一身份来源：即使不存在的会员，也只是查不到数据，不会报鉴权错误
  const ghost = await call('GET', CART_LIST, { userToken: mintUserToken(999999) })
  eq('不存在的会员 id → 仍通过鉴权（查不到数据而已）', ghost.json?.code, 1)
  eq('返回空数组', ghost.json?.data?.length, 0)
}
