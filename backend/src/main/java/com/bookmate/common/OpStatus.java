package com.bookmate.common;

/**
 * 业务操作结果码：替代原先在 Service/Controller 间传递的魔法字符串
 * （"ok"/"not_found"/"bad_time"/"conflict"/"enabled"/"not_time"），编译期可查、不重不漏。
 */
public enum OpStatus {
    OK,
    NOT_FOUND,   // 资源不存在或非本人资源
    NOT_TIME,    // 课程尚未结束，不可登记完成
    BAD_TIME,    // 结束时间未晚于开始时间
    CONFLICT,    // 与已启用模板时间重叠
    ENABLED      // 模板已启用，须先停用
}
