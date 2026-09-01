-- ============================================================
-- 通用师生约课平台（BookMate） MySQL 8.0 建表脚本
-- 设计范式：1NF -> 2NF -> 3NF -> BCNF；M:N 一律 Junction 表
-- 字符集：utf8mb4；引擎：InnoDB；金额/课时用整数
-- ============================================================
CREATE DATABASE IF NOT EXISTS `yueke` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `yueke`;

-- ---------- 用户与角色（单表 + 角色枚举，1:1 扩展档案） ----------
CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role`          TINYINT NOT NULL DEFAULT 0 COMMENT '0游客占位1学员2老师3管理员',
  `phone`         VARCHAR(20) NOT NULL COMMENT '手机号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  `name`          VARCHAR(50) NOT NULL COMMENT '姓名/昵称',
  `avatar_url`    VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `status`        TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用1正常',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户';

CREATE TABLE `teacher_profile` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '老师用户ID',
  `title`        VARCHAR(50) DEFAULT NULL COMMENT '职称',
  `intro`        TEXT COMMENT '个人简介',
  `rating`       DECIMAL(2,1) NOT NULL DEFAULT 5.0 COMMENT '评分',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核1通过2驳回',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_user` (`user_id`),
  CONSTRAINT `fk_tp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='老师档案(1:1)';

CREATE TABLE `student_profile` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '学员用户ID',
  `credits_total`   INT NOT NULL DEFAULT 0 COMMENT '[废弃]通用课时，已改为分课程课时见 student_credit',
  `credits_used`    INT NOT NULL DEFAULT 0 COMMENT '[废弃]通用课时，已改为分课程课时见 student_credit',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_user` (`user_id`),
  CONSTRAINT `fk_sp_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='学员档案(1:1)；课时按课程拆分见 student_credit';

-- ---------- 科目与老师-科目（M:N Junction） ----------
CREATE TABLE `subject` (
  `id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`     VARCHAR(50) NOT NULL COMMENT '科目名',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_name` (`name`)
) ENGINE=InnoDB COMMENT='科目';

CREATE TABLE `teacher_subject` (
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `subject_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`teacher_id`,`subject_id`),
  CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher_profile` (`user_id`),
  CONSTRAINT `fk_ts_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`)
) ENGINE=InnoDB COMMENT='老师-科目 M:N';

-- ---------- 学员分课程课时（课时不通用，每门课独立） ----------
CREATE TABLE `student_credit` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id`    BIGINT UNSIGNED NOT NULL COMMENT '学员用户ID',
  `subject_id`    BIGINT UNSIGNED NOT NULL COMMENT '科目ID',
  `credits_total` INT NOT NULL DEFAULT 0 COMMENT '该课程购买课时',
  `credits_used`  INT NOT NULL DEFAULT 0 COMMENT '该课程已消耗课时',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_subject` (`student_id`,`subject_id`),
  CONSTRAINT `fk_sc_student` FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`user_id`),
  CONSTRAINT `fk_sc_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`)
) ENGINE=InnoDB COMMENT='学员分课程课时（不通用）';

-- ---------- 时段模板与屏蔽 ----------
CREATE TABLE `timeslot_template` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `weekday`    TINYINT NOT NULL COMMENT '1-7',
  `start_time` TIME NOT NULL,
  `end_time`   TIME NOT NULL,
  `subject_id` BIGINT UNSIGNED DEFAULT NULL,
  `enabled`    TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_tt_teacher` (`teacher_id`),
  CONSTRAINT `fk_tt_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher_profile` (`user_id`)
) ENGINE=InnoDB COMMENT='每周可授时段模板';

CREATE TABLE `timeslot_block` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `block_date` DATE NOT NULL COMMENT '屏蔽/加开日期',
  `type`       TINYINT NOT NULL COMMENT '0屏蔽1加开',
  `start_time` TIME DEFAULT NULL COMMENT '加开时填',
  `end_time`   TIME DEFAULT NULL,
  `reason`     VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tb_teacher_date` (`teacher_id`,`block_date`),
  CONSTRAINT `fk_tb_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher_profile` (`user_id`)
) ENGINE=InnoDB COMMENT='特殊日期屏蔽/加开';

