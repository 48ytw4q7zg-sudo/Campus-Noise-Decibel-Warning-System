-- ============================================================
-- 校园噪音分贝预警员系统 · 数据库初始化脚本
-- MySQL 8.4 LTS · InnoDB · utf8mb4
-- 生成日期：2026-06-11 · 数据库名：noise_db（来自 application.yml datasource.url）
-- R-03 审核修复版（15 条 issue 已闭合并同步至本文件）
-- ============================================================

CREATE DATABASE IF NOT EXISTS noise_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE noise_db;

-- ============================================================
-- 1. user（用户表 · P0）
-- ============================================================
CREATE TABLE IF NOT EXISTS user (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(20)     NOT NULL                COMMENT '用户名，登录凭证',
    password    VARCHAR(255)    NOT NULL                COMMENT 'BCrypt密文，60-255字符',
    role        VARCHAR(10)     NOT NULL                COMMENT '角色：普通用户/管理员（应用层校验，INSERT必填）',
    status      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=正常，0=禁用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '逻辑删除（预留）',
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. noise_record（噪声记录表 · P0 · append-only）
-- ============================================================
CREATE TABLE IF NOT EXISTS noise_record (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    location         VARCHAR(10)     NOT NULL                COMMENT '功能区：图书馆/食堂/操场/宿舍',
    decibel          DECIMAL(5,1)    NOT NULL                COMMENT '分贝值，范围20.0-120.0 dB(A)',
    time_point       DATETIME        NOT NULL                COMMENT '噪声时间点',
    device_id        VARCHAR(50)     NOT NULL                COMMENT '设备标识：硬件ID/SIMULATOR/MANUAL_+时间戳',
    is_abnormal      TINYINT         NULL DEFAULT NULL       COMMENT '异常标记：NULL=未判断，0=正常，1=异常',
    judged_by_model  VARCHAR(20)     NOT NULL DEFAULT 'RULE_BASED' COMMENT '判定模型来源：RULE_BASED/ADAPTIVE/HYBRID，P0记录默认RULE_BASED',
    noise_type       VARCHAR(10)     NULL DEFAULT NULL       COMMENT '噪声类型(P2)：交谈/施工/体育活动/交通/其它',
    noise_duration   INT             NULL DEFAULT NULL       COMMENT '噪声持续时间(秒)(P2可选)，NULL=未采集，对接研究报告§4.4改进方向',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    -- 高频查询加速索引（仪表盘按功能区查最新数据），虽被联合索引最左前缀覆盖但保留
    INDEX idx_nr_location (location),
    INDEX idx_nr_time_point (time_point),
    INDEX idx_nr_location_time (location, time_point),
    INDEX idx_nr_is_abnormal (is_abnormal),
    INDEX idx_nr_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='噪声记录表';

-- ============================================================
-- 3. threshold_rule（阈值规则表 · P0）
-- ============================================================
CREATE TABLE IF NOT EXISTS threshold_rule (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    location        VARCHAR(10)     NOT NULL                COMMENT '功能区',
    time_segment    VARCHAR(20)     NOT NULL                COMMENT '时段标签：早读/上课/午休/活动/晚自修/夜间静校/用餐时段/非用餐时段',
    threshold_value INT             NOT NULL                COMMENT '阈值dB(A)，范围0-120',
    description     VARCHAR(200)    NULL DEFAULT NULL       COMMENT '业务逻辑说明',
    status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=启用，0=禁用',
    version         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_rule_location_segment (location, time_segment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阈值规则表';

-- ============================================================
-- 4. alert_log（告警日志表 · P0 · 独立记录）
-- ============================================================
CREATE TABLE IF NOT EXISTS alert_log (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    noise_record_id  BIGINT          NOT NULL                COMMENT '关联噪声记录ID',
    location         VARCHAR(10)     NOT NULL                COMMENT '反范式冗余：查询加速，源字段noise_record.location',
    decibel          DECIMAL(5,1)    NOT NULL                COMMENT '反范式冗余：查询加速，源字段noise_record.decibel',
    threshold_value  INT             NOT NULL                COMMENT '触发时的阈值dB(A)',
    alert_type       VARCHAR(10)     NOT NULL                COMMENT '告警类型：超阈值/骤升(≥15dB)/夜间异常',
    confirm_status   VARCHAR(10)     NOT NULL                COMMENT '确认状态：未确认→已确认→已处置，不允许回退，INSERT必填',
    confirmed_by     BIGINT          NULL DEFAULT NULL       COMMENT '确认人ID→user.id，NULL=未确认时不关联用户',
    remark           VARCHAR(500)    NULL DEFAULT NULL       COMMENT '处理备注',
    version          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '确认/处置时间',
    PRIMARY KEY (id),
    INDEX idx_al_location (location),
    INDEX idx_al_create_time (create_time),
    INDEX idx_al_confirm_status (confirm_status),
    -- 防御性设计：noise_record为append-only不删除，RESTRICT防DBA误操作
    CONSTRAINT fk_alert_noise FOREIGN KEY (noise_record_id) REFERENCES noise_record(id)
        ON DELETE RESTRICT,
    -- 用户注销后告警记录保留，confirmed_by=NULL表示确认人已不存在
    CONSTRAINT fk_alert_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES user(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警日志表';

-- ============================================================
-- 5. area_config（功能区配置表 · P0）
-- ============================================================
CREATE TABLE IF NOT EXISTS area_config (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '功能区ID',
    area_name         VARCHAR(10)     NOT NULL                COMMENT '功能区名称',
    noise_sensitivity TINYINT         NOT NULL                COMMENT '噪声敏感度：1=高，2=中，3=低',
    default_threshold INT             NOT NULL                COMMENT '默认阈值dB(A)，范围0-120',
    description       VARCHAR(200)    NULL DEFAULT NULL       COMMENT '描述',
    status            TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1=启用，0=停用',
    window_size       INT             NULL DEFAULT NULL       COMMENT 'P1-1自适应阈值滑动窗口大小(分钟)，NULL=未配置，默认值15(图书馆/宿舍)/10(食堂/操场)',
    k_value           DECIMAL(3,2)    NULL DEFAULT NULL       COMMENT 'P1-1自适应阈值灵敏度系数k，NULL=未配置，默认值2(图书馆/宿舍)/3(食堂/操场)',
    version           INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_area_name (area_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能区配置表';

-- ============================================================
-- 6. report（报告表 · P2）
-- ============================================================
CREATE TABLE IF NOT EXISTS report (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    report_period VARCHAR(5)    NOT NULL                COMMENT '周期：日/周/月',
    period_start  DATETIME       NOT NULL                COMMENT '统计周期起始时间',
    period_end    DATETIME       NOT NULL                COMMENT '统计周期结束时间',
    content       TEXT           NOT NULL                COMMENT '报告内容（Markdown/HTML格式）',
    status        VARCHAR(10)    NOT NULL                COMMENT '状态：生成中/已生成/生成失败，INSERT必填',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    update_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    -- 支持报告列表按生成时间倒序查询
    INDEX idx_report_period (report_period),
    INDEX idx_report_period_start (period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告表（P2）';

-- ============================================================
-- 7. 初始数据（种子数据 · 按外键依赖顺序INSERT）
-- ============================================================

-- 7.1 预置管理员账号 （密码 admin123 的 BCrypt 哈希）
INSERT INTO user (username, password, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '管理员', 1)
ON DUPLICATE KEY UPDATE username = username;

-- 7.2 四大功能区初始配置
INSERT INTO area_config (area_name, noise_sensitivity, default_threshold, description) VALUES
('图书馆', 1, 40, '需严格安静，噪声敏感度极高，参考GB50118-2010高要求标准'),
('食堂',   2, 65, '用餐时段允许正常交谈和餐具碰撞噪声，非用餐时段降低阈值'),
('操场',   3, 70, '体育活动噪声容忍度高，参考GB3096-2008放宽标准'),
('宿舍',   1, 45, '日常活动噪声容忍度，夜间降低至40dB')
ON DUPLICATE KEY UPDATE area_name = area_name;

-- 7.3 预置业务规则阈值（基于研究报告 §3.3.1）
INSERT INTO threshold_rule (location, time_segment, threshold_value, description) VALUES
-- 图书馆
('图书馆', '早读',       43, '允许轻微晨读噪声，较上课时段放宽3dB'),
('图书馆', '上课',       40, '严格安静要求，参考GB50118-2010高要求标准'),
('图书馆', '午休',       40, '午休保持安静'),
('图书馆', '活动',       40, '晚自修等同上课标准'),
('图书馆', '晚自修',     40, '严格安静要求'),
('图书馆', '夜间静校',   35, '最严格管控，保障夜间安静'),
-- 食堂
('食堂',   '用餐时段',   65, '允许正常交谈、餐具碰撞，放宽至65dB'),
('食堂',   '非用餐时段', 55, '非用餐时段人员稀少，降低阈值'),
-- 操场
('操场',   '活动时段',   70, '体育活动噪声容忍度高'),
('操场',   '夜间',       45, '夜间禁止剧烈活动，按1类区夜间标准'),
-- 宿舍
('宿舍',   '昼间',       45, '日常活动噪声容忍度，参考GB3096-2008 1类区标准'),
('宿舍',   '夜间',       40, '夜间休息，适当严格，降低5dB')
ON DUPLICATE KEY UPDATE location = location;
