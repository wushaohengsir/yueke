// 真实后端 API（Spring Boot :8080），Axios + JWT 拦截器
import axios from 'axios'
import type { Booking, Credit, Teacher, Timeslot, User } from '../types'

const http = axios.create({ baseURL: 'http://localhost:8080', timeout: 10000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

async function call<T>(p: Promise<any>): Promise<T> {
  const res = await p
  const body = res.data
  if (body.code !== 0) throw Object.assign(new Error(body.msg), { code: body.code })
  return body.data as T
}

interface RawTeacher { id: number; name: string; title: string; intro: string; rating: number; subjects: string[] }

export const api = {
  async login(phone: string, _password: string, role: 'student' | 'teacher'): Promise<User> {
    const data = await call<any>(http.post('/api/auth/login', { phone, password: '123456', role: role === 'student' ? 1 : 2 }))
    localStorage.setItem('token', data.token)
    return { id: data.userId, role, name: data.name, phone, credits: 20 }
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
  async completeBooking(id: number): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/teacher/bookings/${id}/complete`, {})); return { ok: true } }
    catch { return { ok: false } }
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
  async toggleTemplate(id: number): Promise<{ ok: boolean }> {
    try { await call(http.post(`/api/teacher/templates/${id}/toggle`, {})); return { ok: true } }
    catch { return { ok: false } }
  },
}
