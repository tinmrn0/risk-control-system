package com.riskcontrol.rule.handler;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import com.riskcontrol.rule.RiskRuleHandler;
import com.riskcontrol.rule.config.FrequencyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/** R2 频率限制：Redis Lua 原子滑动窗口统计交易次数，超过阈值则命中 */
@Component
@RequiredArgsConstructor
public class FrequencyRuleHandler implements RiskRuleHandler {

    private static final String KEY_TEMPLATE = "risk:freq:%s:%s";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> frequencyWindowScript;
    private final RuleConfigParser parser;

    @Override
    public RuleType supports() {
        return RuleType.FREQUENCY;
    }

    @Override
    public RuleHitResult handle(RuleContext ctx, RiskRule rule) {
        FrequencyConfig config = parser.parse(rule.getRuleType(), rule.getThresholdConfig(), FrequencyConfig.class);
        if (config.getWindowSeconds() == null || config.getWindowSeconds() <= 0
                || config.getMaxCount() == null || config.getMaxCount() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "FREQUENCY 规则配置非法，需正数 windowSeconds 与 maxCount");
        }
        String key = String.format(KEY_TEMPLATE, rule.getRuleCode(), ctx.getUserId());
        long now = System.currentTimeMillis();
        Long count = redisTemplate.execute(frequencyWindowScript, List.of(key),
                String.valueOf(now),
                String.valueOf(config.getWindowSeconds() * 1000),
                ctx.getTransactionId());
        if (count == null || count <= config.getMaxCount()) {
            return null;
        }
        String detail = String.format("窗口 %ds 内交易次数 %d > 阈值 %d",
                config.getWindowSeconds(), count, config.getMaxCount());
        return RuleHitResult.of(rule, detail);
    }
}
