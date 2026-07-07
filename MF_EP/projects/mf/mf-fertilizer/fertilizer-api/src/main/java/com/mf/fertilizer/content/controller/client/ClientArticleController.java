package com.mf.fertilizer.content.controller.client;

import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.service.ContentCatalogApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/articles")
@RequiredArgsConstructor
public class ClientArticleController {

    private final ContentCatalogApplicationService contentCatalogApplicationService;

    @GetMapping
    public ResultVO<PageVO<EncyclopediaArticle>> list(@ModelAttribute PageDTO page,
                                                      @RequestParam(name = "categoryId", required = false) Long categoryId,
                                                      @RequestParam(name = "isRecommend", required = false) Integer isRecommend) {
        return ResultVO.success(contentCatalogApplicationService.listArticles(page, categoryId, isRecommend));
    }

    @GetMapping("/{id}")
    public ResultVO<EncyclopediaArticle> detail(@PathVariable Long id) {
        return ResultVO.success(contentCatalogApplicationService.getArticleDetail(id));
    }
}
