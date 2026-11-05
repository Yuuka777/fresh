/**
 * 通用模块接口测试（《接口文档》2.3，共 1 个接口：文件上传）。
 *
 * 注意事项：本测试会真的向对象存储写入对象，结束时用 OSS DELETE 清理。
 * 未配置凭证时会跳过清理并给出提示。
 */
import { BASE, call, eq, ok, section } from './lib.mjs'
import { deleteOssObject, ossReady } from './oss.mjs'

/** 1×1 像素的透明 PNG，作为最小可用的真实图片内容 */
const PNG_1X1 = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==',
  'base64'
)

async function upload(token, filename, buffer, contentType = 'image/png') {
  const form = new FormData()
  form.append('file', new Blob([buffer], { type: contentType }), filename)
  const res = await fetch(`${BASE}/admin/common/upload`, {
    method: 'POST',
    headers: token ? { token } : {},
    body: form
  })
  const text = await res.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    /* 非 JSON 响应 */
  }
  return { status: res.status, json }
}

export default async function run() {
  const login = await call('POST', '/admin/staff/login', {
    body: { username: 'admin', password: '123456' }
  })
  const token = login.json?.data?.token
  ok('取得管理端令牌', Boolean(token))

  const uploaded = []

  /* ---------------- 鉴权 ---------------- */
  section('2.3.1 文件上传 · 鉴权')

  const noToken = await upload(null, 'a.png', PNG_1X1)
  eq('不带 token → code = 0', noToken.json?.code, 0)
  eq('不带 token → msg', noToken.json?.msg, '登录状态已失效')

  /* ---------------- 正常上传 ---------------- */
  section('2.3.1 文件上传  POST /admin/common/upload')

  const okRes = await upload(token, 'xianhe-test.png', PNG_1X1)
  eq('上传图片 code = 1', okRes.json?.code, 1)
  const url = okRes.json?.data
  ok('返回的是 URL 字符串', typeof url === 'string' && url.startsWith('https://'), String(url))
  ok('对象名带 xh-goods 前缀与日期目录',
    typeof url === 'string' && /\/xh-goods\/\d{4}\/\d{2}\/\d{2}\/[0-9a-f]{32}\.png$/.test(url),
    String(url))
  if (url) uploaded.push(url)

  // 桶是公共读：匿名 GET 应当能取到对象，这是前端展示图片的前提
  if (url) {
    const fetched = await fetch(url)
    eq('上传后的 URL 可匿名访问', fetched.status, 200)
    const bytes = Buffer.from(await fetched.arrayBuffer())
    eq('取回的内容与上传一致', bytes.length, PNG_1X1.length)
  }

  // 客户端文件名不参与对象名：换个名字上传，路径仍由服务端生成
  const renamed = await upload(token, '../../evil.png', PNG_1X1)
  eq('文件名含路径穿越字符时仍可上传（服务端不使用客户端文件名）', renamed.json?.code, 1)
  ok('对象名中不含客户端文件名片段',
    typeof renamed.json?.data === 'string' && !renamed.json.data.includes('evil'),
    String(renamed.json?.data))
  if (renamed.json?.data) uploaded.push(renamed.json.data)

  /* ---------------- 类型校验 ---------------- */
  section('2.3.1 文件上传 · 类型白名单')

  const txt = await upload(token, 'note.txt', Buffer.from('hello', 'utf8'), 'text/plain')
  eq('非图片扩展名 → code = 0', txt.json?.code, 0)
  eq('非图片扩展名 → msg', txt.json?.msg, '文件上传失败')

  const noExt = await upload(token, 'noextension', PNG_1X1)
  eq('无扩展名 → msg', noExt.json?.msg, '文件上传失败')

  const gif = await upload(token, 'anim.gif', PNG_1X1, 'image/gif')
  eq('未在白名单内的 gif → msg', gif.json?.msg, '文件上传失败')

  /* ---------------- 体积校验 ---------------- */
  section('2.3.1 文件上传 · 体积上限（2MB）')

  const big = Buffer.alloc(3 * 1024 * 1024, 1) // 3MB
  const tooBig = await upload(token, 'big.png', big)
  eq('超过 2MB → code = 0', tooBig.json?.code, 0)
  eq('超过 2MB → msg', tooBig.json?.msg, '文件上传失败')

  /* ---------------- 清理 ---------------- */
  section('清理上传的测试对象')

  if (!ossReady) {
    console.log('  （未读到 OSS 凭证，跳过清理；如需手动清理见下方 URL）')
    uploaded.forEach((u) => console.log('    ' + u))
  } else if (uploaded.length === 0) {
    console.log('  （本次没有上传成功的对象）')
  } else {
    for (const u of uploaded) {
      const status = await deleteOssObject(u)
      ok(`已删除 ${u.split('/').pop()}（HTTP ${status}）`, status === 204 || status === 200)
    }
  }
}
