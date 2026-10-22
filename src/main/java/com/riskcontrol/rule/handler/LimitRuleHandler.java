package com.riskcontrol.rule.handler;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import com.riskcontrol.rule.RiskRuleHandler;
import com.riskcontrol.rule.config.LimitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** R1 单笔限额：交易金额超过配置上限则命中 */
@Component
@RequiredArgsConstructor
public class LimitRuleHandler implements RiskRuleHandler {

    private final RuleConfigParser parser;

    @Override
    public RuleType supports() {
        return RuleType.LIMIT;
    }

    @Override
    public RuleHitResult handle(RuleContext ctx, RiskRule rule) {
        LimitConfig config = parser.parse(rule.getRuleType(), rule.getThresholdConfig(), LimitConfig.class);
        if (config.getMaxAmount() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "LIMIT 规则缺少 maxAmount 配置");
        }
        // 等于上限不命中，超过才命中
        if (ctx.getAmount().compareTo(config.getMaxAmount()) <= 0) {
            return null;
        }
        String detail = String.format("交易金额 %s > 单笔限额 %s", ctx.getAmount(), config.getMaxAmount());
        return RuleHitResult.of(rule, detail);
    }
}
