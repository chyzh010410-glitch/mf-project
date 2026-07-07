package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.platform.service.PlatformPortalApplicationService;
import com.mf.fertilizer.platform.vo.client.HomePageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/home")
@RequiredArgsConstructor
public class ClientHomeController {

    private final PlatformPortalApplicationService platformPortalApplicationService;

    @GetMapping
    public ResultVO<HomePageVO> index() {
        return ResultVO.success(platformPortalApplicationService.getHomePage());
    }
}
