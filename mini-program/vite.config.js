import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  css: {
    preprocessorOptions: {
      scss: {
        /* uni-app 当前走的是 sass 旧版 JS API，屏蔽 Dart Sass 2.0 的弃用告警，
           否则每次编译都会刷出二十余条无关输出，掩盖真正的报错 */
        silenceDeprecations: ['legacy-js-api']
      }
    }
  }
})
