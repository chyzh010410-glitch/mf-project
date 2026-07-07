package com.mf.fertilizer.user.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.user.entity.PointsRecord;
import com.mf.fertilizer.user.service.UserPointsApplicationService;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/client/points")
@RequiredArgsConstructor
public class ClientPointsController {
    private final UserPointsApplicationService userPointsApplicationService;

    @GetMapping
    public ResultVO<?> balance() {
        return ResultVO.success(Map.of("points", userPointsApplicationService.getBalance(UserContext.getUserId())));
    }

    @GetMapping("/records")
    public ResultVO<PageVO<PointsRecord>> records(@ModelAttribute PageDTO page) {
        return ResultVO.success(userPointsApplicationService.listRecords(UserContext.getUserId(), page));
    }
}
