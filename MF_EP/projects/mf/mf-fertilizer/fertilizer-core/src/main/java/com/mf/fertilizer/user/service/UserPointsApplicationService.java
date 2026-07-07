package com.mf.fertilizer.user.service;

import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.PointsRecord;
import com.mf.fertilizer.vo.PageVO;

public interface UserPointsApplicationService {

    Integer getBalance(Long userId);

    PageVO<PointsRecord> listRecords(Long userId, PageDTO page);
}
