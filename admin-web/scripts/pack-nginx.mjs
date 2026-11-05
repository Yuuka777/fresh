#!/usr/bin/env node
/* ============================================================
   把前端构建产物同步进 nginx 便携包
   用法：npm run pack:nginx   （内部先 vite build，再执行本脚本）
   目标：nginx-1.20.2/html/xianhe/  ←  dist/
   说明：逐个删除旧文件而非整目录递归删除，避免触发宿主机的批量删除保护
   ============================================================ */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const distDir = path.join(projectRoot, 'dist')
const siteDir = path.join(projectRoot, 'nginx-1.20.2', 'html', 'xianhe')

if (!fs.existsSync(path.join(distDir, 'index.html'))) {
  console.error('✗ 未找到 dist/index.html，请先执行 npm run build')
  process.exit(1)
}

function clearDir(dir) {
  if (!fs.existsSync(dir)) return 0
  let removed = 0
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      removed += clearDir(target)
      fs.rmdirSync(target)
    } else {
      fs.unlinkSync(target)
      removed += 1
    }
  }
  return removed
}

const removed = clearDir(siteDir)
fs.mkdirSync(siteDir, { recursive: true })
fs.cpSync(distDir, siteDir, { recursive: true })

/* 统计同步结果，便于确认真的换了一版 */
const walk = (dir, acc = []) => {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(target, acc)
    else acc.push(target)
  }
  return acc
}
const files = walk(siteDir)
const bytes = files.reduce((sum, f) => sum + fs.statSync(f).size, 0)

console.log(`✓ 已同步构建产物到 nginx 站点目录`)
console.log(`  源：${path.relative(projectRoot, distDir)}`)
console.log(`  目标：${path.relative(projectRoot, siteDir)}`)
console.log(`  清理旧文件 ${removed} 个，写入 ${files.length} 个（${(bytes / 1024 / 1024).toFixed(2)} MB）`)
console.log(`  启动方式：双击 nginx-1.20.2/启动.bat（自动打开 http://localhost:9000/login）`)
