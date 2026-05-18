-- 1. 项目表
CREATE TABLE `t_project` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `description` VARCHAR(255) COMMENT '项目描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0:正常, 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目基础信息表';

-- 2. 环境配置表
CREATE TABLE `t_environment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '关联项目ID',
    `env_name` VARCHAR(50) NOT NULL COMMENT '环境名称(如:开发、测试)',
    `base_url` VARCHAR(255) NOT NULL COMMENT '根路径(如: http://api.test.com)',
    `global_headers` TEXT COMMENT '全局请求头(JSON格式)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多环境配置表';

-- 3. 接口定义表 (资产库)
CREATE TABLE `t_api_definition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '所属项目ID',
    `name` VARCHAR(100) NOT NULL COMMENT '接口名称',
    `method` VARCHAR(10) NOT NULL COMMENT '请求方法(GET, POST, PUT, DELETE)',
    `path` VARCHAR(255) NOT NULL COMMENT '接口路径(不包含域名)',
    `request_headers` TEXT COMMENT '默认请求头',
    `request_body` LONGTEXT COMMENT '默认请求体',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口基础定义表';

-- 4. 测试用例/业务流表
CREATE TABLE `t_test_case` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '所属项目ID',
    `case_name` VARCHAR(100) NOT NULL COMMENT '用例名称/业务流名称',
    `description` VARCHAR(255) COMMENT '业务流描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例/业务流主表';

-- 5. 测试步骤表 (核心编排)
CREATE TABLE `t_test_step` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `case_id` BIGINT NOT NULL COMMENT '所属用例ID',
    `api_id` BIGINT NOT NULL COMMENT '引用接口ID',
    `step_order` INT NOT NULL COMMENT '执行顺序(从小到大)',
    `step_name` VARCHAR(100) COMMENT '步骤别名',
    `override_request_body` LONGTEXT COMMENT '覆盖接口定义的请求体(支持变量引用)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_case_id` (`case_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例步骤编排表';

-- 6. 变量提取规则表
CREATE TABLE `t_variable_extract` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `step_id` BIGINT NOT NULL COMMENT '关联步骤ID',
    `variable_name` VARCHAR(50) NOT NULL COMMENT '变量名(如: token)',
    `json_path` VARCHAR(255) NOT NULL COMMENT 'JSONPath 表达式',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口返回结果变量提取配置表';

-- 7. 断言规则表
CREATE TABLE `t_assertion` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `step_id` BIGINT NOT NULL COMMENT '关联步骤ID',
    `assert_type` VARCHAR(20) NOT NULL COMMENT '断言类型(STATUS_CODE, JSON_PATH, DATABASE)',
    `expression` VARCHAR(255) NOT NULL COMMENT '断言表达式(如 JSONPath 或 SQL)',
    `expected_value` TEXT NOT NULL COMMENT '期望值',
    `operator` VARCHAR(20) NOT NULL DEFAULT 'EQUALS' COMMENT '比较符(EQUALS, CONTAINS, GREATER_THAN)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_step_id` (`step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口自动断言规则表';

-- 8. 执行报告记录表
CREATE TABLE `t_execution_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `case_id` BIGINT NOT NULL COMMENT '执行的用例ID',
    `env_id` BIGINT NOT NULL COMMENT '执行环境ID',
    `status` VARCHAR(20) NOT NULL COMMENT '最终结果(PASS, FAIL, ERROR)',
    `total_steps` INT DEFAULT 0 COMMENT '总步骤数',
    `passed_steps` INT DEFAULT 0 COMMENT '通过步骤数',
    `start_time` DATETIME COMMENT '开始执行时间',
    `end_time` DATETIME COMMENT '结束时间',
    `executor` VARCHAR(50) COMMENT '执行人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行报告主表';

-- 9. 执行明细表 (用于 ECharts 钻取数据)
CREATE TABLE `t_execution_detail` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `report_id` BIGINT NOT NULL COMMENT '关联报告ID',
    `step_id` BIGINT NOT NULL COMMENT '关联步骤ID',
    `actual_request` LONGTEXT COMMENT '实际发送请求(含变量置换后)',
    `actual_response` LONGTEXT COMMENT '实际响应结果',
    `assert_result` TEXT COMMENT '断言结果详情(JSON存储)',
    `status` VARCHAR(20) NOT NULL COMMENT '单步结果(PASS, FAIL)',
    `elapsed_ms` LONG COMMENT '响应耗时(毫秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行报告明细/步骤结果表';
-- 10. 用户信息表
CREATE TABLE `t_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号(唯一)',
    `password` VARCHAR(100) NOT NULL COMMENT '登录密码(密文存储)',
    `nickname` VARCHAR(50) COMMENT '用户昵称/展示名',
    `email` VARCHAR(100) COMMENT '邮箱地址(可用于找回密码或接收通知)',
    `avatar` VARCHAR(255) COMMENT '用户头像URL',
    `status` TINYINT(1) DEFAULT 1 COMMENT '账号状态(1:正常, 0:停用)',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0:正常, 1:删除)',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户信息表';
