// Mock API 层：镜像未来 Spring Boot REST 接口，便于 V0.1 演示，后续可无缝替换为真实后端。
import type { Booking, LeaveRequest, Teacher, Timeslot, User } from '../types'

const delay = (ms = 200) => new Promise((r) => setTimeout(r, ms))

// ---- 静态数据 ----
const teachers: Teacher[] = [
  { id: 1, name: '王老师', rating: 4.9, title: '钢琴十级', intro: '中央音乐学院毕业，10 年钢琴教学经验。', subjects: [{ id: 1, name: '钢琴' }] },
  { id: 2, name: '李老师', rating: 4.8, title: '羽毛球教练', intro: '前省队队员，擅长青少年启蒙。', subjects: [{ id: 2, name: '羽毛球' }] },
  { id: 3, name: '张老师', rating: 4.7, title: '编程讲师', intro: '一线工程师，主讲 Python / 前端入门。', subjects: [{ id: 3, name: '编程' }] },
]

// 生成未来 14 天、每天 18/19/20 点的时段
function genSlots(teacherId: number): Timeslot[] {
  const slots: Timeslot[] = []
  const base = new Date()
  for (let d = 1; d <= 14; d++) {
    for (const h of [18, 19, 20]) {
      const s = new Date(base)
      s.setDate(base.getDate() + d)
      s.setHours(h, 0, 0, 0)
      const e = new Date(s)
      e.setHours(h + 1)
      slots.push({ id: `${teacherId}-${s.getTime()}`, teacherId, startAt: s.toISOString(), endAt: e.toISOString(), status: 'available' })
    }
  }
  return slots
}
const slotStore = new Map<number, Timeslot[]>()
teachers.forEach((t) => slotStore.set(t.id, genSlots(t.id)))

// 预置一些已约时段，演示冲突
slotStore.get(1)!.slice(0, 2).forEach((s) => (s.status = 'booked'))

// ---- 会话 ----
let bookings: Booking[] = []
let leaves: LeaveRequest[] = []
let bookingSeq = 1
let leaveSeq = 1

export const api = {
  async login(phone: string, _password: string, role: 'student' | 'teacher'): Promise<User> {
    await delay()
    return { id: 100, role, name: role === 'student' ? '学员小约' : '老师', phone, credits: 12 }
  },

  async listTeachers(): Promise<Teacher[]> {
    await delay()
    return teachers
  },

  async getTeacher(id: number): Promise<Teacher | undefined> {
    await delay()
    return teachers.find((t) => t.id === id)
  },

  async listSlots(teacherId: number): Promise<Timeslot[]> {
    await delay()
    return slotStore.get(teacherId) ?? []
  },

  // 防冲突：同一老师同一开始时间仅一个活跃预约
  async createBooking(teacherId: number, slotId: string, student: User): Promise<{ ok: boolean; msg?: string; booking?: Booking }> {
    await delay(300)
    const slots = slotStore.get(teacherId) ?? []
    const slot = slots.find((s) => s.id === slotId)
    if (!slot) return { ok: false, msg: '时段不存在' }
    if (slot.status === 'booked') return { ok: false, msg: '该时段刚被占用，请选相邻时段' }
    if ((student.credits ?? 0) <= 0) return { ok: false, msg: '课时不足，请先购买课时包' }
    slot.status = 'booked'
    const t = teachers.find((x) => x.id === teacherId)!
    const b: Booking = { id: bookingSeq++, teacherId, teacherName: t.name, subjectName: t.subjects[0].name, startAt: slot.startAt, endAt: slot.endAt, status: 1 }
    bookings.unshift(b)
    return { ok: true, booking: b }
  },

  async listBookings(): Promise<Booking[]> {
    await delay()
    return bookings
  },

  async createLeave(bookingId: number, reason: string): Promise<{ ok: boolean }> {
    await delay()
    const b = bookings.find((x) => x.id === bookingId)
    if (!b) return { ok: false }
    b.status = 4
    leaves.unshift({ id: leaveSeq++, bookingId, reason, status: 1 })
    // 释放时段
    const slots = slotStore.get(b.teacherId) ?? []
    const s = slots.find((x) => x.startAt === b.startAt)
    if (s) s.status = 'available'
    return { ok: true }
  },

  async listLeaves(): Promise<LeaveRequest[]> {
    await delay()
    return leaves
  },
}
