package com.riskcontrol.rule.config;

import lombok.Data;

import java.math.BigDecimal;

/** LIMIT 规则阈值配置，对应 threshold_config JSON：{"maxAmount": 10000.00} */
@Data
public class LimitConfig {

    /** 单笔金额上限 */
    private BigDecimal maxAmount;
}
