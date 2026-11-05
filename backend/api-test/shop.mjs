/**
 * 店铺模块接口测试（《接口文档》2.4，共 2 个接口）。
 *
 * 除 HTTP 响应外，还直接读 Redis 校验 key 与值 —— 这个模块的数据不在数据库里，
 * 只看响应无法确认「状态真的写进去了」。
 */
import { execFileSync } from 'node:child_process'
import { call, eq, ok, section } from './lib.mjs'

const REDIS_CLI = process.env.XH_REDIS_CLI || 'D:/tool/Redis/Redis-x64-3.2.100/redis-cli.exe'
const REDIS_KEY = 'xh:shop:status'

function redis(...args) {
  return execFileSync(
    REDIS_CLI,
    ['-a', '123456', '-n', '11', ...args],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }
  ).trim()
}

export default async function run() {
  const login = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  const token = login.json?.data?.token
  ok('取得管理端令牌', Boolean(token))

  /* ---------------- 鉴权 ---------------- */
  section('2.4 店铺模块 · 鉴权')

  const noToken = await call('GET', '/admin/shop/status')
  eq('不带 token → code = 0', noToken.json?.code, 0)
  eq('不带 token → msg', noToken.json?.msg, '登录状态已失效')

  /* ---------------- 获取状态 ---------------- */
  section('2.4.2 获取营业状态  GET /admin/shop/status')

  const initial = await call('GET', '/admin/shop/status', { token })
  eq('获取状态 code = 1', initial.json?.code, 1)
  ok('返回整数状态值', Number.isInteger(initial.json?.data), JSON.stringify(initial.json?.data))

  /* ---------------- 设置打烊 ---------------- */
  section('2.4.1 设置营业状态  PUT /admin/shop/{status}')

  const close = await call('PUT', '/admin/shop/0', { token })
  eq('设为打烊 code = 1', close.json?.code, 1)
  eq(`Redis ${REDIS_KEY} 已写入 0`, redis('get', REDIS_KEY), '0')

  const afterClose = await call('GET', '/admin/shop/status', { token })
  eq('读回的状态为 0', afterClose.json?.data, 0)

  /* ---------------- 设置营业 ---------------- */
  const open = await call('PUT', '/admin/shop/1', { token })
  eq('设为营业中 code = 1', open.json?.code, 1)
  eq(`Redis ${REDIS_KEY} 已改为 1`, redis('get', REDIS_KEY), '1')

  const afterOpen = await call('GET', '/admin/shop/status', { token })
  eq('读回的状态为 1', afterOpen.json?.data, 1)

  ok('key 名称与约定一致（xh: 前缀，库 11）', redis('exists', REDIS_KEY) === '1')
}
