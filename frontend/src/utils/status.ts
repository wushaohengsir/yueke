// 状态文案/样式映射 · 全站唯一事实来源
// 各视图不再各自维护 stText/stClass 平行数组（易错位），统一用 { text, cls, dot } 表驱动。

export interface StatusMeta {
  text: string
  cls: string // 对应 style.css 的 .st.s1~s4
  dot?: string // 时间轴圆点色
}

/** 预约状态：0 待确认 / 1 已确认 / 2 已完成 / 3 已取消 / 4 已请假 */
export const BOOKING_STATUS: StatusMeta[] = [
  { text: '待确认', cls: 's1', dot: 'var(--blue)' },
  { text: '已确认', cls: 's1', dot: 'var(--blue)' },
  { text: '已完成', cls: 's2', dot: 'var(--green)' },
  { text: '已取消', cls: 's3', dot: '#ccc' },
  { text: '已请假', cls: 's4', dot: 'var(--sun)' },
]

/** 老师周课表时段状态（后端字符串枚举） */
export const SCHEDULE_STATUS: Record<string, StatusMeta> = {
  free: { text: '未预约', cls: 's3' },
  booked: { text: '已预约', cls: 's1' },
  completed: { text: '已完成', cls: 's2' },
}

/** 老师审核状态：0 待审核 / 1 已通过 / 2 已驳回 */
export const AUDIT_STATUS: StatusMeta[] = [
  { text: '待审核', cls: 's4' },
  { text: '已通过', cls: 's2' },
  { text: '已驳回', cls: 's3' },
]

/** 合同状态：0 待签署 / 1 已生效 / 2 已结束 */
export const CONTRACT_STATUS: StatusMeta[] = [
  { text: '待签署', cls: 's4' },
  { text: '已生效', cls: 's2' },
  { text: '已结束', cls: 's3' },
]

/** 请假审批状态：0 待审批 / 1 已批准 / 2 已驳回 */
export const LEAVE_STATUS: StatusMeta[] = [
  { text: '待审批', cls: 's1' },
  { text: '已批准', cls: 's4' },
  { text: '已驳回', cls: 's3' },
]

/** 用户角色：1 学员 / 2 老师 / 3 管理员 */
export const ROLE_TEXT = ['', '学员', '老师', '管理员']
