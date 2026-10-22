package com.riskcontrol.service;

import com.riskcontrol.dto.NameListRequest;
import com.riskcontrol.vo.NameListVO;
import com.riskcontrol.vo.PageVO;

public interface NameListService {

    PageVO<NameListVO> page(long pageNum, long pageSize, String listType, String bizType);

    NameListVO create(NameListRequest request);

    void delete(Long id);
}
