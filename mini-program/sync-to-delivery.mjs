/* 把构建产物铺回交付目录（工程根 = 微信小程序工程）
   保留交付目录内的文档与 .gitignore，其余整体替换 */
import fs from 'node:fs'
import path from 'node:path'

const OUT = 'D:/project/.workbuddy/tmp/build-xianhe/dist/build/mp-weixin'
const ROOT = 'D:/project/wechat_project/xianhe-fresh'
const KEEP = new Set(['.gitignore', '接口文档-用户端.md'])

if (!fs.existsSync(OUT)) {
  console.error('构建产物不存在，请先跑 build -p mp-weixin')
  process.exit(1)
}

const purge = (p) => {
  if (!fs.existsSync(p)) return
  if (!fs.statSync(p).isDirectory()) {
    fs.unlinkSync(p)
    return
  }
  for (const e of fs.readdirSync(p, { withFileTypes: true })) purge(path.join(p, e.name))
  fs.rmdirSync(p)
}

let removed = 0
for (const e of fs.readdirSync(ROOT, { withFileTypes: true })) {
  if (KEEP.has(e.name)) continue
  purge(path.join(ROOT, e.name))
  removed++
}

let n = 0
let kb = 0
const copy = (src, dst) => {
  for (const e of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, e.name)
    const d = path.join(dst, e.name)
    if (e.isDirectory()) {
      fs.mkdirSync(d, { recursive: true })
      copy(s, d)
    } else {
      fs.mkdirSync(path.dirname(d), { recursive: true })
      fs.copyFileSync(s, d)
      n++
      kb += fs.statSync(d).size
    }
  }
}
copy(OUT, ROOT)

const cnt = (p) => {
  let c = 0
  const w = (d) => {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const f = path.join(d, e.name)
      e.isDirectory() ? w(f) : c++
    }
  }
  fs.statSync(p).isDirectory() ? w(p) : (c = 1)
  return c
}

console.log('清掉旧内容 ' + removed + ' 项，铺入 ' + n + ' 个文件 / ' + (kb / 1024).toFixed(0) + 'KB')
console.log('')
console.log('交付目录：')
for (const e of fs.readdirSync(ROOT, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
  const p = path.join(ROOT, e.name)
  console.log('  ' + (e.name + (e.isDirectory() ? '/' : '')).padEnd(26) + (e.isDirectory() ? cnt(p) + ' 文件' : (fs.statSync(p).size / 1024).toFixed(1) + ' KB'))
}

const pc = JSON.parse(fs.readFileSync(ROOT + '/project.config.json', 'utf8'))
const app = JSON.parse(fs.readFileSync(ROOT + '/app.json', 'utf8'))
let total = 0
const w = (d) => {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const f = path.join(d, e.name)
    e.isDirectory() ? w(f) : (total += fs.statSync(f).size)
  }
}
w(ROOT)

const files = []
const walk = (d) => {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const f = path.join(d, e.name)
    e.isDirectory() ? walk(f) : files.push(f)
  }
}
walk(ROOT)
const iconCount = files.filter((f) => f.includes('static' + path.sep + 'icons')).length

console.log('')
console.log('  appid=' + pc.appid + '  pages=' + app.pages.length + '  图标 ' + iconCount + ' 个')
console.log('  整包 ' + (total / 1024).toFixed(0) + 'KB（主包上限 2048KB）')
console.log('  保留文件：' + [...KEEP].filter((f) => fs.existsSync(path.join(ROOT, f))).join('、'))
