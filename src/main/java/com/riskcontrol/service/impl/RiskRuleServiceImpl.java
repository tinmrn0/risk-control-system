package com.riskcontrol.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.dto.RuleSaveRequest;
import com.riskcontrol.entity.RiskRule;
import com.riskcontrol.mapper.RiskRuleMapper;
import com.riskcontrol.rule.RuleConfigParser;
import com.riskcontrol.service.RiskRuleService;
import com.riskcontrol.vo.PageVO;
import com.riskcontrol.vo.RuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RiskRuleServiceImpl implements RiskRuleService {

    /** 各规则类型阈值配置的必填字段，保存时校验，避免脏配置到执行期才失败 */
    private static final Map<RuleType, Set<String>> REQUIRED_CONFIG_KEYS = Map.of(
            RuleType.LIMIT, Set.of("maxAmount"),
            RuleType.FREQUENCY, Set.of("windowSeconds", "maxCount"),
            RuleType.NAMELIST, Set.of(),
            RuleType.TIME_WINDOW, Set.of("startTime", "endTime")
    );

    private final RiskRuleMapper ruleMapper;
    private final RuleConfigParser ruleConfigParser;

    @Override
    public PageVO<RuleVO> page(long pageNum, long pageSize, String ruleType, String status) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<RiskRule>()
                .eq(ruleType != null && !ruleType.isBlank(), RiskRule::getRuleType, ruleType)
                .eq(status != null && !status.isBlank(), RiskRule::getStatus, status)
                .orderByAsc(RiskRule::getPriority);
        IPage<RuleVO> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper)
                .convert(RuleVO::from);
        return PageVO.of(page);
    }

    @Override
    public RuleVO create(RuleSaveRequest request) {
        validateConfig(request.getRuleType(), request.getThresholdConfig());
        Long count = ruleMapper.selectCount(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getRuleCode, request.getRuleCode()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "规则编码已存在: " + request.getRuleCode());
        }
        RiskRule rule = new RiskRule();
        copyProperties(request, rule);
        ruleMapper.insert(rule);
        return RuleVO.from(rule);
    }

    @Override
    public RuleVO update(Long id, RuleSaveRequest request) {
        RiskRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "规则不存在: " + id);
        }
        validateConfig(request.getRuleType(), request.getThresholdConfig());
        Long count = ruleMapper.selectCount(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getRuleCode, request.getRuleCode())
                .ne(RiskRule::getId, id));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "规则编码已存在: " + request.getRuleCode());
        }
        copyProperties(request, exist);
        ruleMapper.updateById(exist);
        return RuleVO.from(exist);
    }

    private void validateConfig(String ruleType, String thresholdConfig) {
        RuleType type = RuleType.of(ruleType);
        Map<String, Object> config = ruleConfigParser.parseMap(type, thresholdConfig);
        for (String requiredKey : REQUIRED_CONFIG_KEYS.get(type)) {
            if (!config.containsKey(requiredKey)) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        type.name() + " 规则配置缺少必填字段: " + requiredKey);
            }
        }
    }

    private void copyProperties(RuleSaveRequest request, RiskRule rule) {
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setPriority(request.getPriority());
        rule.setStatus(request.getStatus());
        rule.setThresholdConfig(request.getThresholdConfig());
        rule.setAction(request.getAction());
    }
}