-- ---------- 预约单（核心，并发防冲突） ----------
CREATE TABLE `booking` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teacher_id`  BIGINT UNSIGNED NOT NULL,
  `student_id`  BIGINT UNSIGNED NOT NULL,
  `subject_id`  BIGINT UNSIGNED DEFAULT NULL,
  `start_at`    DATETIME NOT NULL COMMENT '开始时间',
  `end_at`      DATETIME NOT NULL COMMENT '结束时间',
  `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认1已确认2已完成3已取消4已请假',
  `active_flag` TINYINT GENERATED ALWAYS AS (IF(`status` IN (0,1),1,NULL)) STORED COMMENT '活跃标记，配合唯一索引防并发',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_active_slot` (`teacher_id`,`start_at`,`active_flag`),
  KEY `idx_b_student` (`student_id`),
  CONSTRAINT `fk_b_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher_profile` (`user_id`),
  CONSTRAINT `fk_b_student` FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`user_id`)
) ENGINE=InnoDB COMMENT='预约单';

-- ---------- 请假申请 ----------
CREATE TABLE `leave_request` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `booking_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `reason`     VARCHAR(255) NOT NULL,
  `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批1批准2驳回',
  `handled_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人(老师/管理员)',
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lr_booking` (`booking_id`),
  CONSTRAINT `fk_lr_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`)
) ENGINE=InnoDB COMMENT='请假申请';

-- ---------- 课时流水（不存派生值，流水可审计） ----------
CREATE TABLE `credit_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id`  BIGINT UNSIGNED NOT NULL,
  `subject_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '科目ID（分课程课时）',
  `delta`       INT NOT NULL COMMENT '正=充值/返还 负=消耗',
  `reason`      VARCHAR(50) NOT NULL COMMENT '约课消耗/请假返还/购买充值/补登记',
  `ref_booking` BIGINT UNSIGNED DEFAULT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cl_student` (`student_id`),
  CONSTRAINT `fk_cl_student` FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`user_id`)
) ENGINE=InnoDB COMMENT='课时流水';

-- ---------- 合同 ----------
CREATE TABLE `contract` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id`    BIGINT UNSIGNED NOT NULL,
  `teacher_id`    BIGINT UNSIGNED NOT NULL,
  `total_credits` INT NOT NULL COMMENT '课时包总课时',
  `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '0待签署1生效2结束',
  `signed_at`     DATETIME DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_c_student` (`student_id`),
  CONSTRAINT `fk_c_student` FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`user_id`),
  CONSTRAINT `fk_c_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher_profile` (`user_id`)
) ENGINE=InnoDB COMMENT='课时包合同';

-- ---------- 商品与订单 ----------
CREATE TABLE `product` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `price_cents` INT NOT NULL COMMENT '价格(分)',
  `stock`       INT NOT NULL DEFAULT 0,
  `status`      TINYINT NOT NULL DEFAULT 1 COMMENT '0下架1上架',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='商品';

CREATE TABLE `order` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id`  BIGINT UNSIGNED NOT NULL,
  `product_id`  BIGINT UNSIGNED NOT NULL,
  `qty`         INT NOT NULL DEFAULT 1,
  `amount_cents` INT NOT NULL COMMENT '总额(分)',
  `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付1完成2取消',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_o_student` (`student_id`),
  CONSTRAINT `fk_o_student` FOREIGN KEY (`student_id`) REFERENCES `student_profile` (`user_id`),
  CONSTRAINT `fk_o_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB COMMENT='商品订单';

-- ---------- 通知 ----------
CREATE TABLE `notification` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `type`       VARCHAR(30) NOT NULL COMMENT 'booking/leave/audit 等',
  `content`    VARCHAR(255) NOT NULL,
  `read_flag`  TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_n_user` (`user_id`),
  CONSTRAINT `fk_n_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='站内通知';
