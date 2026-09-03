-- 集成测试数据（在 yueke_test 库上执行；@Sql 每次测试前清理重插）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE credit_log;
TRUNCATE TABLE leave_request;
TRUNCATE TABLE booking;
TRUNCATE TABLE contract;
TRUNCATE TABLE timeslot_template;
TRUNCATE TABLE timeslot_block;
TRUNCATE TABLE student_credit;
TRUNCATE TABLE teacher_subject;
TRUNCATE TABLE subject;
TRUNCATE TABLE teacher_profile;
TRUNCATE TABLE student_profile;
TRUNCATE TABLE `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- 老师（id=1，王老师）
INSERT INTO `user` (id, role, phone, password_hash, name, status) VALUES
  (1, 2, '13800000001', '$2a$10$abcdefghijklmnopqrstuv', '王老师', 1);
INSERT INTO teacher_profile (id, user_id, title, intro, rating, audit_status) VALUES
  (1, 1, '钢琴十级', '测试老师', 4.9, 1);

-- 学员（id=2，学员小约）
INSERT INTO `user` (id, role, phone, password_hash, name, status) VALUES
  (2, 1, '13800000000', '$2a$10$abcdefghijklmnopqrstuv', '学员小约', 1);
INSERT INTO student_profile (id, user_id, credits_total, credits_used) VALUES
  (1, 2, 0, 0);

-- 科目
INSERT INTO subject (id, name, category) VALUES (1, '钢琴', '音乐');
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES (1, 1);

-- 时段模板：周一(weekday=1) 18:00-19:00，默认停用(enabled=0)
INSERT INTO timeslot_template (id, teacher_id, weekday, start_time, end_time, subject_id, enabled) VALUES
  (1, 1, 1, '18:00:00', '19:00:00', 1, 0),
  (2, 1, 1, '18:00:00', '20:00:00', 1, 0);

-- 学员分课程课时
INSERT INTO student_credit (id, student_id, subject_id, credits_total, credits_used) VALUES
  (1, 2, 1, 10, 0);
