package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.service.PlatformPortalApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client/activities")
@RequiredArgsConstructor
public class ClientActivityController {

    private final PlatformPortalApplicationService platformPortalApplicationService;

    @GetMapping
    public ResultVO<List<ActivityEntity>> list() {
        return ResultVO.success(platformPortalApplicationService.listActiveActivities());
    }

    @GetMapping("/{id}")
    public ResultVO<ActivityEntity> detail(@PathVariable Long id) {
        return ResultVO.success(platformPortalApplicationService.getActivityDetail(id));
    }
}
