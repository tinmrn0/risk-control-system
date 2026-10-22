package com.riskcontrol.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.riskcontrol.dto.HitRecordPageQuery;
import com.riskcontrol.entity.RiskHitRecord;
import com.riskcontrol.mapper.RiskHitRecordMapper;
import com.riskcontrol.service.HitRecordService;
import com.riskcontrol.vo.HitRecordVO;
import com.riskcontrol.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HitRecordServiceImpl implements HitRecordService {

    private final RiskHitRecordMapper hitRecordMapper;

    @Override
    public PageVO<HitRecordVO> page(HitRecordPageQuery query) {
        LambdaQueryWrapper<RiskHitRecord> wrapper = new LambdaQueryWrapper<RiskHitRecord>()
                .eq(query.getUserId() != null && !query.getUserId().isBlank(),
                        RiskHitRecord::getUserId, query.getUserId())
                .eq(query.getDecision() != null && !query.getDecision().isBlank(),
                        RiskHitRecord::getDecision, query.getDecision())
                .ge(query.getStartTime() != null, RiskHitRecord::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, RiskHitRecord::getCreateTime, query.getEndTime())
                .orderByDesc(RiskHitRecord::getId);
        IPage<HitRecordVO> page = hitRecordMapper.selectPage(
                        new Page<>(query.getPageNum(), query.getPageSize()), wrapper)
                .convert(HitRecordVO::from);
        return PageVO.of(page);
    }
}
