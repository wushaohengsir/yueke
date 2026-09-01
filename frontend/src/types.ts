export type Role = 'guest' | 'student' | 'teacher' | 'admin'

export interface User {
  id: number
  role: Role
  name: string
  phone: string
  credits?: number // 学员剩余课时
}

export interface Subject {
  id: number
  name: string
}

export interface Teacher {
  id: number
  name: string
  rating: number
  title: string
  intro: string
  subjects: Subject[]
}

export interface Timeslot {
  id: string
  teacherId: number
  startAt: string // ISO
  endAt: string
  status: 'available' | 'booked'
}

export type BookingStatus = 0 | 1 | 2 | 3 | 4 // 待确认 已确认 已完成 已取消 已请假

export interface Booking {
  id: number
  teacherId: number
  teacherName: string
  subjectName: string
  startAt: string
  endAt: string
  status: BookingStatus
}

export interface LeaveRequest {
  id: number
  bookingId: number
  reason: string
  status: 0 | 1 | 2
}
