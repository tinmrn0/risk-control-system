package com.riskcontrol.rule.handler;

import com.riskcontrol.common.enums.ListType;
import com.riskcontrol.common.enums.NameBizType;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.cache.NameListCache;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.rule.RuleContext;
import com.riskcontrol.rule.RuleHitResult;
import com.riskcontrol.rule.RiskRuleHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** R3 黑名单：userId / accountNo 任一命中黑名单即命中（白名单短路在主流程前置处理） */
@Component
@RequiredArgsConstructor
public class NameListRuleHandler implements RiskRuleHandler {

    private final NameListCache nameListCache;

    @Override
    public RuleType supports() {
        return RuleType.NAMELIST;
    }

    @Override
    public RuleHitResult handle(RuleContext ctx, RiskRule rule) {
        if (nameListCache.hit(ListType.BLACK.name(), NameBizType.USER_ID.name(), ctx.getUserId())) {
            return RuleHitResult.of(rule, "userId 命中黑名单");
        }
        if (nameListCache.hit(ListType.BLACK.name(), NameBizType.ACCOUNT_NO.name(), ctx.getAccountNo())) {
            return RuleHitResult.of(rule, "accountNo 命中黑名单");
        }
        return null;
    }
}
