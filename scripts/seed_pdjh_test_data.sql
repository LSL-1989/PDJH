-- =============================================================================
-- pdjh 测试数据种子脚本（表/列名下划线，与 pdjh-server/SCHEMA_NAMING.md 一致）
-- 使用方式：
--   1) 确认库名（示例为 pdjh），与 application.yml 中 JDBC 一致
--   2) mysql -h主机 -u用户 -p 库名 < scripts/seed_pdjh_test_data.sql
--   3) 若表已由 Hibernate ddl-auto=update 建过，直接执行即可；若列名不一致请先启动一次后端生成表结构再对照调整
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------- 清理（便于重复灌数；首次空库可保留）
DELETE FROM exam_record_detail WHERE patient_code IN ('P_EXAM_001', 'P241101', 'P241102', 'P241103');
DELETE FROM dict_exam_item WHERE id IN (9001, 9002);
DELETE FROM screen_dept_mapping WHERE screen_id = 'SCR001';
DELETE FROM screen_room_mapping WHERE screen_id = 'SCR001';
DELETE FROM sign_room_mapping WHERE sign_device_id IN ('SIGN_DEV001', 'SIGN_DEV_LAB');
DELETE FROM sign_device WHERE id IN ('SIGN_DEV001', 'SIGN_DEV_LAB');
DELETE FROM office_room WHERE id IN ('ROOM001', 'ROOM002', 'ROOM_LAB001', 'ROOM_LAB002');
DELETE FROM call_list WHERE patient_code IN ('P_EXAM_001', 'P241101', 'P241102', 'P241103', 'P_LAB_001');
DELETE FROM doctor WHERE id IN ('DOC001', 'DOC_LAB');
DELETE FROM doc_screen WHERE id = 'SCR001';
DELETE FROM sys_menu WHERE id LIKE 'SEED_%';
DELETE FROM sys_role_menu WHERE role_id IN ('admin', 'seed_doctor');
DELETE FROM sys_role WHERE role_id IN ('admin', 'seed_doctor');
DELETE FROM users WHERE user_no IN ('admin', 'wuaitecadmin', 'doctor01');
DELETE FROM department WHERE id IN ('DEPT001', 'DEPT002', 'DEPT003', 'DEPT004');

-- ----------------------------------------------------------------------------- 科室
INSERT INTO department (id, name, type, business_type, state, father_id, queue, is_suspended, med_tech_sub_type) VALUES
('DEPT001', '内科门诊', 1, 'OUTPATIENT', 1, '0', 'NKQ', 0, NULL),
('DEPT002', '医学检验科', 2, 'LAB', 1, '0', 'JYQ', 0, 'LAB'),
('DEPT003', '超声医学科', 2, 'ULTRASOUND', 1, '0', 'CSQ', 0, 'ULTRASOUND'),
('DEPT004', '健康管理中心', 1, 'HEALTH_CHECKUP', 1, '0', 'TJQ', 0, NULL);

