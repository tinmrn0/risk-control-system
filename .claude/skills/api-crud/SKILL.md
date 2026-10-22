---
name: api-crud
description: 给定表名/实体与字段定义，按固定分层生成完整 CRUD 模块（Entity → Mapper → Service → Controller → 单测 → 输出清单）。触发词：生成 CRUD、新增模块、按表生成接口。
---

# 前置输入

调用本 skill 前需要确认：表名与字段（含类型、注释）、是否需要分页查询、业务异常场景。

# 执行步骤

1. **Entity**：按表结构生成实体类，Lombok + Validation 注解 + `@TableName`；金额用 `BigDecimal`，时间用 `LocalDateTime`
2. **Mapper**：MyBatis-Plus `BaseMapper` 接口；自定义 SQL 写 XML 且全部预编译（`#{}`）
3. **DTO / VO**：入参 DTO（Validation 注解齐全）与出参 VO 分开定义，禁止 Entity 直接对外
4. **Service**：接口 + 实现；业务异常统一抛 `BizException`；关键方法写清楚注释
5. **Controller**：统一返回体 `Result<T>`；`@Valid` 校验入参；RESTful 命名
6. **单测**：Service 层正常 / 边界 / 异常三类用例，使用 AssertJ 断言
7. **输出清单**：列出本次新增 / 修改的文件与对应接口路径，标注未覆盖事项

# 约束提醒

- 执行全程遵守 `.claude/rules/` 下常驻规则（安全规则优先）
- 不自行引入新依赖；需要时先停下确认
