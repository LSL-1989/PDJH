-- 用户表扩展：统一账户的手机号、登录场景、密码重置申请
-- 若已开启 spring.jpa.hibernate.ddl-auto=update，通常由 Hibernate 自动加列；生产可手工执行。

ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号，可选登录';
ALTER TABLE users ADD COLUMN login_scene VARCHAR(20) NULL COMMENT '登录场景 ADMIN/DOCTOR/NURSE/PHARMACY/LAB/EXAM';
ALTER TABLE users ADD COLUMN must_change_password INT NULL DEFAULT 0 COMMENT '1=须改密';
ALTER TABLE users ADD COLUMN password_reset_requested INT NULL DEFAULT 0 COMMENT '1=已申请重置';
ALTER TABLE users ADD COLUMN password_reset_request_time DATETIME NULL COMMENT '申请时间';

UPDATE users SET login_scene = 'ADMIN' WHERE login_scene IS NULL OR login_scene = '';
