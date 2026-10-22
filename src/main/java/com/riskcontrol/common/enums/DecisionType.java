package com.riskcontrol.common.enums;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风险决策：severity 数值越大越严重，最终决策取所有命中规则中最严重级。
 */
@Getter
@AllArgsConstructor
public enum DecisionType {

    PASS(1),
    REVIEW(2),
    REJECT(3);

    private final int severity;

    public static DecisionType mostSevere(DecisionType a, DecisionType b) {
        return a.severity >= b.severity ? a : b;
    }

    public static DecisionType of(String value) {
        return Arrays.stream(values())
                .filter(t -> t.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_ERROR, "非法决策值: " + value));
    }
}