-- ----------------------------------------------------------------------------- 菜单（与 HJ管理后台 constantRoutes 一致：一级 page_url=父路由 name，二级=子 path）
INSERT INTO sys_menu (id, page_show_name, page_url, icon, level_id, parent_page_id, always_show, no_cache, role_codes, sort_order) VALUES
('SEED_M010', '首页', 'dashboard', 'Home', 0, '0', 1, 0, NULL, 10),
('SEED_M020', '主任工作台', 'directorDesk', 'el-icon-s-custom', 0, '0', 1, 0, NULL, 20),
('SEED_M021', '门诊主任工作台', 'outpatientDesk', 'el-icon-menu', 1, 'SEED_M020', 1, 0, NULL, 21),
('SEED_M022', '药房主任工作台', 'pharmacyDesk', 'el-icon-menu', 1, 'SEED_M020', 1, 0, NULL, 22),
('SEED_M023', '体检中心主任工作台', 'examDesk', 'el-icon-menu', 1, 'SEED_M020', 1, 0, NULL, 23),
('SEED_M024', '检验主任工作台', 'labDesk', 'el-icon-menu', 1, 'SEED_M020', 1, 0, NULL, 24),
('SEED_M025', '医技主任工作台', 'medtechDesk', 'el-icon-menu', 1, 'SEED_M020', 1, 0, NULL, 25),
('SEED_M026', '全院排队统计分析', 'statistics', 'el-icon-s-data', 1, 'SEED_M020', 1, 0, NULL, 26),
('SEED_M030', '设备与屏幕', 'deviceManage', 'el-icon-monitor', 0, '0', 1, 0, NULL, 30),
('SEED_M031', '屏幕管理', 'ScreenManages', 'el-icon-menu', 1, 'SEED_M030', 1, 0, NULL, 31),
('SEED_M032', '编号规则管理', 'AutoNumManages', 'el-icon-menu', 1, 'SEED_M030', 1, 0, NULL, 32),
('SEED_M040', '大屏显示内容配置', 'screenConfig', 'el-icon-video-camera', 0, '0', 1, 0, NULL, 40),
('SEED_M041', '大屏显示内容配置', 'screenConfigIndex', 'el-icon-menu', 1, 'SEED_M040', 1, 0, NULL, 41),
('SEED_M050', '院内业务树', 'hospitalInfo', 'el-icon-office-building', 0, '0', 1, 0, NULL, 50),
('SEED_M051', '科室管理', 'DeptManages', 'el-icon-menu', 1, 'SEED_M050', 1, 0, NULL, 51),
('SEED_M052', '物理诊室与终端资源', 'OfficeRoomManages', 'el-icon-menu', 1, 'SEED_M050', 1, 0, NULL, 52),
('SEED_M053', '医生管理', 'DoctorProfile', 'el-icon-menu', 1, 'SEED_M050', 1, 0, NULL, 53),
('SEED_M060', '业务配置中心', 'businessConfig', 'el-icon-s-tools', 0, '0', 1, 0, NULL, 60),
('SEED_M061', '叫号规则配置中心', 'ruleCenter', 'el-icon-menu', 1, 'SEED_M060', 1, 0, NULL, 61),
('SEED_M062', '出诊排班管理', 'doctorSchedule', 'el-icon-menu', 1, 'SEED_M060', 1, 0, NULL, 62),
('SEED_M065', '护士当班排班', 'nurseSchedule', 'el-icon-menu', 1, 'SEED_M060', 1, 0, NULL, 65),
('SEED_M063', '体检套餐管理', 'examPackage', 'el-icon-menu', 1, 'SEED_M060', 1, 0, NULL, 63),
('SEED_M064', '医技预约时段配置', 'medtechAppointment', 'el-icon-menu', 1, 'SEED_M060', 1, 0, NULL, 64),
('SEED_M070', '集成网关管理', 'systemIntegration', 'el-icon-connection', 0, '0', 1, 0, NULL, 70),
('SEED_M071', '数据源接口配置', 'integrationConfig', 'el-icon-menu', 1, 'SEED_M070', 1, 0, NULL, 71),
('SEED_M072', '运维护航沙盘', 'integrationDashboard', 'el-icon-odometer', 1, 'SEED_M070', 1, 0, NULL, 72),
('SEED_M073', '外部网关审计查询', 'auditLog', 'el-icon-menu', 1, 'SEED_M070', 1, 0, NULL, 73),
('SEED_M074', '业务事件日志轨迹', 'systemLog', 'el-icon-menu', 1, 'SEED_M070', 1, 0, NULL, 74),
('SEED_M080', '业务实施中心', 'implementation', 'el-icon-guide', 0, '0', 1, 0, NULL, 80),
('SEED_M081', '业务实施中心', 'wizard', 'el-icon-menu', 1, 'SEED_M080', 1, 0, NULL, 81),
('SEED_M090', '系统与权限管理', 'userManage', 'el-icon-s-operation', 0, '0', 1, 0, NULL, 90),
('SEED_M091', '全局字典参数', 'systemConfig', 'el-icon-menu', 1, 'SEED_M090', 1, 0, NULL, 91),
('SEED_M092', '用户管理', 'maintenanceUser', 'user', 1, 'SEED_M090', 1, 0, NULL, 92),
('SEED_M093', '角色管理', 'maintenanceRole', 'peoples', 1, 'SEED_M090', 1, 0, NULL, 93),
('SEED_M094', '菜单管理', 'maintenanceMenu', 'tree-table', 1, 'SEED_M090', 1, 0, NULL, 94);

