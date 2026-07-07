package com.mf.fertilizer.content.controller.admin;

import com.mf.fertilizer.annotation.OperationLog;
import com.mf.fertilizer.content.dto.admin.ArticleSaveDTO;
import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.service.ContentAdminApplicationService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import com.mf.fertilizer.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ContentAdminApplicationService contentAdminApplicationService;

    @GetMapping
    public ResultVO<PageVO<EncyclopediaArticle>> list(@ModelAttribute PageDTO page,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer isPublished,
                                                      @RequestParam(required = false) Integer isTop,
                                                      @RequestParam(required = false) Integer isRecommend) {
        return ResultVO.success(contentAdminApplicationService.listArticles(
                page, keyword, isPublished, isTop, isRecommend));
    }

    @GetMapping("/{id}")
    public ResultVO<EncyclopediaArticle> detail(@PathVariable Long id) {
        return ResultVO.success(contentAdminApplicationService.getArticle(id));
    }

    @PostMapping
    @OperationLog(module = "文章管理", action = "新增")
    public ResultVO<?> save(@RequestBody ArticleSaveDTO dto) {
        contentAdminApplicationService.createArticle(dto);
        return ResultVO.success();
    }

    @PutMapping("/{id}")
    @OperationLog(module = "文章管理", action = "编辑")
    public ResultVO<?> update(@PathVariable Long id, @RequestBody ArticleSaveDTO dto) {
        contentAdminApplicationService.updateArticle(id, dto);
        return ResultVO.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "文章管理", action = "删除")
    public ResultVO<?> delete(@PathVariable Long id) {
        contentAdminApplicationService.deleteArticle(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/publish")
    @OperationLog(module = "文章管理", action = "发布切换")
    public ResultVO<?> togglePublish(@PathVariable Long id) {
        contentAdminApplicationService.toggleArticlePublish(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/top")
    public ResultVO<?> toggleTop(@PathVariable Long id) {
        contentAdminApplicationService.toggleArticleTop(id);
        return ResultVO.success();
    }

    @PutMapping("/{id}/recommend")
    public ResultVO<?> toggleRecommend(@PathVariable Long id) {
        contentAdminApplicationService.toggleArticleRecommend(id);
        return ResultVO.success();
    }
}
