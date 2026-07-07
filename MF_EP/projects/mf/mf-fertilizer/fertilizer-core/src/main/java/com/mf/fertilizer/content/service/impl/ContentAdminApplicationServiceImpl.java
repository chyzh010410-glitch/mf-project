package com.mf.fertilizer.content.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.content.dto.admin.ArticleSaveDTO;
import com.mf.fertilizer.content.dto.admin.EncyclopediaSaveDTO;
import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.content.service.CommunityCommentService;
import com.mf.fertilizer.content.service.ContentAdminApplicationService;
import com.mf.fertilizer.content.service.EncyclopediaArticleService;
import com.mf.fertilizer.content.service.EncyclopediaEntryService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.exception.BusinessException;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentAdminApplicationServiceImpl implements ContentAdminApplicationService {

    private final EncyclopediaArticleService articleService;
    private final EncyclopediaEntryService entryService;
    private final CommunityCommentService commentService;

    @Override
    public PageVO<EncyclopediaArticle> listArticles(PageDTO page, String keyword, Integer isPublished,
                                                    Integer isTop, Integer isRecommend) {
        var wrapper = new LambdaQueryWrapper<EncyclopediaArticle>()
                .like(StrUtil.isNotBlank(keyword), EncyclopediaArticle::getTitle, keyword)
                .eq(isPublished != null, EncyclopediaArticle::getIsPublished, isPublished)
                .eq(isTop != null, EncyclopediaArticle::getIsTop, isTop)
                .eq(isRecommend != null, EncyclopediaArticle::getIsRecommend, isRecommend)
                .orderByDesc(EncyclopediaArticle::getIsTop)
                .orderByDesc(EncyclopediaArticle::getCreateTime);
        var result = articleService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public EncyclopediaArticle getArticle(Long id) {
        return requireArticle(id);
    }

    @Override
    public void createArticle(ArticleSaveDTO dto) {
        EncyclopediaArticle article = new EncyclopediaArticle();
        BeanUtils.copyProperties(dto, article);
        applyArticleDefaults(article);
        articleService.save(article);
    }

    @Override
    public void updateArticle(Long id, ArticleSaveDTO dto) {
        EncyclopediaArticle article = requireArticle(id);
        BeanUtils.copyProperties(dto, article);
        article.setId(id);
        articleService.updateById(article);
    }

    @Override
    public void deleteArticle(Long id) {
        articleService.removeById(id);
    }

    @Override
    public void toggleArticlePublish(Long id) {
        EncyclopediaArticle article = requireArticle(id);
        article.setIsPublished(toggleFlag(article.getIsPublished()));
        articleService.updateById(article);
    }

    @Override
    public void toggleArticleTop(Long id) {
        EncyclopediaArticle article = requireArticle(id);
        article.setIsTop(toggleFlag(article.getIsTop()));
        articleService.updateById(article);
    }

    @Override
    public void toggleArticleRecommend(Long id) {
        EncyclopediaArticle article = requireArticle(id);
        article.setIsRecommend(toggleFlag(article.getIsRecommend()));
        articleService.updateById(article);
    }

    @Override
    public PageVO<EncyclopediaEntry> listEntries(PageDTO page, String keyword, Integer isPublished) {
        var wrapper = new LambdaQueryWrapper<EncyclopediaEntry>()
                .and(StrUtil.isNotBlank(keyword), q -> q
                        .like(EncyclopediaEntry::getName, keyword)
                        .or().like(EncyclopediaEntry::getScientificName, keyword)
                        .or().like(EncyclopediaEntry::getAlias, keyword))
                .eq(isPublished != null, EncyclopediaEntry::getIsPublished, isPublished)
                .orderByDesc(EncyclopediaEntry::getCreateTime);
        var result = entryService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public EncyclopediaEntry getEntry(Long id) {
        return requireEntry(id);
    }

    @Override
    public void createEntry(EncyclopediaSaveDTO dto) {
        EncyclopediaEntry entry = new EncyclopediaEntry();
        BeanUtils.copyProperties(dto, entry);
        applyEntryDefaults(entry);
        entryService.save(entry);
    }

    @Override
    public void updateEntry(Long id, EncyclopediaSaveDTO dto) {
        EncyclopediaEntry entry = requireEntry(id);
        BeanUtils.copyProperties(dto, entry);
        entry.setId(id);
        entryService.updateById(entry);
    }

    @Override
    public void deleteEntry(Long id) {
        entryService.removeById(id);
    }

    @Override
    public void toggleEntryPublish(Long id) {
        EncyclopediaEntry entry = requireEntry(id);
        entry.setIsPublished(toggleFlag(entry.getIsPublished()));
        entryService.updateById(entry);
    }

    @Override
    public PageVO<CommunityComment> listComments(PageDTO page, String targetType, String keyword, Integer isDeleted) {
        var wrapper = new LambdaQueryWrapper<CommunityComment>()
                .eq(StrUtil.isNotBlank(targetType), CommunityComment::getTargetType, targetType)
                .eq(isDeleted != null, CommunityComment::getIsDeletedByAdmin, isDeleted)
                .like(StrUtil.isNotBlank(keyword), CommunityComment::getContent, keyword)
                .orderByDesc(CommunityComment::getCreateTime);
        var result = commentService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public void hideComment(Long id) {
        CommunityComment comment = requireComment(id);
        comment.setIsDeletedByAdmin(1);
        commentService.updateById(comment);
    }

    @Override
    public void restoreComment(Long id) {
        CommunityComment comment = requireComment(id);
        comment.setIsDeletedByAdmin(0);
        commentService.updateById(comment);
    }

    private EncyclopediaArticle requireArticle(Long id) {
        EncyclopediaArticle article = articleService.getById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return article;
    }

    private EncyclopediaEntry requireEntry(Long id) {
        EncyclopediaEntry entry = entryService.getById(id);
        if (entry == null) {
            throw new BusinessException(404, "百科条目不存在");
        }
        return entry;
    }

    private CommunityComment requireComment(Long id) {
        CommunityComment comment = commentService.getById(id);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        return comment;
    }

    private void applyArticleDefaults(EncyclopediaArticle article) {
        if (article.getIsPublished() == null) {
            article.setIsPublished(0);
        }
        if (article.getIsTop() == null) {
            article.setIsTop(0);
        }
        if (article.getIsRecommend() == null) {
            article.setIsRecommend(0);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
    }

    private void applyEntryDefaults(EncyclopediaEntry entry) {
        if (entry.getIsPublished() == null) {
            entry.setIsPublished(0);
        }
        if (entry.getViewCount() == null) {
            entry.setViewCount(0);
        }
    }

    private Integer toggleFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 0 : 1;
    }
}
