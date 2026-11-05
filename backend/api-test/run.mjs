/**
 * 接口测试入口。
 *
 * 用法：
 *   node api-test/run.mjs            # 跑全部模块
 *   node api-test/run.mjs staff      # 只跑店员模块
 *
 * 前置条件：后端已在 http://127.0.0.1:8080 运行，且数据库 xianhe_fresh 已导入种子数据。
 */
import { getCounters, resetCounters, summary, waitForServer, BASE } from './lib.mjs'
import staff from './staff.mjs'
import category from './category.mjs'
import common from './common.mjs'
import shop from './shop.mjs'
import goods from './goods.mjs'
import bundle from './bundle.mjs'
import userLogin from './user-login.mjs'
import userGoods from './user-goods.mjs'
import userCart from './user-cart.mjs'
import userAddress from './user-address.mjs'
import userOrder from './user-order.mjs'
import adminOrder from './admin-order.mjs'
import workspace from './workspace.mjs'
import report from './report.mjs'
import hardening from './hardening.mjs'

/** 模块清单：左侧为命令行参数名，右侧为测试函数 */
const MODULES = [
  ['staff', '店员模块', staff],
  ['category', '商品分类模块', category],
  ['common', '通用模块（文件上传）', common],
  ['shop', '店铺模块', shop],
  ['goods', '商品模块', goods],
  ['bundle', '组合箱模块', bundle],
  ['user-login', '用户端 · 登录与鉴权', userLogin],
  ['user-goods', '用户端 · 商品浏览（含缓存）', userGoods],
  ['user-cart', '用户端 · 购物车', userCart],
  ['user-address', '用户端 · 收货地址', userAddress],
  ['user-order', '用户端 · 订单（提交 / 支付 / 查询 / 详情 / 取消 / 复购 / 催单）', userOrder],
  ['admin-order', '管理端 · 订单（搜索 / 状态流转 / 超时取消）', adminOrder],
  ['workspace', '管理端 · 工作台（今日数据 / 订单·商品·组合箱总览）', workspace],
  ['report', '管理端 · 报表（营业额 / 会员 / 订单 / 销量 TOP10）', report],
  ['hardening', '加固项（并发 / 鉴权 / 口令 / 上传 / 索引）', hardening]
]

const picked = process.argv[2]
const todo = picked ? MODULES.filter(([key]) => key === picked) : MODULES

if (!todo.length) {
  console.error(`未知模块：${picked}`)
  console.error(`可选：${MODULES.map(([key]) => key).join(', ')}`)
  process.exit(2)
}

console.log(`后端地址：${BASE}`)
console.log('等待服务就绪…')

if (!(await waitForServer())) {
  console.error(`服务未在 ${BASE} 响应，请先启动后端。`)
  process.exit(2)
}
console.log('服务已就绪。')

let totalPass = 0
let totalFail = 0

for (const [key, title, run] of todo) {
  resetCounters()
  console.log(`\n${'='.repeat(56)}`)
  console.log(`  ${title}（${key}）`)
  console.log('='.repeat(56))
  try {
    await run()
  } catch (err) {
    console.error('\n模块执行中断：', err.message)
  }
  totalFail += summary(title)
  totalPass += getCounters().pass
}

console.log(`\n${'='.repeat(56)}`)
console.log(`合计：通过 ${totalPass} / 失败 ${totalFail}`)
console.log('='.repeat(56))

process.exit(totalFail > 0 ? 1 : 0)
