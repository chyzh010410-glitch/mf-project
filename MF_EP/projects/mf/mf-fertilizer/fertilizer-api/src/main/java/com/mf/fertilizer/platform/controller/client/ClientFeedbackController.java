package com.mf.fertilizer.platform.controller.client;

import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.platform.dto.client.FeedbackSubmitDTO;
import com.mf.fertilizer.platform.service.PlatformPortalApplicationService;
import com.mf.fertilizer.vo.ResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/feedback")
@RequiredArgsConstructor
public class ClientFeedbackController {

    private final PlatformPortalApplicationService platformPortalApplicationService;

    @PostMapping
    public ResultVO<?> submit(@Valid @RequestBody FeedbackSubmitDTO dto) {
        platformPortalApplicationService.submitFeedback(UserContext.getUserId(), dto);
        return ResultVO.success();
    }
}
