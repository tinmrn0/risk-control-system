# 编码规范（rule：常驻约束，每次会话生效）

## 技术栈约定

- Java 17 + Spring Boot 3.2.x + MyBatis-Plus 3.5.x + MySQL 8 + Redis 7（Spring Data Redis）
- 包结构：`com.riskcontrol` 下按 `controller / service / mapper / entity / dto / vo / rule / common` 分层

## 分层与结构

- Controller 只做参数校验与编排，业务逻辑写在 Service，SQL 只出现在 Mapper
- 统一返回体 `Result<T>`（code / message / data），业务异常统一抛 `BizException`，由全局异常处理器兜底
- 入参用 DTO + Validation 注解，出参用 VO，禁止 Entity 直接对外
- Service 方法必须有单测：正常路径 + 边界 + 异常路径（JUnit 5 + AssertJ）

## 代码风格

- 遵循《阿里巴巴 Java 开发手册》；禁止魔法值，常量收敛到常量类或枚举
- 日志使用 Lombok `@Slf4j` + 占位符写法（`log.info("risk check cost={}ms", costMs)`），禁止字符串拼接
- 禁止硬编码密钥、地址、账号——配置统一放 application.yml，敏感项走环境变量
- 引入任何新第三方依赖前必须先征得确认，说明用途与理由
- 时间字段统一 `LocalDateTime`，金额统一 `BigDecimal`（禁止 double / float）
