package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.content.entity.Favorite;
import com.mf.fertilizer.content.service.ContentInteractionApplicationService;
import com.mf.fertilizer.context.UserContext;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/favorites")
@RequiredArgsConstructor
public class ClientFavoriteController {

    private final ContentInteractionApplicationService contentInteractionApplicationService;

    @GetMapping
    public ResultVO<PageVO<Favorite>> list(@ModelAttribute PageDTO page,
                                           @RequestParam(name = "targetType", required = false) String targetType,
                                           @RequestParam(name = "targetId", required = false, defaultValue = "") String targetId) {
        return ResultVO.success(contentInteractionApplicationService.listFavorites(UserContext.getUserId(), page, targetType, targetId));
    }

    @PostMapping
    public ResultVO<Favorite> add(@RequestBody Favorite favorite) {
        return ResultVO.success(contentInteractionApplicationService.addFavorite(UserContext.getUserId(), favorite));
    }

    @DeleteMapping("/{id}")
    public ResultVO<?> remove(@PathVariable Long id) {
        contentInteractionApplicationService.removeFavorite(UserContext.getUserId(), id);
        return ResultVO.success();
    }
}
