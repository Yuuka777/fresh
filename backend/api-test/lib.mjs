/**
 * 接口测试公共工具。
 *
 * 设计取舍：
 * 1. 零依赖 —— 只用 Node 内置的 fetch 与 child_process，避免为测试再引入 npm 包。
 *    在工程根执行 `node api-test/run.mjs` 即可。
 * 2. 同时验「HTTP 响应」与「数据库真实状态」。只断言响应体的话，
 *    服务端返回成功但没写库这类问题会被漏掉。
 */
import { execFileSync } from 'node:child_process'
import { createHmac } from 'node:crypto'
import { readFileSync } from 'node:fs'

export const BASE = process.env.XH_BASE || 'http://127.0.0.1:8080'

const MYSQL = process.env.XH_MYSQL || 'D:/tool/mysql/bin/mysql.exe'
const DB = process.env.XH_DB || 'xianhe_fresh'
const DB_USER = process.env.XH_DB_USER || 'root'
const DB_PASSWORD = process.env.XH_DB_PASSWORD || '123456'

const state = { pass: 0, fail: 0, failures: [] }

/**
 * 发一个 HTTP 请求。
 *
 * @param method HTTP 方法
 * @param path   以 / 开头的路径，例如 /admin/staff/login
 * @param options.body  请求体对象（自动 JSON 序列化）
 * @param options.token 管理端令牌，会放进 token 请求头
 * @param options.userToken 用户端令牌，会放进 authentication 请求头
 * @param options.query 查询参数对象
 */
export async function call(method, path, options = {}) {
  const { body, token, userToken, query } = options
  const url = new URL(BASE + path)
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null) url.searchParams.set(k, String(v))
    }
  }

  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.token = token
  if (userToken) headers[USER_HEADER] = userToken

  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  const text = await res.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    /* 非 JSON 响应（例如网关错误页）保持为 null，由调用方断言 */
  }
  return { status: res.status, json, text }
}

/**
 * 直接查库。用于前置清理、后置清理，以及断言「数据是否真的落库」。
 *
 * 必须带 --default-character-set=utf8mb4：Windows 下客户端默认 gbk，
 * 会把中文按错误编码解读。
 *
 * 返回值统一把 CRLF 归一成 LF：mysql.exe 在 Windows 下按 CRLF 输出，
 * 调用方普遍按 '\n' 切行，不归一的话每行尾部都会多出一个 '\r'，
 * 断言里就会出现「看起来一模一样却判不相等」的假失败。
 */
export function sql(query) {
  return execFileSync(
    MYSQL,
    [
      `-u${DB_USER}`,
      `-p${DB_PASSWORD}`,
      '-h127.0.0.1',
      '--default-character-set=utf8mb4',
      '-N',
      '-B',
      '-e',
      query,
      DB
    ],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }
  )
    .replace(/\r\n/g, '\n')
    .trim()
}

/* ---------------- 断言 ---------------- */

export function ok(name, condition, detail = '') {
  if (condition) {
    state.pass++
    console.log(`  \u2714 ${name}`)
  } else {
    state.fail++
    state.failures.push(name + (detail ? `（${detail}）` : ''))
    console.log(`  \u2718 ${name}${detail ? '   ' + detail : ''}`)
  }
}

export function eq(name, actual, expected) {
  const same = actual === expected
  ok(name, same, same ? '' : `期望 ${JSON.stringify(expected)}，实际 ${JSON.stringify(actual)}`)
}

export function section(title) {
  console.log(`\n${title}`)
}

export function summary(title) {
  console.log(`\n${'-'.repeat(56)}`)
  console.log(`${title}：通过 ${state.pass} / 失败 ${state.fail}`)
  if (state.failures.length) {
    console.log('失败项：')
    state.failures.forEach((f) => console.log(`  - ${f}`))
  }
  return state.fail
}

/** 供 run.mjs 汇总各模块结果 */
export function resetCounters() {
  state.pass = 0
  state.fail = 0
  state.failures = []
}

export function getCounters() {
  return { pass: state.pass, fail: state.fail, failures: [...state.failures] }
}

/** 等待服务就绪（服务冷启动或重启后使用） */
export async function waitForServer(timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const res = await fetch(BASE + '/admin/staff/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: '__probe__', password: '__probe__' })
      })
      if (res.status === 200) return true
    } catch {
      /* 服务还没起来，继续等 */
    }
    await new Promise((r) => setTimeout(r, 1000))
  }
  return false
}

/* ---------------- 用户端令牌 ---------------- */

/** 用户端令牌的请求头名（与 application.yml 的 xianhe.jwt.user-token-name 一致） */
export const USER_HEADER = 'authentication'

/**
 * 读本地 application-dev.yml 里的用户端 JWT 密钥。
 *
 * 为什么要自己签令牌：/user/member/login 需要小程序 wx.login 换来的 code，
 * 自动化测试拿不到（微信只认真实小程序发起的 code）。因此除登录接口本身之外，
 * 用户端接口的鉴权用「同一密钥、同一算法自签一个令牌」来覆盖——
 * 签出来的令牌与登录签发的在服务端看来完全等价。
 */
export function readUserJwt() {
  const yml = readFileSync(
    process.env.XH_DEV_YML || 'xianhe-server/src/main/resources/application-dev.yml',
    'utf8'
  )
  const secret = (yml.match(/user-secret-key:\s*(\S+)/) || [])[1]
  const ttl = Number((yml.match(/user-ttl:\s*(\d+)/) || [])[1] || 7200000)
  if (!secret) {
    throw new Error('application-dev.yml 里没找到 xianhe.jwt.user-secret-key')
  }
  return { secret, ttl }
}

/**
 * 自签一个用户端 JWT（HS256，与 JwtUtil.createJWT 的实现一致）
 *
 * @param memberId 会员 id，会写进载荷的 memberId 声明
 * @param options.secret 密钥，默认取自 application-dev.yml
 * @param options.ttlMs  有效期（毫秒），传负数可造一个已过期的令牌
 */
export function mintUserToken(memberId, options = {}) {
  const jwt = options.secret ? { secret: options.secret, ttl: 7200000 } : readUserJwt()
  const ttlMs = options.ttlMs === undefined ? jwt.ttl : options.ttlMs
  const b64 = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url')
  const exp = Math.floor((Date.now() + ttlMs) / 1000)
  const data = `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ memberId, exp })}`
  const sig = createHmac('sha256', Buffer.from(jwt.secret, 'utf8')).update(data).digest('base64url')
  return `${data}.${sig}`
}
