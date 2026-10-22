package com.riskcontrol.rule.handler;

import com.riskcontrol.cache.NameListCache;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NameListRuleHandlerTest {

    @Mock
    private NameListCache nameListCache;

    private NameListRuleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NameListRuleHandler(nameListCache);
    }

    private RiskRule rule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("NAMELIST_BLACK");
        rule.setRuleName("黑名单");
        rule.setRuleType("NAMELIST");
        rule.setAction("REJECT");
        rule.setThresholdConfig("{}");
        return rule;
    }

    private RuleContext ctx() {
        return RuleContext.builder().userId("U1001").accountNo("A1001").build();
    }

    @Test
    void shouldHitWhenUserIdInBlackList() {
        when(nameListCache.hit("BLACK", "USER_ID", "U1001")).thenReturn(true);
        RuleHitResult hit = handler.handle(ctx(), rule());
        assertNotNull(hit);
        assertTrue(hit.getHitDetail().contains("userId"));
    }

    @Test
    void shouldHitWhenAccountNoInBlackList() {
        when(nameListCache.hit("BLACK", "USER_ID", "U1001")).thenReturn(false);
        when(nameListCache.hit("BLACK", "ACCOUNT_NO", "A1001")).thenReturn(true);
        RuleHitResult hit = handler.handle(ctx(), rule());
        assertNotNull(hit);
        assertTrue(hit.getHitDetail().contains("accountNo"));
    }

    @Test
    void shouldNotHitWhenNotInBlackList() {
        when(nameListCache.hit("BLACK", "USER_ID", "U1001")).thenReturn(false);
        when(nameListCache.hit("BLACK", "ACCOUNT_NO", "A1001")).thenReturn(false);
        assertNull(handler.handle(ctx(), rule()));
    }
}
