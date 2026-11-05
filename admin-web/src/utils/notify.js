/* ============================================================
   提示音：直接播放 src/assets 下的两个音频素材
   · 来单提醒 new-order.mp3
   · 催单提醒 reminder.mp3
   素材取自参照工程 sky-take-out 的前端 media 目录
   仅支付成功后才会由服务端推送（见 ADR 0004），此处只负责发声
   ============================================================ */
import newOrderSound from '@/assets/new-order.mp3'
import reminderSound from '@/assets/reminder.mp3'

/* 每次提醒都新建一个播放器：同一个实例重复播放时，浏览器会在播完后释放已缓冲的音频，
   再播只剩元数据、时间轴不走（表现为「第二次不响」） */
function play(src) {
  if (typeof Audio === 'undefined') return false
  const audio = new Audio(src)
  const started = audio.play()
  if (started && typeof started.catch === 'function') {
    // 浏览器要求页面先有一次用户交互才允许出声，冷启动直接收到推送时会走到这里
    started.catch(() => console.warn('提示音被浏览器拦截：页面需要先有一次点击'))
  }
  return true
}

/* 新订单：收到服务端 type = 1 时播放 */
export function playNewOrderSound() {
  return play(newOrderSound)
}

/* 客户催单：收到服务端 type = 2 时播放 */
export function playReminderSound() {
  return play(reminderSound)
}
