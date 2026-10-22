package com.riskcontrol.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskcontrol.cache.NameListCache;
import com.riskcontrol.dto.RiskCheckRequest;
import com.riskcontrol.dto.RiskCheckResponse;
import com.riskcontrol.entity.RiskHitRecord;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.mapper.RiskHitRecordMapper;
import com.riskcontrol.mapper.RiskRuleMapper;
import com.riskcontrol.rule.RiskRuleHandlerFactory;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.rule.handler.LimitRuleHandler;
import com.riskcontrol.rule.handler.NameListRuleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskCheckServiceTest {

    @Mock
    private RiskRuleMapper ruleMapper;

    @Mock
    private RiskHitRecordMapper hitRecordMapper;

    @Mock
    private NameListCache nameListCache;

    private RiskCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        RuleConfigParser parser = new RuleConfigParser(new ObjectMapper().findAndRegisterModules());
        RiskRuleHandlerFactory factory = new RiskRuleHandlerFactory(
                List.of(new LimitRuleHandler(parser), new NameListRuleHandler(nameListCache)));
        service = new RiskCheckServiceImpl(ruleMapper, hitRecordMapper, nameListCache, factory);
    }

    private RiskCheckRequest request() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setTransactionId("T1001");
        request.setUserId("U1001");
        request.setAccountNo("A1001");
        request.setAmount(new BigDecimal("100.01"));
        request.setCurrency("CNY");
        request.setChannel("APP");
        request.setOccurTime(LocalDateTime.of(2026, 10, 22, 21, 30));
        return request;
    }

    private RiskRule nameListRule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("NAMELIST_BLACK");
        rule.setRuleName("黑名单");
        rule.setRuleType("NAMELIST");
        rule.setPriority(1);
        rule.setStatus("ENABLED");
        rule.setThresholdConfig("{}");
        rule.setAction("REJECT");
        return rule;
    }

    private RiskRule limitRule() {
        RiskRule rule = new RiskRule();
        rule.setRuleCode("LIMIT_SINGLE");
        rule.setRuleName("单笔限额");
        rule.setRuleType("LIMIT");
        rule.setPriority(10);
        rule.setStatus("ENABLED");
        rule.setThresholdConfig("{\"maxAmount\": 100.00}");
        rule.setAction("REVIEW");
        return rule;
    }

    @Test
    void shouldShortCircuitPassWhenWhiteListed() {
        when(nameListCache.hit("WHITE", "USER_ID", "U1001")).thenReturn(true);
        RiskCheckResponse response = service.check(request());
        assertEquals("PASS", response.getDecision());
        assertTrue(response.getHitRules().isEmpty());
        verify(ruleMapper, never()).selectList(any());
        verify(hitRecordMapper, never()).insert(any(RiskHitRecord.class));
    }

    @Test
    void shouldRejectAndSaveRecordsWhenBlackListedAndLimitExceeded() {
        when(nameListCache.hit(anyString(), anyString(), anyString())).thenReturn(false);
        when(nameListCache.hit("BLACK", "USER_ID", "U1001")).thenReturn(true);
        when(ruleMapper.selectList(any())).thenReturn(List.of(nameListRule(), limitRule()));

        RiskCheckResponse response = service.check(request());

        assertEquals("REJECT", response.getDecision());
        assertEquals(2, response.getHitRules().size());
        verify(hitRecordMapper, times(2)).insert(any(RiskHitRecord.class));
    }

    @Test
    void shouldPassWhenNoRulesHit() {
        when(nameListCache.hit(anyString(), anyString(), anyString())).thenReturn(false);
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        RiskCheckResponse response = service.check(request());

        assertEquals("PASS", response.getDecision());
        assertTrue(response.getHitRules().isEmpty());
        verify(hitRecordMapper, never()).insert(any(RiskHitRecord.class));
    }
}
