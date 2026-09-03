import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { auth: true } },
    { path: '/book', name: 'book', component: () => import('../views/BookingListView.vue'), meta: { guest: true } },
    { path: '/book/:id', name: 'book-detail', component: () => import('../views/BookingDetailView.vue'), meta: { auth: true } },
    { path: '/leave', name: 'leave', component: () => import('../views/LeaveView.vue'), meta: { auth: true } },
    { path: '/history', name: 'history', component: () => import('../views/HistoryView.vue'), meta: { auth: true } },
    { path: '/mine', name: 'mine', component: () => import('../views/MineView.vue'), meta: { auth: true } },
    { path: '/contract', name: 'contract', component: () => import('../views/ContractView.vue'), meta: { auth: true } },
    { path: '/teacher', name: 'teacher', component: () => import('../views/TeacherView.vue'), meta: { auth: true, teacher: true } },
    { path: '/teacher/schedule/:date', name: 'teacher-day-schedule', component: () => import('../views/TeacherDayScheduleView.vue'), meta: { auth: true, teacher: true } },
    { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { auth: true, admin: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const role = auth.user?.role
  // 需要登录的页面：未登录或游客，一律去登录
  if (to.meta.auth && !auth.isLoggedIn) return { name: 'login' }
  // 游客仅能访问标记 guest 的页面（师资介绍），其余去登录
  if (role === 'guest' && !to.meta.guest && to.name !== 'login') return { name: 'login' }
  if (to.meta.teacher && role !== 'teacher') return { name: 'home' }
  if (to.meta.admin && role !== 'admin') return { name: 'home' }
  if (role === 'teacher' && !to.meta.teacher && to.name !== 'login') return { name: 'teacher' }
  if (role === 'admin' && !to.meta.admin && to.name !== 'login') return { name: 'admin' }
})

export default router
