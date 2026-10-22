package com.riskcontrol.rule.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskcontrol.common.BizException;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWindowRuleHandlerTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeWindowRuleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TimeWindowRuleHandler(new RuleConfigParser(new ObjectMapper().findAndRegisterModules()));
    }

    private RiskRule rule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("TIME_WINDOW");
        rule.setRuleName("交易时段");
        rule.setRuleType("TIME_WINDOW");
        rule.setAction("REVIEW");
        rule.setThresholdConfig("{\"startTime\": \"09:00\", \"endTime\": \"22:00\"}");
        return rule;
    }

    private RuleContext ctx(String occurTime) {
        return RuleContext.builder().occurTime(LocalDateTime.parse(occurTime, FORMATTER)).build();
    }

    @Test
    void shouldNotHitWhenInsideWindow() {
        assertNull(handler.handle(ctx("2026-10-22 10:30:00"), rule()));
    }

    @Test
    void shouldNotHitAtWindowBoundaries() {
        assertNull(handler.handle(ctx("2026-10-22 09:00:00"), rule()));
        assertNull(handler.handle(ctx("2026-10-22 22:00:00"), rule()));
    }

    @Test
    void shouldHitWhenOutOfWindow() {
        RuleHitResult hit = handler.handle(ctx("2026-10-22 23:30:00"), rule());
        assertNotNull(hit);
        assertTrue(hit.getHitDetail().contains("23:30"));
    }

    @Test
    void shouldThrowWhenConfigMissing() {
        RiskRule bad = rule();
        bad.setThresholdConfig("{\"startTime\": \"09:00\"}");
        assertThrows(BizException.class, () -> handler.handle(ctx("2026-10-22 23:30:00"), bad));
    }
}
