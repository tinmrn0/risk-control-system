-- 风控系统初始化脚本：建库建表 + 默认规则 + 示例名单
-- 执行：mysql -uroot -p < docs/sql/init.sql

CREATE DATABASE IF NOT EXISTS risk_control DEFAULT CHARACTER SET utf8mb4;
USE risk_control;

-- 会话字符集必须与文件编码一致，否则中文经管道导入时被按客户端默认字符集双重编码
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS risk_rule (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_code        VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_name        VARCHAR(64)  NOT NULL COMMENT '规则名称',
    rule_type        VARCHAR(32)  NOT NULL COMMENT '规则类型：LIMIT/FREQUENCY/NAMELIST/TIME_WINDOW',
    priority         INT          NOT NULL DEFAULT 100 COMMENT '优先级，数值越小越先执行',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    threshold_config JSON         NOT NULL COMMENT '阈值配置，结构按 rule_type 约定',
    action           VARCHAR(16)  NOT NULL COMMENT '命中动作：PASS/REVIEW/REJECT',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_status_priority (status, priority)
) ENGINE = InnoDB COMMENT '风控规则表';

CREATE TABLE IF NOT EXISTS risk_name_list (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    list_type   VARCHAR(16)  NOT NULL COMMENT '名单类型：BLACK/WHITE',
    biz_type    VARCHAR(16)  NOT NULL COMMENT '业务维度：USER_ID/ACCOUNT_NO',
    biz_value   VARCHAR(128) NOT NULL COMMENT '业务值',
    remark      VARCHAR(255) NULL COMMENT '备注',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_list_biz (list_type, biz_type, biz_value)
) ENGINE = InnoDB COMMENT '风控名单表';

CREATE TABLE IF NOT EXISTS risk_hit_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    transaction_id VARCHAR(64)  NOT NULL COMMENT '交易单号',
    user_id        VARCHAR(64)  NOT NULL COMMENT '用户标识',
    rule_code      VARCHAR(64)  NOT NULL COMMENT '命中规则编码',
    decision       VARCHAR(16)  NOT NULL COMMENT '最终决策：PASS/REVIEW/REJECT',
    hit_detail     VARCHAR(512) NOT NULL COMMENT '命中详情',
    cost_ms        INT          NOT NULL DEFAULT 0 COMMENT '检查耗时 ms',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_transaction_id (transaction_id),
    KEY idx_user_id_create_time (user_id, create_time)
) ENGINE = InnoDB COMMENT '风控命中记录表';

-- 默认规则：黑名单最先执行，其余按优先级依次判定
INSERT INTO risk_rule (rule_code, rule_name, rule_type, priority, status, threshold_config, action) VALUES
('NAMELIST_BLACK', '黑名单', 'NAMELIST', 1, 'ENABLED', '{}', 'REJECT'),
('LIMIT_SINGLE', '单笔限额', 'LIMIT', 10, 'ENABLED', '{"maxAmount": 10000.00}', 'REVIEW'),
('FREQ_10MIN', '10分钟交易频率', 'FREQUENCY', 20, 'ENABLED', '{"windowSeconds": 600, "maxCount": 10}', 'REVIEW'),
('TIME_WINDOW', '交易时段', 'TIME_WINDOW', 30, 'ENABLED', '{"startTime": "09:00", "endTime": "22:00"}', 'REVIEW');

-- 示例黑名单
INSERT INTO risk_name_list (list_type, biz_type, biz_value, remark, status) VALUES
('BLACK', 'USER_ID', 'U99999', '示例：测试黑名单用户', 'ENABLED');
