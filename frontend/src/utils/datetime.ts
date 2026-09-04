// 日期时间格式化 · 全站唯一事实来源
// 之前各视图各自实现 p2/fmt/range/dayLabel，口径不一（M月D日 vs M月D号、H:00 vs HH:mm），
// 收敛到本模块后：改一处，全站生效。

export const p2 = (n: number) => String(n).padStart(2, '0')

/** 周一=0 … 周日=6 */
export const WD_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const WD_SHORT = '日一二三四五六'

const toDate = (d: string | Date) => (typeof d === 'string' ? new Date(d) : d)
/** 后端 LocalDate 字符串（yyyy-MM-dd）按本地零点解析，避免 UTC 偏移 */
const parseDay = (dateStr: string) => new Date(dateStr + 'T00:00:00')

/** yyyy-MM-dd（供 <input type="date"> 使用） */
export function fmtDate(d: Date): string {
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`
}

/** HH:mm */
export function hm(d: string | Date): string {
  const dt = toDate(d)
  return `${p2(dt.getHours())}:${p2(dt.getMinutes())}`
}

/** M/D HH:mm */
export function mdHm(d: string | Date): string {
  const dt = toDate(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${hm(dt)}`
}

/** HH:mm-HH:mm */
export function timeRange(startAt: string, endAt: string): string {
  return `${hm(startAt)}-${hm(endAt)}`
}

/** M/D HH:mm-HH:mm */
export function mdHmRange(startAt: string, endAt: string): string {
  const a = toDate(startAt)
  return `${a.getMonth() + 1}/${a.getDate()} ${hm(a)}-${hm(endAt)}`
}

/** M月D号（入参 yyyy-MM-dd） */
export function dayLabel(dateStr: string): string {
  const d = parseDay(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}号`
}

/** M月D号 周X（入参 yyyy-MM-dd 或 Date） */
export function dayLabelWeek(d: string | Date): string {
  const dt = typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d) ? parseDay(d) : toDate(d)
  return `${dt.getMonth() + 1}月${dt.getDate()}号 周${WD_SHORT[dt.getDay()]}`
}

/** 后端时间可能带秒（HH:mm:ss），裁剪到 HH:mm */
export function trimHM(s: string): string {
  return String(s || '').slice(0, 5)
}
