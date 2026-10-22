package com.riskcontrol.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.RuleType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 规则阈值配置解析器：保存时校验与执行期解析共用同一实现，保证两端约定一致。
 */
@Component
public class RuleConfigParser {

    private final ObjectMapper objectMapper;

    public RuleConfigParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 解析为 Map，供保存时校验必填字段 */
    public Map<String, Object> parseMap(RuleType ruleType, String thresholdConfig) {
        try {
            Map<String, Object> map = objectMapper.readValue(
                    thresholdConfig, new TypeReference<Map<String, Object>>() {});
            if (map == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, ruleType + " 规则配置必须是 JSON 对象");
            }
            return map;
        } catch (BizException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, ruleType + " 规则配置不是合法 JSON 对象");
        }
    }

    /** 解析为具体配置类，供执行期使用 */
    public <T> T parse(String ruleType, String thresholdConfig, Class<T> type) {
        try {
            return objectMapper.readValue(thresholdConfig, type);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, ruleType + " 规则配置非法");
        }
    }
}
