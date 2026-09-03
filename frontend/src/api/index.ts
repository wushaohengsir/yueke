// 真实后端 API（Spring Boot :8080），Axios + JWT 拦截器
import type { Booking, Credit, Teacher, Timeslot, User } from '../types'
import { http } from './http'

async function call<T>(p: Promise<any>): Promise<T> {
  const res = await p
  const body = res.data
  if (body.code !== 0) throw Object.assign(new Error(body.msg), { code: body.code })
  return body.data as T
}

interface RawTeacher { id: number; name: string; title: string; intro: string; rating: number; subjects: string[] }

export const api = {
  async login(phone: string, password: string, role: 'student' | 'teacher' | 'admin',
              subjectId?: number | null, name?: string): Promise<User> {
    const roleNum = role === 'student' ? 1 : role === 'teacher' ? 2 : 3
    const data = await call<any>(http.post('/api/auth/login', {
      phone, password, role: roleNum,
      subjectId: subjectId ?? undefined,
      name: name || undefined,
    }))
    localStorage.setItem('token', data.token)
    return { id: data.userId, role, name: data.name, phone }
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

  async createBooking(teacherId: number, slot: Timeslot, _student: User): Promise<{ ok: boolean; msg?: string; booking?: Booking }> {
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

  async createLeave(bookingId: number, reason: string): Promise<{ ok: boolean }> {
    try {
      await call(http.post('/api/leave', { bookingId, reason }))
      return { ok: true }
    } catch {
      return { ok: false }
    }
  },

  async listLeaves(): Promise<any[]> { return [] },

  // ---- 老师端 ----
  async getTeacherBookings(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/bookings'))
  },
  async getWeekSchedule(weekOffset: number): Promise<any> {
    return call<any>(http.get('/api/teacher/week-schedule', { params: { weekOffset } }))
  },
  async completeBooking(id: number): Promise<{ ok: boolean; msg?: string }> {
    try { await call(http.post(`/api/teacher/bookings/${id}/complete`, {})); return { ok: true } }
    catch (e: any) { return { ok: false, msg: e.message } }
  },
  async getTeacherLeaves(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/leaves'))
  },
  async handleLeave(id: number, approve: boolean): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/teacher/leaves/${id}/handle`, { approve })); return { ok: true } }
    catch { return { ok: false } }
  },
  async getTemplates(): Promise<any[]> {
    return call<any[]>(http.get('/api/teacher/templates'))
  },
  async addTemplate(t: { weekday: number; start: string; end: string; subjectId?: number }): Promise<{ ok: boolean }> {
    try { await call(http.post('/api/teacher/templates', t)); return { ok: true } }
    catch { return { ok: false } }
  },
  async toggleTemplate(id: number): Promise<{ ok: boolean; msg?: string }> {
    try { await call(http.post(`/api/teacher/templates/${id}/toggle`, {})); return { ok: true } }
    catch (e: any) { return { ok: false, msg: e.message } }
  },
  async deleteTemplate(id: number): Promise<{ ok: boolean }> {
    try { await call(http.delete(`/api/teacher/templates/${id}`)); return { ok: true } }
    catch { return { ok: false } }
  },

  // ---- 管理端 ----
  async getAdminTeachers(status?: number): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/teachers', { params: status != null ? { status } : {} }))
  },
  async auditTeacher(userId: number, approve: boolean): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/admin/teachers/${userId}/audit`, { approve })); return { ok: true } }
    catch { return { ok: false } }
  },
  async getDashboard(): Promise<any> {
    return call<any>(http.get('/api/admin/dashboard'))
  },
  async getAdminUsers(role?: number): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/users', { params: role != null ? { role } : {} }))
  },
  async toggleUser(id: number, enable: boolean): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/admin/users/${id}/toggle`, { enable })); return { ok: true } }
    catch { return { ok: false } }
  },
  async getAdminSubjects(): Promise<any[]> {
    return call<any[]>(http.get('/api/admin/subjects'))
  },
  async addSubject(name: string, category: string): Promise<{ ok: boolean; msg?: string }> {
    try { await call(http.post('/api/admin/subjects', { name, category })); return { ok: true } }
    catch (e: any) { return { ok: false, msg: e.message } }
  },

  // ---- 合同 ----
  async getContracts(): Promise<any[]> {
    return call<any[]>(http.get('/api/contracts'))
  },
  async purchase(teacherId: number, credits: number): Promise<{ ok: boolean; msg?: string }> {
    try { await call(http.post('/api/contracts', { teacherId, credits })); return { ok: true } }
    catch (e: any) { return { ok: false, msg: e.message } }
  },
  async signContract(id: number): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/contracts/${id}/sign`, {})); return { ok: true } }
    catch { return { ok: false } }
  },
}
