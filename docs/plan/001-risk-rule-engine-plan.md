# 001 实现方案（Plan）

- 对应 spec：docs/spec/001-risk-rule-engine.md
- 状态：AI 辅助生成的实现方案；部分决策尚未完成独立人工复核
- 预计产出：约 40 个文件，5 次提交切片

## 1. 总体分层

```
com.riskcontrol
├── common        Result / BizException / ErrorCode / GlobalExceptionHandler
├── config        MybatisPlusConfig(分页) / WebConfig + ApiKeyInterceptor(鉴权)
├── controller    RiskCheckController / RiskRuleController / NameListController / HitRecordController
├── dto           RiskCheckRequest、各管理接口入参（Validation 注解齐全）
├── vo            RiskCheckResponse、HitRuleVO、PageVO<T> 等
├── entity        RiskRule / RiskNameList / RiskHitRecord（@TableName + Lombok）
├── mapper        三个 BaseMapper（MyBatis-Plus，无自定义 XML 需求则不建 XML）
├── service       RiskCheckService / RiskRuleService / NameListService / HitRecordService（接口 + impl）
└── rule          引擎核心（策略模式）
    ├── RuleContext            规则执行上下文（请求字段 + 预查询结果）
    ├── RuleHitResult          单条规则命中结果（ruleCode / hitDetail / action）
    ├── RiskRuleHandler        接口：supports(ruleType) + handle(ctx, rule)
    ├── RiskRuleHandlerFactory 按 ruleType 路由（Spring 注入全部 handler 构建映射）
    └── handler
        ├── LimitRuleHandler       R1 单笔限额
        ├── FrequencyRuleHandler   R2 频率限制（Redis Lua）
        ├── NameListRuleHandler    R3 黑名单
        └── TimeWindowRuleHandler  R4 时段异常
resources
├── lua/frequency_window.lua   ZADD + ZREMRANGEBYSCORE + ZCARD 原子脚本
└── sql/init.sql               建表 + 4 条默认规则 + 示例名单
```

## 2. 核心流程（RiskCheckService.impl）

```
1. 参数校验通过后，组装 RuleContext
2. 白名单检查（USER_ID + ACCOUNT_NO 双 biz_type）→ 命中：直接 PASS 返回（不落命中记录）
3. 黑名单检查 → 命中：REJECT，落 hit_record
4. 查询 ENABLED 规则，按 priority 升序逐条执行：
   - Factory 路由到对应 handler，未命中跳过，命中收集 RuleHitResult
5. 决策归并：取最严重级（REJECT > REVIEW > PASS）
6. 命中的普通规则落 hit_record（同步单条 insert）
7. 返回决策 + hitRules 明细 + costMs（StopWatch 计时）
```

## 3. 关键技术设计

| 主题 | 方案 | 理由 |
|---|---|---|
| 规则类型扩展 | 策略模式：`RiskRuleHandler` + `supports()` 路由 | 新增规则类型只加 handler 类，主流程零改动 |
| threshold_config 解析 | Jackson 反序列化为各 handler 私有配置类（LimitConfig / FrequencyConfig / TimeWindowConfig） | 类型安全，避免到处解析 JsonNode |
| 频率统计 | Lua 脚本原子执行 ZADD + ZREMRANGEBYSCORE + ZCARD，`DefaultRedisScript` 加载 classpath 脚本 | 满足 security-rules 原子性要求 |
| 名单缓存 | StringRedisTemplate，key `risk:nl:{listType}:{bizType}:{bizValue}`，TTL 5min，未命中回源 DB；名单增删时主动失效对应 key | 满足 ≤5s 生效 + 读路径快 |
| 规则加载 | 每次请求直接查 DB（规则表仅个位数条 + 全表小） | MVP 不引 Caffeine（遵守"新依赖需确认"）；天然满足热更新，后续可无感加缓存 |
| 命中记录落库 | 同步单条 insert（~2ms） | MVP 简单可靠，预算内；异步化留作演进点 |
| 鉴权 | `ApiKeyInterceptor`：校验 `X-Api-Key` header，值来自环境变量 `RISK_API_KEY`，管理接口与 risk-check 全部拦截 | 落实"接口必须有鉴权"，无登录体系下的最小方案 |
| 时段规则 | `startTime ~ endTime` 含头含尾，不支持跨零点 | MVP 简化，跨零点留作迭代 |
| 分页 | MyBatis-Plus `PaginationInnerInterceptor` | 框架标准方案 |

## 4. 测试策略

- **Handler 单测**（Mockito mock Redis / Mapper）：R1–R4 各覆盖正常 / 边界（等于阈值不命中）/ 配置非法异常
- **RiskCheckService 单测**：白名单短路顺序、多规则命中取最严重级、无规则全 PASS
- **Lua 原子性**：单测覆盖脚本参数与返回值解析；真实并发计数准确性放联调阶段（本地起 Redis 手工压），README 说明
- **Controller 层**：MVP 不做 slice 测试，靠 Service 覆盖 + 联调

## 5. 提交切片（每片可编译可运行）

| # | 内容 | 产出 |
|---|---|---|
| C1 | 基建 | common 四件套 + MybatisPlusConfig + ApiKey 拦截器 + WebConfig |
| C2 | 管理 CRUD | 实体 / mapper / 三个管理 service + controller（首次实战 api-crud skill） |
| C3 | 规则引擎 | Lua 脚本 + 4 个 handler + Factory + RiskCheckService 主流程 |
| C4 | 接口与测试 | RiskCheckController + 全部单测 |
| C5 | 收尾 | init.sql + README 快速开始补全 + security-rules 逐条自查记录 |

## 6. 待人审确认的决策点

1. 名单/规则缓存策略：名单走 Redis + TTL，规则 MVP 直接查库不引 Caffeine
2. 白名单短路返回 PASS，但不落 hit_record（黑名单与普通规则命中才落）
3. 鉴权采用 X-Api-Key + 拦截器（无登录体系的最小方案）
4. hit_record 同步落库（异步化留作演进点）
5. 时段规则不支持跨零点

> 本方案用于记录 AI 辅助开发时形成的设计，不应作为方案已由项目发起者独立设计或完整审定的证明。特别是风控异常处理等决策，仍需结合生产要求复核。
