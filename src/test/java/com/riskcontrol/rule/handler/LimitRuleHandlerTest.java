package com.riskcontrol.rule.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskcontrol.common.BizException;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitRuleHandlerTest {

    private LimitRuleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LimitRuleHandler(new RuleConfigParser(new ObjectMapper()));
    }

    private RiskRule rule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("LIMIT_SINGLE");
        rule.setRuleName("单笔限额");
        rule.setRuleType("LIMIT");
        rule.setAction("REVIEW");
        rule.setThresholdConfig("{\"maxAmount\": 100.00}");
        return rule;
    }

    private RuleContext ctx(String amount) {
        return RuleContext.builder().amount(new BigDecimal(amount)).build();
    }

    @Test
    void shouldHitWhenAmountExceedsLimit() {
        RuleHitResult hit = handler.handle(ctx("100.01"), rule());
        assertNotNull(hit);
        assertEquals("REVIEW", hit.getAction().name());
        assertTrue(hit.getHitDetail().contains("100.01"));
    }

    @Test
    void shouldNotHitWhenAmountEqualsLimit() {
        assertNull(handler.handle(ctx("100.00"), rule()));
    }

    @Test
    void shouldNotHitWhenAmountBelowLimit() {
        assertNull(handler.handle(ctx("99.99"), rule()));
    }

    @Test
    void shouldThrowWhenConfigInvalid() {
        RiskRule bad = rule();
        bad.setThresholdConfig("{invalid}");
        assertThrows(BizException.class, () -> handler.handle(ctx("100.01"), bad));
    }
}
