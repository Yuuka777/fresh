/* Cookie 工具：管理端 token 存放位置为 Cookie（key = token），与《接口文档》1.3 一致 */
const DEFAULT_DAYS = 1

export function getCookie(key) {
  const m = document.cookie.match(new RegExp(`(?:^|;\\s*)${encodeURIComponent(key)}=([^;]*)`))
  return m ? decodeURIComponent(m[1]) : ''
}

export function setCookie(key, value, days = DEFAULT_DAYS) {
  const expires = new Date(Date.now() + days * 86400000).toUTCString()
  document.cookie = `${encodeURIComponent(key)}=${encodeURIComponent(value)}; expires=${expires}; path=/`
}

export function removeCookie(key) {
  document.cookie = `${encodeURIComponent(key)}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
}
