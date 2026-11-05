import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import 'element-plus/dist/index.css'
import '@/styles/tokens.css'
import '@/styles/element.css'
import '@/styles/base.css'

import App from './App.vue'
import router from './router'
import ListPager from '@/components/ListPager.vue'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default', zIndex: 2000 })
/* 列表分页条是每个列表页都要用的版式件，全局注册以免五处重复引入 */
app.component('ListPager', ListPager)
app.mount('#app')
