package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.service.ContentCatalogApplicationService;
import com.mf.fertilizer.content.service.EncyclopediaArticleService;
import com.mf.fertilizer.content.service.EncyclopediaEntryService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentCatalogApplicationServiceImpl implements ContentCatalogApplicationService {

    private final EncyclopediaArticleService articleService;
    private final EncyclopediaEntryService entryService;

    @Override
    public PageVO<EncyclopediaArticle> listArticles(PageDTO page, Long categoryId, Integer isRecommend) {
        var wrapper = new LambdaQueryWrapper<EncyclopediaArticle>()
                .eq(EncyclopediaArticle::getIsPublished, 1)
                .eq(categoryId != null, EncyclopediaArticle::getCategoryId, categoryId)
                .eq(isRecommend != null, EncyclopediaArticle::getIsRecommend, isRecommend)
                .orderByDesc(EncyclopediaArticle::getCreateTime);
        var result = articleService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public EncyclopediaArticle getArticleDetail(Long id) {
        EncyclopediaArticle article = articleService.getById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        article.setViewCount(safeIncrement(article.getViewCount()));
        articleService.updateById(article);
        return article;
    }

    @Override
    public PageVO<EncyclopediaEntry> listEntries(PageDTO page, String keyword, Long categoryId) {
        var wrapper = new LambdaQueryWrapper<EncyclopediaEntry>()
                .eq(EncyclopediaEntry::getIsPublished, 1)
                .like(keyword != null, EncyclopediaEntry::getName, keyword)
                .eq(categoryId != null, EncyclopediaEntry::getCategoryId, categoryId)
                .orderByDesc(EncyclopediaEntry::getViewCount);
        var result = entryService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public EncyclopediaEntry getEntryDetail(Long id) {
        EncyclopediaEntry entry = entryService.getById(id);
        if (entry == null) {
            throw new BusinessException(404, "百科条目不存在");
        }
        entry.setViewCount(safeIncrement(entry.getViewCount()));
        entryService.updateById(entry);
        return entry;
    }

    private Integer safeIncrement(Integer value) {
        return value == null ? 1 : value + 1;
    }
}
