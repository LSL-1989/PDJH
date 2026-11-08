-- [已废弃] 早期原型 DDL，与当前 JPA 实体不一致，Spring Boot 不会自动执行本文件。
-- 建表请启动 pdjh-server（ddl-auto=update）；命名约定见 pdjh-server/SCHEMA_NAMING.md
-- ---------------------------------------------------------------------------

-- pdjh-server/src/main/resources/schema.sql
-- Initializes the fundamental tables for the hospital triage queuing matrix

CREATE TABLE IF NOT EXISTS patient (
    patient_code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    gender VARCHAR(10),
    age INT,
    priority_level INT DEFAULT 0 COMMENT '0: normal, 50: high, 99: emergency'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS queue_entry (
    queue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_code VARCHAR(64) NOT NULL,
    dept_id VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'WAITING',
    is_appointment BOOLEAN DEFAULT FALSE,
    wait_start_time DATETIME NOT NULL,
    INDEX idx_dept_state (dept_id, state),
    FOREIGN KEY (patient_code) REFERENCES patient(patient_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_dependency (
    item_code VARCHAR(64) PRIMARY KEY,
    requires_fasting BOOLEAN DEFAULT FALSE,
    pre_requisite_item_code VARCHAR(64) NULL,
    FOREIGN KEY (pre_requisite_item_code) REFERENCES exam_dependency(item_code) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS integration_endpoint (
    endpoint_id VARCHAR(64) PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL,
    env_mode INT DEFAULT 0 COMMENT '0: PROD, 1: MOCK',
    mapping_template JSON
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id VARCHAR(64) PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) COMMENT '角色编码',
    is_delete BOOLEAN DEFAULT FALSE COMMENT '是否禁用：false=启用，true=禁用',
    create_date_time DATETIME COMMENT '创建时间',
    remark VARCHAR(500) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id VARCHAR(64) NOT NULL COMMENT '角色ID',
    menu_id VARCHAR(64) NOT NULL COMMENT '菜单ID',
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化默认角色数据
INSERT IGNORE INTO sys_role (role_id, role_name, role_code, is_delete, create_date_time, remark) VALUES
    ('admin', '系统管理员', 'ROLE_ADMIN', FALSE, NOW(), '系统管理员，拥有所有权限'),
    ('director', '主任', 'ROLE_DIRECTOR', FALSE, NOW(), '科室主任'),
    ('doctor', '医生', 'ROLE_DOCTOR', FALSE, NOW(), '医生'),
    ('nurse', '护士', 'ROLE_NURSE', FALSE, NOW(), '护士');
