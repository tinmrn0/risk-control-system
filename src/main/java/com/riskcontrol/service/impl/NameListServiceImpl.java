package com.riskcontrol.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.riskcontrol.cache.NameListCache;
import com.riskcontrol.common.BizException;
import com.riskcontrol.common.ErrorCode;
import com.riskcontrol.common.enums.CommonStatus;
import com.riskcontrol.dto.NameListRequest;
import com.riskcontrol.entity.RiskNameList;
import com.riskcontrol.mapper.RiskNameListMapper;
import com.riskcontrol.service.NameListService;
import com.riskcontrol.vo.NameListVO;
import com.riskcontrol.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NameListServiceImpl implements NameListService {

    private final RiskNameListMapper nameListMapper;
    private final NameListCache nameListCache;

    @Override
    public PageVO<NameListVO> page(long pageNum, long pageSize, String listType, String bizType) {
        LambdaQueryWrapper<RiskNameList> wrapper = new LambdaQueryWrapper<RiskNameList>()
                .eq(listType != null && !listType.isBlank(), RiskNameList::getListType, listType)
                .eq(bizType != null && !bizType.isBlank(), RiskNameList::getBizType, bizType)
                .orderByDesc(RiskNameList::getId);
        IPage<NameListVO> page = nameListMapper.selectPage(new Page<>(pageNum, pageSize), wrapper)
                .convert(NameListVO::from);
        return PageVO.of(page);
    }

    @Override
    public NameListVO create(NameListRequest request) {
        Long count = nameListMapper.selectCount(new LambdaQueryWrapper<RiskNameList>()
                .eq(RiskNameList::getListType, request.getListType())
                .eq(RiskNameList::getBizType, request.getBizType())
                .eq(RiskNameList::getBizValue, request.getBizValue()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "名单记录已存在");
        }
        RiskNameList entity = new RiskNameList();
        entity.setListType(request.getListType());
        entity.setBizType(request.getBizType());
        entity.setBizValue(request.getBizValue());
        entity.setRemark(request.getRemark());
        entity.setStatus(StringUtils.hasText(request.getStatus())
                ? request.getStatus() : CommonStatus.ENABLED.name());
        nameListMapper.insert(entity);
        nameListCache.evict(request.getListType(), request.getBizType(), request.getBizValue());
        return NameListVO.from(entity);
    }

    @Override
    public void delete(Long id) {
        RiskNameList exist = nameListMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "名单记录不存在: " + id);
        }
        nameListMapper.deleteById(id);
        nameListCache.evict(exist.getListType(), exist.getBizType(), exist.getBizValue());
    }
}