-- ----------------------------------------------------------------------------- 角色与菜单授权（登录按 sys_role_menu 下发 Routes，见 pdjh-server/RBAC.md）
INSERT INTO sys_role (role_id, role_name, role_code, is_delete, create_date_time, remark) VALUES
('admin', '系统管理员', 'ROLE_ADMIN', 0, NOW(), '种子：全部 SEED 菜单'),
('seed_doctor', '门诊医生', 'ROLE_DOCTOR', 0, NOW(), '种子：仅首页');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 'admin', id FROM sys_menu WHERE id LIKE 'SEED_%';

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
('seed_doctor', 'SEED_M010');

-- ----------------------------------------------------------------------------- 后台用户（超管 wuaitecadmin 由 AdminBootstrapRunner 以 BCrypt 写入，勿在 SQL 中写明文密码）
INSERT INTO users (user_no, user_name, pass_word, create_date, state, dept_id, token, token_time, role, sign_code, role_id, login_scene) VALUES
('doctor01', '门诊医生甲', 'Doctor@2026', NOW(), 1, 'DEPT001', NULL, NULL, 'ROLE_DOCTOR', 'SIGN_DOC01', 'seed_doctor', 'DOCTOR');

-- ----------------------------------------------------------------------------- 物理诊室
INSERT INTO office_room (id, name, dept_id, floor, room_no, screen_id, voice_device_id, state, business_type) VALUES
('ROOM001', '内科一诊室', 'DEPT001', '1F', '101', 'SCR001', NULL, 1, 'OUTPATIENT'),
('ROOM002', '内科二诊室', 'DEPT001', '1F', '102', NULL, NULL, 1, 'OUTPATIENT'),
('ROOM_LAB001', '采血1窗', 'DEPT002', '2F', '201', NULL, NULL, 1, 'LAB'),
('ROOM_LAB002', '采血2窗', 'DEPT002', '2F', '202', NULL, NULL, 1, 'LAB');

-- ----------------------------------------------------------------------------- 大屏 + 诊室映射
INSERT INTO doc_screen (id, name, state, create_date, call_code, device_code) VALUES
('SCR001', '一楼候诊综合屏', 1, NOW(), NULL, 'TERMINAL_HALL_01');

INSERT INTO screen_room_mapping (screen_id, office_room_id) VALUES
('SCR001', 'ROOM001'),
('SCR001', 'ROOM002');

INSERT INTO screen_dept_mapping (screen_id, dept_id) VALUES
('SCR001', 'DEPT001');

-- ----------------------------------------------------------------------------- 签到终端 + 诊室绑定
INSERT INTO sign_device (id, device_code, name, title, location, show_text, status, create_date) VALUES
('SIGN_DEV001', 'SIGN-NK-01', '内科签到机1号', '内科候诊区签到', '1F 内科候诊区', '请扫码或输入签到码', 1, NOW());

INSERT INTO sign_room_mapping (sign_device_id, office_room_id) VALUES
('SIGN_DEV001', 'ROOM001'),
('SIGN_DEV001', 'ROOM002');

INSERT INTO sign_device (id, device_code, name, title, location, show_text, status, create_date) VALUES
('SIGN_DEV_LAB', 'SIGN-JY-01', '检验签到机1号', '检验采血签到', '2F 采血大厅', '请扫码分配采血窗口', 1, NOW());

INSERT INTO sign_room_mapping (sign_device_id, office_room_id) VALUES
('SIGN_DEV_LAB', 'ROOM_LAB001'),
('SIGN_DEV_LAB', 'ROOM_LAB002');

