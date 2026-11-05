import { createRouter, createWebHistory } from 'vue-router'
import { getCookie } from '@/utils/cookie'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '工作台', menu: 'dashboard' }
      },
      {
        path: 'statistics',
        name: 'statistics',
        component: () => import('@/views/StatisticsView.vue'),
        meta: { title: '数据统计', menu: 'statistics' }
      },
      {
        path: 'order',
        name: 'order',
        component: () => import('@/views/OrderView.vue'),
        meta: { title: '订单管理', menu: 'order' }
      },
      {
        path: 'bundle',
        name: 'bundle',
        component: () => import('@/views/BundleView.vue'),
        meta: { title: '组合箱管理', menu: 'bundle' }
      },
      {
        path: 'bundle/add',
        name: 'bundle-add',
        component: () => import('@/views/BundleEditView.vue'),
        meta: { title: '新增组合箱', editTitle: '修改组合箱', menu: 'bundle' }
      },
      {
        path: 'goods',
        name: 'goods',
        component: () => import('@/views/GoodsView.vue'),
        meta: { title: '商品管理', menu: 'goods' }
      },
      {
        path: 'goods/add',
        name: 'goods-add',
        component: () => import('@/views/GoodsEditView.vue'),
        meta: { title: '新增商品', editTitle: '修改商品', menu: 'goods' }
      },
      {
        path: 'goodsCategory',
        name: 'goodsCategory',
        component: () => import('@/views/CategoryView.vue'),
        meta: { title: '商品分类管理', menu: 'goodsCategory' }
      },
      {
        path: 'staff',
        name: 'staff',
        component: () => import('@/views/StaffView.vue'),
        meta: { title: '店员管理', menu: 'staff' }
      },
      {
        path: 'staff/add',
        name: 'staff-add',
        component: () => import('@/views/StaffEditView.vue'),
        meta: { title: '新增店员', editTitle: '修改店员', menu: 'staff' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人主页', menu: 'profile' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 鲜禾运营台` : '鲜禾运营台'
  if (to.meta.public) return true
  if (!getCookie('token')) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  return true
})

export default router
