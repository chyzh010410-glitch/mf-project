package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.service.PlatformPortalApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client/faq")
@RequiredArgsConstructor
public class ClientFaqController {

    private final PlatformPortalApplicationService platformPortalApplicationService;

    @GetMapping
    public ResultVO<List<Faq>> list(@RequestParam(name = "category", required = false) String category) {
        return ResultVO.success(platformPortalApplicationService.listPublishedFaqs(category));
    }
}
