# 交易风控系统（MVP）

精简版交易风控系统：规则引擎 + 交易风险检查接口 + 名单管理 + 命中记录。
技术栈：Java 17 + Spring Boot 3.2.x + MyBatis-Plus + MySQL 8 + Redis 7。

## 常驻规则（每次会话必须遵守）

@.claude/rules/coding-standards.md
@.claude/rules/security-rules.md

## 工作流约定

1. **Spec 先行**：新功能先写 spec 到 `docs/spec/`（需求、接口定义、表结构、验收标准），经我确认后才能进入实现
2. **Plan 先审**：实现前先给出实现方案（分层、类清单、关键逻辑），我确认后再写代码
3. **按 skill 执行**：生成 CRUD 模块时使用 `api-crud` skill（.claude/skills/api-crud/SKILL.md）
4. **完成后自查**：对照 security-rules 逐条检查；确认边界条件与异常路径有单测覆盖

## 当前进行中

- 001 风控规则引擎与交易风险检查：MVP 已完成（18/18 单测通过）
- 待办：git init + 推送 GitHub；本地 Redis 联调压测频率规则原子性
