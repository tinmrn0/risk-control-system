# risk-control-system

精简版交易风控系统 MVP：规则引擎 + 交易风险检查 + 名单管理 + 命中记录。

> 本项目同时是一套 **AI 开发工作流**的落地演示：代码由 AI 按 `spec → plan → 实现 → review` 流程生成，人的角色是写 spec、审方案、做 review。

## 功能模块

| 模块 | 说明 |
|---|---|
| 风控规则引擎 | 单笔限额 / 频率限制 / 黑白名单 / 时段异常，命中判定输出 PASS / REVIEW / REJECT |
| 交易风险检查 | `POST /api/transactions/risk-check` 同步决策接口 |
| 名单管理 | 黑 / 白名单 CRUD + Redis 缓存 |
| 命中记录 | 风控事件落库与分页查询 |
| 演示控制台 | 内置单文件可视化页面（`src/main/resources/static/`），支持决策演示与一键并发压测 |

技术栈：Java 17 · Spring Boot 3.2 · MyBatis-Plus · MySQL 8 · Redis 7

## AI 工作流说明

```
docs/spec/*.md          ← 写 spec：需求 / 接口定义 / 表结构 / 验收标准
        ↓
Plan 方案 → 人审       ← AI 出实现方案，确认分层与关键设计后再动手
        ↓
AI 实现               ← AI 按 .claude/rules（常驻约束）+ .claude/skills（按需流程）生成代码
        ↓
Review + 单测         ← 重点审：安全规则遵守情况、边界条件、SQL 预编译
```

- `CLAUDE.md`：工作流入口，定义 spec 先行 / plan 先审约定
- `.claude/rules/`：常驻约束（编码规范 + 安全规则），每次会话生效
- `.claude/skills/api-crud/`：按需流程（CRUD 生成手册），仅生成 CRUD 时加载——按需加载即上下文经济

## 快速开始

环境要求：JDK 17+ · MySQL 8 · Redis 7 · Maven 3.9+

```bash
# 1. 初始化数据库（建库 + 三张表 + 默认规则 + 示例黑名单）
mysql -uroot -p < docs/sql/init.sql

# 2. 配置环境变量（密钥不落配置文件）
export MYSQL_USER=root
export MYSQL_PASSWORD=你的密码
export REDIS_PASSWORD=你的redis密码   # 本地无密码可不设
export RISK_API_KEY=dev-key-123      # 接口鉴权密钥，未配置时所有接口拒绝访问

# 3. 启动
mvn spring-boot:run

# 4. 测试（18 个单测，无需外部依赖）
mvn test

# 5. 打开演示控制台
open http://localhost:8080/
```

发起一笔风险检查：

```bash
curl -X POST http://localhost:8080/api/transactions/risk-check \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: dev-key-123" \
  -d '{
    "transactionId": "T1001",
    "userId": "U1001",
    "accountNo": "A1001",
    "amount": 100.01,
    "currency": "CNY",
    "channel": "APP",
    "occurTime": "2026-10-22 21:30:00"
  }'
```

命中单笔限额时返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "transactionId": "T1001",
    "decision": "REVIEW",
    "hitRules": [
      { "ruleCode": "LIMIT_SINGLE", "ruleName": "单笔限额", "hitDetail": "交易金额 100.01 > 单笔限额 100.00" }
    ],
    "costMs": 3
  }
}
```

## 验证记录（2026-10-22，Docker MySQL 8 + Redis 7 实测）

### 功能链路

| 场景 | 请求要点 | 结果 | 耗时 |
|---|---|---|---|
| 正常小额交易 | amount=10.5，时段内 | PASS | 251ms（首请求含名单缓存回源） |
| 大额交易 | amount=15000 > maxAmount=10000 | REVIEW（LIMIT_SINGLE） | 36ms |
| 黑名单用户 | userId=U99999 | REJECT（NAMELIST_BLACK） | 21ms |
| 时段外交易 | occurTime=23:30 | REVIEW（TIME_WINDOW） | 20ms |
| 白名单短路 | U1002 加白后重发大额 | PASS | **3ms**（36ms → 3ms，短路跳过规则执行） |

### 频率规则并发压测（Lua 原子性）

条件：同一用户 30 请求 / 10 并发 / maxCount=10 / 滑动窗口 10 分钟

- 结果：**10 PASS + 20 REVIEW 精确切分**——第 1~10 次计数 1~10 ≤ maxCount 放行，第 11 次起 count=11 > 10 命中
- 窗口计数 ZCARD=30，**零丢失计数**；落库 20 条与 REVIEW 数完全一致
- 结论：Lua 脚本在并发下保证"计数 + 判定"原子性，无竞态漏判

### 踩坑记录

- **MySQL 8 Docker + `useSSL=false`**：caching_sha2_password 认证需从服务端取 RSA 公钥，JDBC URL 必须加 `allowPublicKeyRetrieval=true`；因 Hikari 连接池懒加载，应用启动正常、首条 SQL 才暴露该问题——排查时通过 MySQL `general_log` 确认 SQL 未到达，定位到连接建立阶段

## 接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/transactions/risk-check | 交易风险检查（核心） |
| GET/POST/PUT | /api/rules | 风控规则分页 / 新增 / 更新 |
| GET/POST/DELETE | /api/name-lists | 名单分页 / 新增 / 删除 |
| GET | /api/hit-records | 命中记录分页查询 |

## 设计要点

- **策略模式规则引擎**：`RiskRuleHandler` 接口 + Factory 按规则类型路由，新增规则类型只需新增实现类
- **决策归并**：多规则命中取最严重级（REJECT > REVIEW > PASS）；白名单前置短路
- **原子频率统计**：Redis Lua 滑动窗口（`ZADD` + `ZREMRANGEBYSCORE` + `ZCARD`），脚本在 `src/main/resources/lua/`
- **名单缓存**：正缓存 TTL 5min + 负缓存 TTL 1min，名单变更主动失效
- **保存时配置校验**：`threshold_config` 在规则保存时校验必填字段，避免脏配置到执行期才失败
- **已知取舍**：命中记录同步落库；频率规则对被拒绝的交易也计数；单条规则异常降级跳过不影响整体

详细设计见 [docs/spec/001-risk-rule-engine.md](docs/spec/001-risk-rule-engine.md)。
