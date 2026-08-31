import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页' } },
      { path: 'flight', name: 'flight', component: () => import('@/views/flight/FlightView.vue'), meta: { title: '机票预订' } },
      { path: 'flight/:id', name: 'flight-detail', component: () => import('@/views/flight/FlightDetailView.vue'), meta: { title: '航班详情' } },
      { path: 'flight/:id/booking', name: 'flight-booking', component: () => import('@/views/flight/FlightBookingView.vue'), meta: { title: '机票下单', requiresAuth: true } },
      { path: 'train', name: 'train', component: () => import('@/views/train/TrainView.vue'), meta: { title: '火车票' } },
      { path: 'train/:id', name: 'train-detail', component: () => import('@/views/train/TrainDetailView.vue'), meta: { title: '车次详情' } },
      { path: 'train/:id/booking', name: 'train-booking', component: () => import('@/views/train/TrainBookingView.vue'), meta: { title: '火车票下单', requiresAuth: true } },
      { path: 'hotel', name: 'hotel', component: () => import('@/views/hotel/HotelView.vue'), meta: { title: '酒店住宿' } },
      { path: 'hotel/:id', name: 'hotel-detail', component: () => import('@/views/hotel/HotelDetailView.vue'), meta: { title: '酒店详情' } },
      { path: 'hotel/:id/booking', name: 'hotel-booking', component: () => import('@/views/hotel/HotelBookingView.vue'), meta: { title: '酒店预订', requiresAuth: true } },
      { path: 'tour', name: 'tour', component: () => import('@/views/tour/TourView.vue'), meta: { title: '旅游度假' } },
      { path: 'tour/:id', name: 'tour-detail', component: () => import('@/views/tour/TourDetailView.vue'), meta: { title: '产品详情' } },
      { path: 'tour/:id/booking', name: 'tour-booking', component: () => import('@/views/tour/TourBookingView.vue'), meta: { title: '旅游产品下单', requiresAuth: true } },
      { path: 'shares', name: 'shares', component: () => import('@/views/share/ShareListView.vue'), meta: { title: '旅行分享' } },
      { path: 'shares/create', name: 'share-create', component: () => import('@/views/share/ShareCreateView.vue'), meta: { title: '发布分享', requiresAuth: true } },
      { path: 'shares/:id', name: 'share-detail', component: () => import('@/views/share/ShareDetailView.vue'), meta: { title: '分享详情' } },
      { path: 'trip-plans', name: 'trip-plans', component: () => import('@/views/tripplan/TripPlanView.vue'), meta: { title: '行程规划', requiresAuth: true } },
      { path: 'trip-plans/:id', name: 'trip-plan-detail', component: () => import('@/views/tripplan/TripPlanDetailView.vue'), meta: { title: '行程详情', requiresAuth: true } },
      { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderView.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'orders/:id', name: 'order-detail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/user/ProfileView.vue'), meta: { title: '个人中心', requiresAuth: true } }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/auth/AdminLoginView.vue'),
    meta: { title: '后台登录', guestOnly: true }
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, title: '后台管理' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/dashboard/DashboardView.vue'), meta: { title: '后台首页', requiresAuth: true, requiresAdmin: true } },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/users/UserManageView.vue'), meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'products/flights', name: 'admin-flights', component: () => import('@/views/admin/products/FlightManageView.vue'), meta: { title: '航班管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'products/trains', name: 'admin-trains', component: () => import('@/views/admin/products/TrainManageView.vue'), meta: { title: '车次管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'products/hotels', name: 'admin-hotels', component: () => import('@/views/admin/products/HotelManageView.vue'), meta: { title: '酒店管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'products/rooms', name: 'admin-hotel-rooms', component: () => import('@/views/admin/products/HotelRoomManageView.vue'), meta: { title: '房型管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'products/tours', name: 'admin-tours', component: () => import('@/views/admin/products/TourManageView.vue'), meta: { title: '旅游产品管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/orders/OrderManageView.vue'), meta: { title: '订单管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'orders/:id', name: 'admin-order-detail', component: () => import('@/views/admin/orders/AdminOrderDetailView.vue'), meta: { title: '订单详情', requiresAuth: true, requiresAdmin: true } },
      { path: 'content/shares', name: 'admin-shares', component: () => import('@/views/admin/content/ShareManageView.vue'), meta: { title: '分享管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'content/reviews', name: 'admin-reviews', component: () => import('@/views/admin/content/ReviewManageView.vue'), meta: { title: '评价管理', requiresAuth: true, requiresAdmin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (to.meta?.title) {
    document.title = `${to.meta.title} - 出行旅游平台`
  }

  // Refresh a restored login once per page load instead of trusting stale localStorage data.
  if (userStore.isLoggedIn && !userStore.profileSynced) {
    try {
      await userStore.fetchCurrentUser()
    } catch (error) {
      userStore.clearLogin()
    }
  }

  if (to.meta?.guestOnly && userStore.isLoggedIn && userStore.isAdmin) {
    return '/admin/dashboard'
  }

  if (to.meta?.requiresAuth && !userStore.isLoggedIn) {
    return {
      path: to.meta?.requiresAdmin ? '/admin/login' : '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta?.requiresAdmin && !userStore.isAdmin) {
    return userStore.isLoggedIn
      ? '/'
      : {
          path: '/admin/login',
          query: { redirect: to.fullPath }
        }
  }

  return true
})

export default router
