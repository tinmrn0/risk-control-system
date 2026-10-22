package com.riskcontrol.rule.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskcontrol.common.BizException;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrequencyRuleHandlerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private FrequencyRuleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FrequencyRuleHandler(redisTemplate, new DefaultRedisScript<>(),
                new RuleConfigParser(new ObjectMapper()));
    }

    private RiskRule rule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("FREQ_10MIN");
        rule.setRuleName("10分钟交易频率");
        rule.setRuleType("FREQUENCY");
        rule.setAction("REVIEW");
        rule.setThresholdConfig("{\"windowSeconds\": 600, \"maxCount\": 10}");
        return rule;
    }

    private RuleContext ctx() {
        return RuleContext.builder().transactionId("T1001").userId("U1001").build();
    }

    @Test
    void shouldHitWhenCountExceedsThreshold() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(),
                any(), any(), any())).thenReturn(11L);
        RuleHitResult hit = handler.handle(ctx(), rule());
        assertNotNull(hit);
        assertTrue(hit.getHitDetail().contains("11"));
    }

    @Test
    void shouldNotHitWhenCountEqualsThreshold() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(),
                any(), any(), any())).thenReturn(10L);
        assertNull(handler.handle(ctx(), rule()));
    }

    @Test
    void shouldNotHitWhenCountIsNull() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(),
                any(), any(), any())).thenReturn(null);
        assertNull(handler.handle(ctx(), rule()));
    }

    @Test
    void shouldThrowWhenConfigInvalid() {
        RiskRule bad = rule();
        bad.setThresholdConfig("{\"windowSeconds\": 0, \"maxCount\": 10}");
        assertThrows(BizException.class, () -> handler.handle(ctx(), bad));
    }
}
