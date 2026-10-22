package com.riskcontrol.rule.handler;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import com.riskcontrol.rule.RiskRuleHandler;
import com.riskcontrol.rule.config.TimeWindowConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/** R4 时段异常：交易时间落在允许时段（含边界）之外则命中 */
@Component
@RequiredArgsConstructor
public class TimeWindowRuleHandler implements RiskRuleHandler {

    private final RuleConfigParser parser;

    @Override
    public RuleType supports() {
        return RuleType.TIME_WINDOW;
    }

    @Override
    public RuleHitResult handle(RuleContext ctx, RiskRule rule) {
        TimeWindowConfig config = parser.parse(rule.getRuleType(), rule.getThresholdConfig(), TimeWindowConfig.class);
        if (config.getStartTime() == null || config.getEndTime() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "TIME_WINDOW 规则缺少 startTime/endTime 配置");
        }
        LocalTime occur = ctx.getOccurTime().toLocalTime();
        boolean inWindow = !occur.isBefore(config.getStartTime()) && !occur.isAfter(config.getEndTime());
        if (inWindow) {
            return null;
        }
        String detail = String.format("交易时间 %s 在允许时段 %s~%s 之外",
                occur, config.getStartTime(), config.getEndTime());
        return RuleHitResult.of(rule, detail);
    }
}
