// 真实后端 API（Spring Boot :8080），Axios + JWT 拦截器
// 接口约定（视图只需知道这两条）：
//   查询类：失败直接 throw（视图可在 onMounted 外层兜底）
//   操作类：统一返回 { ok, msg? }，永不 throw（内部 attempt 收敛 try/catch）
import type { Booking, Credit, Teacher, Timeslot, User } from '../types'
import { http } from './http'

async function call<T>(p: Promise<any>): Promise<T> {
  const res = await p
  const body = res.data
  if (body.code !== 0) throw Object.assign(new Error(body.msg), { code: body.code })
  return body.data as T
}

/** 操作类统一出口：把异常收敛为 { ok:false, msg }，消灭每个方法的重复 try/catch */
async function attempt(p: Promise<any>): Promise<{ ok: boolean; msg?: string }> {
  try {
    await call(p)
    return { ok: true }
  } catch (e: any) {
    return { ok: false, msg: e.message }
  }
}

interface RawTeacher { id: number; name: string; title: string; intro: string; rating: number; subjects: string[] }

export const api = {
  /** 登录（纯登录：未注册手机号会被后端拒绝；老师未过审也会被拒） */
  async login(phone: string, password: string, role: 'student' | 'teacher' | 'admin'): Promise<User> {
    const roleNum = role === 'student' ? 1 : role === 'teacher' ? 2 : 3
    const data = await call<any>(http.post('/api/auth/login', { phone, password, role: roleNum }))
    sessionStorage.setItem('token', data.token)
    return { id: data.userId, role, name: data.name, phone }
  },

  /**
   * 注册：仅学员/老师。
   *  - 学员：成功即自动登录返回 user（已有 token）。
   *  - 老师：进入待审（pending=true），审核通过前无法登录老师端。
   */
  async register(p: {
    role: 'student' | 'teacher'; name: string; phone: string; password: string; subjectId?: number | null
  }): Promise<{ ok: boolean; msg?: string; pending?: boolean; user?: User }> {
    try {
      const d = await call<any>(http.post('/api/auth/register', {
        role: p.role === 'student' ? 1 : 2,
        name: p.name, phone: p.phone, password: p.password,
        subjectId: p.subjectId ?? undefined,
      }))
      if (d.token) {
        sessionStorage.setItem('token', d.token)
        return { ok: true, user: { id: d.userId, role: p.role, name: d.name, phone: p.phone } }
      }
      return { ok: true, pending: true } // 老师待审：后端不签发 token
    } catch (e: any) {
      return { ok: false, msg: e.message }
    }
  },

  /** 科目列表（登录页老师注册用；视图不再直接摸 http） */
  async listSubjects(): Promise<{ id: number; name: string }[]> {
    return call(http.get('/api/auth/subjects'))
  },
  /** 当前登录用户资料（按 token；「我的」页刷新真实姓名/手机号） */
  async getMe(): Promise<User> {
    const d = await call<any>(http.get('/api/auth/me'))
    const roleMap: Record<number, User['role']> = { 1: 'student', 2: 'teacher', 3: 'admin' }
    return { id: d.userId, role: roleMap[d.role] || 'guest', name: d.name, phone: d.phone }
  },

  async listTeachers(): Promise<Teacher[]> {
    const list = await call<RawTeacher[]>(http.get('/api/teachers'))
    return list.map((t) => ({ ...t, subjects: t.subjects.map((n, i) => ({ id: i, name: n })) }))
  },

  async getTeacher(id: number): Promise<Teacher | undefined> {
    const list = await this.listTeachers()
    return list.find((t) => t.id === id)
  },

  async listSlots(teacherId: number): Promise<Timeslot[]> {
    return call<Timeslot[]>(http.get(`/api/teachers/${teacherId}/slots`))
  },

  async getCredits(): Promise<Credit[]> {
    return call<Credit[]>(http.get('/api/credits'))
  },

  async createBooking(teacherId: number, slot: Timeslot): Promise<{ ok: boolean; msg?: string; booking?: Booking }> {
    try {
      const booking = await call<Booking>(http.post('/api/bookings', { teacherId, startAt: slot.startAt, endAt: slot.endAt }))
      return { ok: true, booking }
    } catch (e: any) {
      return { ok: false, msg: e.message }
    }
  },

  async listBookings(): Promise<Booking[]> {
    const [list, teachers] = await Promise.all([
      call<Booking[]>(http.get('/api/bookings')),
      this.listTeachers(),
    ])
    const tmap = new Map(teachers.map((t) => [t.id, t]))
    return list.map((b) => {
      const t = tmap.get(b.teacherId)
      return { ...b, teacherName: t?.name ?? '老师', subjectName: t?.subjects[0]?.name ?? '' }
    })
  },

  async createLeave(bookingId: number, reason: string) {
    return attempt(http.post('/api/leave', { bookingId, reason }))
  },

  // ---- 老师端 ----
  async getTeacherBookings(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/bookings'))
  },
  async getWeekSchedule(weekOffset: number): Promise<any> {
    return call<any>(http.get('/api/teacher/week-schedule', { params: { weekOffset } }))
  },
  async completeBooking(id: number) {
    return attempt(http.post(`/api/teacher/bookings/${id}/complete`, {}))
  },
  async getTeacherLeaves(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/leaves'))
  },
  async getPendingCompletions(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/pending-completions'))
  },
  async handleLeave(id: number, approve: boolean) {
    return attempt(http.post(`/api/teacher/leaves/${id}/handle`, { approve }))
  },
  async getTemplates(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/templates'))
  },
  async addTemplate(t: { weekday: number; start: string; end: string; subjectId?: number }) {
    return attempt(http.post('/api/teacher/templates', t))
  },
  async toggleTemplate(id: number) {
    return attempt(http.post(`/api/teacher/templates/${id}/toggle`, {}))
  },
  async updateTemplate(id: number, start: string, end: string) {
    return attempt(http.put(`/api/teacher/templates/${id}`, { start, end }))
  },
  async deleteTemplate(id: number) {
    return attempt(http.delete(`/api/teacher/templates/${id}`))
  },

  // ---- 管理端 ----
  async getAdminTeachers(status?: number): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/teachers', { params: status != null ? { status } : {} }))
  },
  async auditTeacher(userId: number, approve: boolean) {
    return attempt(http.post(`/api/admin/teachers/${userId}/audit`, { approve }))
  },
  async getDashboard(): Promise<any> {
    return call<any>(http.get('/api/admin/dashboard'))
  },
  async getAdminUsers(role?: number): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/users', { params: role != null ? { role } : {} }))
  },
  async toggleUser(id: number, enable: boolean) {
    return attempt(http.post(`/api/admin/users/${id}/toggle`, { enable }))
  },
  /** 管理员新建账号（学员/管理员；老师须公开注册+审核） */
  async adminCreateUser(p: { role: 'student' | 'admin'; name: string; phone: string; password: string }) {
    return attempt(http.post('/api/admin/users', {
      role: p.role === 'student' ? 1 : 3, name: p.name, phone: p.phone, password: p.password,
    }))
  },
  async getAdminSubjects(): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/subjects'))
  },
  async addSubject(name: string, category: string) {
    return attempt(http.post('/api/admin/subjects', { name, category }))
  },

  // ---- 管理端排课（代学生预约未来课程）----
  async getAdminPlanSlots(teacherId: number, date: string): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/plan/slots', { params: { teacherId, date } }))
  },
  async adminBookPlan(p: { studentId: number; teacherId: number; startAt: string; endAt: string }) {
    return attempt(http.post('/api/admin/plan/book', p))
  },

  // ---- 管理端停课（老师休息日：屏蔽某日/某时段可约）----
  async getAdminBlocks(teacherId: number): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/blocks', { params: { teacherId } }))
  },
  async adminAddBlock(p: { teacherId: number; date: string; start?: string; end?: string; reason?: string }) {
    return attempt(http.post('/api/admin/blocks', p))
  },
  async adminRemoveBlock(id: number) {
    return attempt(http.delete(`/api/admin/blocks/${id}`))
  },

  // ---- 合同 ----
  async getContracts(): Promise<any[]> {
    return call<any[]>(http.get('/api/contracts'))
  },
  async purchase(teacherId: number, credits: number) {
    return attempt(http.post('/api/contracts', { teacherId, credits }))
  },
  async signContract(id: number) {
    return attempt(http.post(`/api/contracts/${id}/sign`, {}))
  },
}
