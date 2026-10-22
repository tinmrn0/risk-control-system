package com.riskcontrol.rule;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 规则执行上下文，由风控检查请求构建，只读 */
@Getter
@Builder
public class RuleContext {

    private final String transactionId;
    private final String userId;
    private final String accountNo;
    private final BigDecimal amount;
    private final String currency;
    private final String channel;
    private final LocalDateTime occurTime;
}
