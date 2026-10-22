package com.riskcontrol.service;

import com.riskcontrol.dto.HitRecordPageQuery;
import com.riskcontrol.vo.HitRecordVO;
import com.riskcontrol.vo.PageVO;

public interface HitRecordService {

    PageVO<HitRecordVO> page(HitRecordPageQuery query);
}
