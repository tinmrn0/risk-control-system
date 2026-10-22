package com.riskcontrol.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskcontrol.cache.NameListCache;
import com.riskcontrol.common.enums.CommonStatus;
import com.riskcontrol.common.enums.DecisionType;
import com.riskcontrol.common.enums.ListType;
import com.riskcontrol.common.enums.NameBizType;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.dto.RiskCheckRequest;
import com.riskcontrol.dto.RiskCheckResponse;
import com.riskcontrol.entity.RiskHitRecord;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.mapper.RiskHitRecordMapper;
import com.riskcontrol.mapper.RiskRuleMapper;
import com.riskcontrol.rule.RiskRuleHandler;
import com.riskcontrol.rule.RiskRuleHandlerFactory;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import com.riskcontrol.service.RiskCheckService;
import com.riskcontrol.vo.HitRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 风控检查主流程：
 * 1. 白名单短路（USER_ID / ACCOUNT_NO 任一命中直接 PASS，不落命中记录）
 * 2. 加载 ENABLED 规则按 priority 升序执行，命中收集
 * 3. 决策归并取最严重级（REJECT > REVIEW > PASS）
 * 4. 命中规则落 hit_record（decision 记录最终决策）
 * 单条规则执行异常只降级该规则，不影响整体可用性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskCheckServiceImpl implements RiskCheckService {

    private final RiskRuleMapper ruleMapper;
    private final RiskHitRecordMapper hitRecordMapper;
    private final NameListCache nameListCache;
    private final RiskRuleHandlerFactory handlerFactory;

    @Override
    public RiskCheckResponse check(RiskCheckRequest request) {
        long begin = System.currentTimeMillis();
        RuleContext ctx = RuleContext.builder()
                .transactionId(request.getTransactionId())
                .userId(request.getUserId())
                .accountNo(request.getAccountNo())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .channel(request.getChannel())
                .occurTime(request.getOccurTime())
                .build();

        if (isWhiteListed(ctx)) {
            return buildResponse(request, DecisionType.PASS, List.of(), begin);
        }

        List<RuleHitResult> hits = executeRules(ctx);
        DecisionType decision = hits.stream()
                .map(RuleHitResult::getAction)
                .reduce(DecisionType::mostSevere)
                .orElse(DecisionType.PASS);

        saveHitRecords(request, decision, hits);
        return buildResponse(request, decision, hits, begin);
    }

    private boolean isWhiteListed(RuleContext ctx) {
        return nameListCache.hit(ListType.WHITE.name(), NameBizType.USER_ID.name(), ctx.getUserId())
                || nameListCache.hit(ListType.WHITE.name(), NameBizType.ACCOUNT_NO.name(), ctx.getAccountNo());
    }

    private List<RuleHitResult> executeRules(RuleContext ctx) {
        List<RiskRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, CommonStatus.ENABLED.name())
                .orderByAsc(RiskRule::getPriority));
        List<RuleHitResult> hits = new ArrayList<>();
        for (RiskRule rule : rules) {
            try {
                RiskRuleHandler handler = handlerFactory.get(RuleType.of(rule.getRuleType()));
                if (handler == null) {
                    log.warn("规则 {} 类型 {} 无对应处理器，跳过", rule.getRuleCode(), rule.getRuleType());
                    continue;
                }
                RuleHitResult hit = handler.handle(ctx, rule);
                if (hit != null) {
                    hits.add(hit);
                }
            } catch (Exception e) {
                log.error("规则 {} 执行异常，降级跳过", rule.getRuleCode(), e);
            }
        }
        return hits;
    }

    private void saveHitRecords(RiskCheckRequest request, DecisionType decision, List<RuleHitResult> hits) {
        for (RuleHitResult hit : hits) {
            RiskHitRecord record = new RiskHitRecord();
            record.setTransactionId(request.getTransactionId());
            record.setUserId(request.getUserId());
            record.setRuleCode(hit.getRuleCode());
            record.setDecision(decision.name());
            record.setHitDetail(hit.getHitDetail());
            hitRecordMapper.insert(record);
        }
    }

    private RiskCheckResponse buildResponse(RiskCheckRequest request, DecisionType decision,
                                            List<RuleHitResult> hits, long begin) {
        int costMs = (int) Math.min(System.currentTimeMillis() - begin, Integer.MAX_VALUE);
        return RiskCheckResponse.builder()
                .transactionId(request.getTransactionId())
                .decision(decision.name())
                .hitRules(hits.stream().map(HitRuleVO::from).toList())
                .costMs(costMs)
                .build();
    }
}
