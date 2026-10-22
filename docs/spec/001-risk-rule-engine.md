# 001 - 风控规则引擎与交易风险检查

- 状态：待实现
- 优先级：P0
- 范围：规则引擎 + risk-check 接口（名单管理、命中记录为 P1，接口契约本文一并定义）

## 1. 背景与目标

为交易平台提供同步交易风险检查能力：业务方在交易提交时调用本系统，传入交易上下文，系统按启用中的规则集判定风险并返回决策。

- 决策取值：`PASS`（放行）/ `REVIEW`（转人工审核）/ `REJECT`（拒绝）
- 性能目标：单次检查接口 P99 < 100ms（含 Redis 与 MySQL 访问）

## 2. 功能范围

| 编号 | 规则 | 逻辑 | 默认动作 |
|---|---|---|---|
| R1 | 单笔限额 | amount > 阈值 | REVIEW |
| R2 | 频率限制 | 同一 userId 在滑动窗口内交易次数 > 阈值 | REVIEW |
| R3 | 名单 | 命中黑名单 / 白名单 | BLACK→REJECT，WHITE→直接 PASS |
| R4 | 时段异常 | occurTime 不在配置的允许交易时段内 | REVIEW |

不在本期范围：设备指纹、模型评分、图分析（后续迭代 spec）。

## 3. 接口定义

### 3.1 交易风险检查（核心）

`POST /api/transactions/risk-check`

请求：

```json
{
  "transactionId": "T2026102200001",
  "userId": "U10001",
  "accountNo": "A20001",
  "amount": 199.00,
  "currency": "CNY",
  "channel": "APP",
  "occurTime": "2026-10-22 21:30:00"
}
```

响应：

```json
{
  "transactionId": "T2026102200001",
  "decision": "REVIEW",
  "hitRules": [
    { "ruleCode": "LIMIT_SINGLE", "ruleName": "单笔限额", "hitDetail": "amount=199.00 > threshold=100.00" }
  ],
  "costMs": 23
}
```

校验要求：所有字段非空、amount > 0、occurTime 合法时间格式（遵循 security-rules）。

### 3.2 规则管理

- `GET /api/rules`：分页查询（ruleType / status 过滤）
- `POST /api/rules`、`PUT /api/rules/{id}`：新增 / 修改（含 threshold_config JSON 与 action）
- 规则增改后生效时延 ≤ 5s（缓存 TTL 或版本号失效）

### 3.3 名单管理

- `GET / POST / DELETE /api/name-lists`：按 list_type（BLACK / WHITE）+ biz_type（USER_ID / ACCOUNT_NO）+ biz_value 维护

### 3.4 命中记录

- `GET /api/hit-records`：分页查询，支持 userId / decision / 时间范围过滤

## 4. 表结构

```sql
-- 规则表
CREATE TABLE risk_rule (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code        VARCHAR(64)  NOT NULL COMMENT '规则编码，唯一',
  rule_name        VARCHAR(64)  NOT NULL,
  rule_type        VARCHAR(32)  NOT NULL COMMENT 'LIMIT/FREQUENCY/NAMELIST/TIME_WINDOW',
  priority         INT          NOT NULL DEFAULT 100 COMMENT '数值越小越先执行',
  status           VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
  threshold_config JSON         NOT NULL COMMENT '阈值配置，结构按 rule_type 约定',
  action           VARCHAR(16)  NOT NULL COMMENT '命中后动作：PASS/REVIEW/REJECT',
  create_time      DATETIME     NOT NULL,
  update_time      DATETIME     NOT NULL,
  UNIQUE KEY uk_rule_code (rule_code)
) COMMENT '风控规则表';

-- 名单表
CREATE TABLE risk_name_list (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_type   VARCHAR(16)  NOT NULL COMMENT 'BLACK/WHITE',
  biz_type    VARCHAR(16)  NOT NULL COMMENT 'USER_ID/ACCOUNT_NO',
  biz_value   VARCHAR(128) NOT NULL,
  remark      VARCHAR(255),
  status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
  create_time DATETIME     NOT NULL,
  update_time DATETIME     NOT NULL,
  UNIQUE KEY uk_list (list_type, biz_type, biz_value)
) COMMENT '黑白名单表';

-- 命中记录表
CREATE TABLE risk_hit_record (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_id VARCHAR(64)  NOT NULL,
  user_id        VARCHAR(64)  NOT NULL,
  rule_code      VARCHAR(64)  NOT NULL,
  decision       VARCHAR(16)  NOT NULL,
  hit_detail     VARCHAR(512),
  cost_ms        INT,
  create_time    DATETIME     NOT NULL,
  KEY idx_user_time (user_id, create_time),
  KEY idx_transaction (transaction_id)
) COMMENT '风控命中记录表';
```

## 5. Redis 设计

- 频率统计（滑动窗口，ZSET）：
  - key：`risk:freq:{ruleCode}:{userId}`，member：transactionId，score：时间戳
  - `ZADD` + `ZREMRANGEBYSCORE` + `ZCARD` 必须在同一 Lua 脚本内原子执行（见 security-rules）
- 名单缓存：key `risk:nl:{listType}:{bizType}:{bizValue}`，String，TTL 5min，未命中回源 DB

## 6. 决策流程

```
入参校验
  → 白名单命中？ → 是 → 直接 PASS 返回
  → 黑名单命中？ → 是 → REJECT
  → 加载 ENABLED 规则，按 priority 升序逐条执行
  → 命中的规则记录 hit_detail；最终决策取最严重级（REJECT > REVIEW > PASS）
  → 命中记录落库
  → 返回决策与命中明细
```

规则执行器采用策略模式：`RiskRuleHandler` 接口 + 按 `rule_type` 路由，新增规则类型不改动主流程。

## 7. 验收标准

- [ ] risk-check 全链路（含 Redis + MySQL）压测 P99 < 100ms
- [ ] 判定顺序正确：WHITE 短路 PASS → BLACK REJECT → 普通规则按 priority
- [ ] 多规则命中时最终决策取最严重级，hitRules 明细完整
- [ ] 频率统计 Lua 原子执行，并发下计数准确（写并发单测验证）
- [ ] 规则增改后 ≤ 5s 生效
- [ ] R1–R4 每类规则均有单测：正常 / 边界 / 异常
- [ ] 安全自查通过：SQL 全预编译、日志脱敏、无硬编码密钥
