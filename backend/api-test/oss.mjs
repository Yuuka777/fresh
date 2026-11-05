/**
 * 对象存储相关的测试辅助。
 *
 * 用途：上传测试完成后删掉产生的对象，避免在 OSS 里留下垃圾。
 * 这里手写 OSS V1 签名（HMAC-SHA1），不引入 SDK —— 只为一个 DELETE 请求不值得加依赖。
 *
 * 凭证来源：读工程内的 application-dev.yml，避免在测试代码里再维护一份。
 */
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = path.dirname(fileURLToPath(import.meta.url))
const DEV_YML = path.resolve(HERE, '../xianhe-server/src/main/resources/application-dev.yml')

/** 从 yml 中取出 `key: value` 形式的配置项，够用即可，不引入 YAML 解析库 */
function readConfig() {
  const fallback = { endpoint: '', accessKeyId: '', accessKeySecret: '', bucketName: '' }
  if (!fs.existsSync(DEV_YML)) {
    return fallback
  }
  const text = fs.readFileSync(DEV_YML, 'utf8')
  const pick = (key) => {
    const m = text.match(new RegExp(`^\\s*${key}:\\s*(\\S+)\\s*$`, 'm'))
    return m ? m[1] : ''
  }
  return {
    endpoint: pick('endpoint'),
    accessKeyId: pick('access-key-id'),
    accessKeySecret: pick('access-key-secret'),
    bucketName: pick('bucket-name')
  }
}

const cfg = readConfig()

export const ossReady = Boolean(cfg.accessKeyId && cfg.accessKeySecret && cfg.bucketName)

/**
 * 删除一个由上传接口返回的 OSS 对象。
 *
 * @param url 上传接口返回的公网地址
 * @returns HTTP 状态码；未配置凭证时返回 0 表示跳过
 */
export async function deleteOssObject(url) {
  if (!ossReady) {
    return 0
  }
  const parsed = new URL(url)
  const object = decodeURIComponent(parsed.pathname.replace(/^\//, ''))
  const date = new Date().toUTCString()

  // OSS V1 签名串：VERB + Content-MD5 + Content-Type + Date + CanonicalizedResource
  const stringToSign = ['DELETE', '', '', date, `/${cfg.bucketName}/${object}`].join('\n')
  const signature = crypto
    .createHmac('sha1', cfg.accessKeySecret)
    .update(stringToSign, 'utf8')
    .digest('base64')

  const res = await fetch(url, {
    method: 'DELETE',
    headers: {
      Date: date,
      Authorization: `OSS ${cfg.accessKeyId}:${signature}`
    }
  })
  return res.status
}

export const ossBucket = cfg.bucketName
