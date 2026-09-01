import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { auth: true } },
    { path: '/book', name: 'book', component: () => import('../views/BookingListView.vue'), meta: { auth: true } },
    { path: '/book/:id', name: 'book-detail', component: () => import('../views/BookingDetailView.vue'), meta: { auth: true } },
    { path: '/leave', name: 'leave', component: () => import('../views/LeaveView.vue'), meta: { auth: true } },
    { path: '/history', name: 'history', component: () => import('../views/HistoryView.vue'), meta: { auth: true } },
    { path: '/mine', name: 'mine', component: () => import('../views/MineView.vue'), meta: { auth: true } },
    { path: '/teacher', name: 'teacher', component: () => import('../views/TeacherView.vue'), meta: { auth: true, teacher: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) return { name: 'login' }
  const isTeacher = auth.user?.role === 'teacher'
  if (to.meta.teacher && !isTeacher) return { name: 'home' }
  if (!to.meta.teacher && isTeacher && to.name !== 'login') return { name: 'teacher' }
})

export default router