-- 修改项目表，增加创建人字段，用于数据隔离（用户登录后只能看到自己创建的项目）
ALTER TABLE `t_project` 
ADD COLUMN `create_by` BIGINT NOT NULL COMMENT '创建人(关联 t_user.id)' AFTER `description`;

-- 可选：为 create_by 增加普通索引，提升查询自己项目列表时的性能
ALTER TABLE `t_project` ADD INDEX `idx_create_by` (`create_by`);

-- ============================================================
-- M1 接口管理模块补丁 2026-05-10
-- 为 t_api_definition 追加 create_by 与 is_deleted 字段：
--   1. create_by 冗余存储创建人，避免每次查接口都 join t_project
--   2. is_deleted 与 t_project 风格一致，采用逻辑删除
-- ============================================================
ALTER TABLE `t_api_definition`
    ADD COLUMN `create_by` BIGINT NOT NULL COMMENT '创建人(关联 t_user.id)' AFTER `project_id`;

ALTER TABLE `t_api_definition`
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常, 1:删除)';

-- ============================================================
-- M2 测试用例模块补丁 2026-05-10
-- 为 t_test_case 追加 create_by 与 is_deleted 字段（与 M1 风格一致）：
--   1. create_by 用例创建人，用于数据隔离
--   2. is_deleted 用例软删除；步骤/变量提取/断言为子表，跟随用例物理删除
-- ============================================================
ALTER TABLE `t_test_case`
    ADD COLUMN `create_by` BIGINT NOT NULL COMMENT '创建人(关联 t_user.id)' AFTER `project_id`;

ALTER TABLE `t_test_case`
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常, 1:删除)';

-- ============================================================
-- M3 测试执行与报告模块补丁 2026-05-18
-- 1. t_execution_report：补齐数据隔离字段 + 失败步骤数 + 总耗时 + 软删
--    状态枚举统一为 RUNNING / PASSED / FAILED（与 detail.status 区分）
-- 2. t_execution_detail：补齐执行序号 + 实际请求摘要拆分字段 + 失败原因 + 状态码
--    状态枚举统一为 PASSED / FAILED
-- 注意：actual_request / actual_response 字段保留并复用，存最终摘要 JSON；
--       新增的 status_code / request_method / request_url / step_order / fail_reason
--       是为前端报告页钻取/筛选/排序服务（避免每次解析 actual_request JSON）
-- ============================================================

ALTER TABLE `t_execution_report`
    ADD COLUMN `project_id` BIGINT NOT NULL COMMENT '所属项目ID(冗余，便于 LEFT JOIN 列表与数据隔离)' AFTER `case_id`,
    ADD COLUMN `create_by` BIGINT NOT NULL COMMENT '创建人(关联 t_user.id，数据隔离)' AFTER `project_id`,
    ADD COLUMN `failed_steps` INT NOT NULL DEFAULT 0 COMMENT '失败步骤数(遇错即停时最多为 1)' AFTER `passed_steps`,
    ADD COLUMN `total_duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '总耗时(毫秒，从首步开始到末步结束)' AFTER `end_time`,
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常, 1:删除)';

ALTER TABLE `t_execution_report`
    MODIFY COLUMN `status` VARCHAR(20) NOT NULL COMMENT '最终结果(RUNNING / PASSED / FAILED)';

ALTER TABLE `t_execution_report` ADD INDEX `idx_project_create_by` (`project_id`, `create_by`);

ALTER TABLE `t_execution_detail`
    ADD COLUMN `step_order` INT NOT NULL DEFAULT 0 COMMENT '执行序号(从 1 起步)' AFTER `step_id`,
    ADD COLUMN `request_method` VARCHAR(10) COMMENT '实际请求方法(GET/POST/...)' AFTER `step_order`,
    ADD COLUMN `request_url` VARCHAR(500) COMMENT '实际请求 URL(变量替换后)' AFTER `request_method`,
    ADD COLUMN `status_code` INT COMMENT 'HTTP 响应状态码(网络异常时为 0)' AFTER `actual_response`,
    ADD COLUMN `fail_reason` VARCHAR(500) COMMENT '失败原因(网络异常 / 断言失败描述)' AFTER `assert_result`;

ALTER TABLE `t_execution_detail`
    MODIFY COLUMN `status` VARCHAR(20) NOT NULL COMMENT '单步结果(PASSED / FAILED)';

-- 修正历史脏类型：原 schema 中 elapsed_ms 误写为 LONG，被 MySQL 解析为 MEDIUMTEXT
ALTER TABLE `t_execution_detail`
    MODIFY COLUMN `elapsed_ms` BIGINT COMMENT '响应耗时(毫秒)';