-- ----------------------------------------------------------------------------- 医生（HJ医生工作站 URL 可用 ?deptId=DEPT001&deviceCode=TERMINAL_HALL_01）
INSERT INTO doctor (id, work_no, name, gender, title, dept_id, screen_id, office_name, is_on_duty, avatar, remarks, specialty, display_status, create_date) VALUES
('DOC001', 'D001', '李医生', '男', '主治医师', 'DEPT001', 'SCR001', '内科一诊室', 1, NULL, '擅长常见病诊治', '内科常见病', 1, NOW()),
('DOC_LAB', 'L001', '王技师', '女', '主管技师', 'DEPT002', NULL, '采血1窗', 1, NULL, '检验采血', '静脉采血', 1, NOW());

-- ----------------------------------------------------------------------------- 体检字典 + 明细（HJ体检H5：患者编号 P_EXAM_001）
INSERT INTO dict_exam_item (id, item_name, associated_dept_id, is_fasting, gender_limit, avg_time_minutes, is_enabled, depends_on_ids) VALUES
(9001, '血常规', 'DEPT002', 0, 0, 12, 1, NULL),
(9002, '腹部彩超', 'DEPT003', 1, 0, 25, 1, NULL);

INSERT INTO exam_record_detail (patient_code, exam_batch_no, item_id, state, enqueue_time, complete_time) VALUES
('P_EXAM_001', 'BATCH20260511', 9001, 0, NOW(), NULL),
('P_EXAM_001', 'BATCH20260511', 9002, 3, NOW(), NOW());

-- ----------------------------------------------------------------------------- 排队主表（列名按 Spring Boot 默认 CamelCase → snake_case）
-- P241101：待首诊签到（HJ自助签到终端 手动输入或 ?patientCode=P241101）
-- P241102：已进等候池，供医生站/大屏拉队
-- P241103：复诊通道 REVISIT
INSERT INTO call_list (
  patient_code, patient_name, sflbbigtype, call_time, sflbmc, xh, xhlb, show_no,
  old_dept_id, dept_id, out_patient_office, office_room_id, doctors, remark, state, pym, call_date, end_date,
  doctor_id, zcbz, is_forzen, sex, age_str,
  is_appointment, appointment_start_time, appointment_end_time, start_time,
  triage_state, call_count, check_in_time, source_type
) VALUES
(
  'P241101', '张三', 'MZ', NOW(), '普通门诊', 10, '普', 'A101',
  NULL, 'DEPT001', '内科一诊室', 'ROOM001', '李医生', NULL, 0, 'zs', CURDATE(), NULL,
  'DOC001', NULL, 0, '男', '35岁',
  0, NULL, NULL, NULL,
  'UNARRIVED', 0, NULL, 'CLINIC'
),
(
  'P241102', '李四', 'MZ', NOW(), '普通门诊', 20, '普', 'A102',
  NULL, 'DEPT001', '内科一诊室', 'ROOM001', '李医生', NULL, 0, 'ls', CURDATE(), NULL,
  'DOC001', NULL, 0, '女', '42岁',
  0, NULL, NULL, NULL,
  'WAITING', 0, NOW(), 'CLINIC'
),
(
  'P241103', '王五', 'MZ', NOW(), '专家门诊', 5, '专', 'A103',
  NULL, 'DEPT001', '内科二诊室', 'ROOM002', '李医生', NULL, 0, 'ww', CURDATE(), NULL,
  'DOC001', NULL, 0, '男', '58岁',
  1, '08:00', '08:30', NULL,
  'REVISIT', 1, NULL, 'CLINIC'
);

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 联调提示：
--   签到 H5：http://localhost:5174/?deviceId=SIGN-NK-01  患者 P241101
--   大屏 H5：deviceCode=TERMINAL_HALL_01 绑定 ROOM001/ROOM002
--   体检 H5：在页面中填患者编号 P_EXAM_001
--   医生/大屏队列：GetDocCallList ?deptid=DEPT001
--   管理后台登录：wuaitecadmin / wuaitec@123（密码由启动器 BCrypt 入库，本脚本不写超管）
-- =============================================================================
