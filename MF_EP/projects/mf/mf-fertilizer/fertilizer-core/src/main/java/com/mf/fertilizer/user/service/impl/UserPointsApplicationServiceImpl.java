package com.mf.fertilizer.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.PointsRecord;
import com.mf.fertilizer.user.service.PointsRecordService;
import com.mf.fertilizer.user.service.UserPointsApplicationService;
import com.mf.fertilizer.user.service.UserService;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointsApplicationServiceImpl implements UserPointsApplicationService {

    private final PointsRecordService pointsRecordService;
    private final UserService userService;

    @Override
    public Integer getBalance(Long userId) {
        var user = userService.getById(userId);
        return user != null && user.getPoints() != null ? user.getPoints() : 0;
    }

    @Override
    public PageVO<PointsRecord> listRecords(Long userId, PageDTO page) {
        var result = pointsRecordService.lambdaQuery()
                .eq(PointsRecord::getUserId, userId)
                .orderByDesc(PointsRecord::getCreateTime)
                .page(new Page<>(page.getPage(), page.getSize()));
        return PageVO.of(page, result);
    }
}
