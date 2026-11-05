/* ============================================================
   运行期配置
   ============================================================ */

/* 后端入口。小程序仅本地开发、不发布，故直连本机后端：
     · 8080 → 后端本体（推荐，少一跳）
     · 9000 → 工程内 nginx 便携包（/user/** 已被代理，用于验证整链路）
   切换时改这里即可。 */
export const BASE_URL = 'http://localhost:8080'

/* 本地存储 key（统一前缀，避免与第三方 SDK 冲突） */
export const STORAGE_KEYS = {
  token: 'xh_token',
  member: 'xh_member',
  addressId: 'xh_last_address_id',
  searchHistory: 'xh_search_history'
}

/* 无需登录即可调用的接口（与《接口文档-用户端》放行白名单一致） */
export const AUTH_WHITE_LIST = ['/user/member/login', '/user/shop/status']

/* 业务常量 */
export const DELIVERY_FEE = 5 // 运送费（元）；后续若后端下发则以接口为准
export const ORDER_PAY_TIMEOUT_MINUTES = 15 // 未支付订单超时自动取消（与服务端 Spring Task 一致）
export const DEFAULT_PAGE_SIZE = 10
