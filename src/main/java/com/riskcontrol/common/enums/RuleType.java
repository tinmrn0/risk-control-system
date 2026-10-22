package com.riskcontrol.common.enums;

import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RuleType {

    /** 单笔限额 */
    LIMIT,
    /** 频率限制 */
    FREQUENCY,
    /** 名单 */
    NAMELIST,
    /** 时段异常 */
    TIME_WINDOW;

    public static RuleType of(String value) {
        return Arrays.stream(values())
                .filter(t -> t.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_ERROR, "非法规则类型: " + value));
    }
}
