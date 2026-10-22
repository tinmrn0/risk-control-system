package com.riskcontrol.rule;

import com.riskcontrol.common.enums.RuleType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 规则处理器路由：Spring 注入全部实现，按规则类型分发 */
@Component
public class RiskRuleHandlerFactory {

    private final Map<RuleType, RiskRuleHandler> handlers;

    public RiskRuleHandlerFactory(List<RiskRuleHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(RiskRuleHandler::supports, Function.identity(),
                        (a, b) -> a, () -> new EnumMap<>(RuleType.class)));
    }

    /** 未注册的规则类型返回 null，调用方跳过并告警 */
    public RiskRuleHandler get(RuleType ruleType) {
        return handlers.get(ruleType);
    }
}
