# risk-control-system

精简版交易风控系统 MVP：规则引擎 + 交易风险检查 + 名单管理 + 命中记录。

> **项目说明：**项目选题（交易风控系统）由我提出；需求拆解、技术方案、文档和代码主要由 AI 生成。我没有独立完成系统设计或逐行编写代码，因此本项目更准确地说是一次 AI 辅助开发实践和学习材料，不代表我已经熟练掌握 AI workflow、rules 或 skills。

## 功能模块

| 模块 | 说明 |
|---|---|
| 风控规则引擎 | 单笔限额 / 频率限制 / 黑白名单 / 时段异常，命中判定输出 PASS / REVIEW / REJECT |
| 交易风险检查 | `POST /api/transactions/risk-check` 同步决策接口 |
| 名单管理 | 黑 / 白名单 CRUD + Redis 缓存 |
| 命中记录 | 风控事件落库与分页查询 |
| 演示控制台 | 内置单文件可视化页面（`src/main/resources/static/`），支持决策演示与一键并发压测 |

技术栈：Java 17 · Spring Boot 3.2 · MyBatis-Plus · MySQL 8 · Redis 7

## AI 辅助开发过程与个人参与

本项目从“做一套风控系统”这个选题开始，后续需求/spec、实现方案、代码及测试主要由 AI 辅助生成。仓库中的 `CLAUDE.md`、`.claude/rules/` 和 `.claude/skills/` 是项目采用的约束与流程文件；它们展示了我尝试使用规则和 skill 组织 AI 开发的方式，但不应理解为这些设计和实现均由我独立完成。

我目前仍在补足对代码和 AI 工作流的理解。面试交流时，我会如实说明自己能解释和验证的部分，也会明确指出还不能独立判断的设计细节。展示这个项目的目的，是呈现一次具体的 AI 辅助开发尝试，并作为后续学习和改进的基础。

## 计划采用的 AI 工作流

```
docs/spec/*.md          ← 写 spec：需求 / 接口定义 / 表结构 / 验收标准
        ↓
Plan 方案 → 人工审阅   ← 检查分层、关键设计和风险点
        ↓
AI 实现               ← AI 按 .claude/rules（常驻约束）+ .claude/skills（按需流程）生成代码
        ↓
Review + 单测         ← 重点审：安全规则遵守情况、边界条件、SQL 预编译
```

- `CLAUDE.md`：工作流入口，记录 spec 先行 / plan 先审约定
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

## 已记录的验证结果（2026-10-22，Docker MySQL 8 + Redis 7）

以下结果来自项目开发过程中的本地联调记录。当前仓库没有附完整的压测脚本和环境复现配置；面试展示时应将其作为已有开发记录，而不是可由他人直接复现的基准测试报告。

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

## 当前设计与已知限制

- **策略模式规则引擎**：`RiskRuleHandler` 接口 + Factory 按规则类型路由，新增规则类型只需新增实现类
- **决策归并**：多规则命中取最严重级（REJECT > REVIEW > PASS）；白名单前置短路
- **原子频率统计**：Redis Lua 滑动窗口（`ZADD` + `ZREMRANGEBYSCORE` + `ZCARD`），脚本在 `src/main/resources/lua/`
- **名单缓存**：正缓存 TTL 5min + 负缓存 TTL 1min，名单变更主动失效
- **保存时配置校验**：`threshold_config` 在规则保存时校验必填字段，避免脏配置到执行期才失败
- **已知取舍**：命中记录同步落库；频率规则对被拒绝的交易也计数；单条规则异常会被跳过。这种异常降级可能造成风险规则未生效，生产风控场景需要重新评估并考虑 fail-closed、人工审核或告警等处理
- **演示范围**：当前为 MVP，尚未证明满足生产环境的可用性、安全性、容量和监控要求

详细设计见 [docs/spec/001-risk-rule-engine.md](docs/spec/001-risk-rule-engine.md)，实现方案见 [docs/plan/001-risk-rule-engine-plan.md](docs/plan/001-risk-rule-engine-plan.md)。这些文档也由 AI 辅助生成，内容仍需结合实际实现持续校正。
