package com.mf.fertilizer.content.service;

import com.mf.fertilizer.content.dto.admin.ArticleSaveDTO;
import com.mf.fertilizer.content.dto.admin.EncyclopediaSaveDTO;
import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;

public interface ContentAdminApplicationService {

    PageVO<EncyclopediaArticle> listArticles(PageDTO page, String keyword, Integer isPublished,
                                             Integer isTop, Integer isRecommend);

    EncyclopediaArticle getArticle(Long id);

    void createArticle(ArticleSaveDTO dto);

    void updateArticle(Long id, ArticleSaveDTO dto);

    void deleteArticle(Long id);

    void toggleArticlePublish(Long id);

    void toggleArticleTop(Long id);

    void toggleArticleRecommend(Long id);

    PageVO<EncyclopediaEntry> listEntries(PageDTO page, String keyword, Integer isPublished);

    EncyclopediaEntry getEntry(Long id);

    void createEntry(EncyclopediaSaveDTO dto);

    void updateEntry(Long id, EncyclopediaSaveDTO dto);

    void deleteEntry(Long id);

    void toggleEntryPublish(Long id);

    PageVO<CommunityComment> listComments(PageDTO page, String targetType, String keyword, Integer isDeleted);

    void hideComment(Long id);

    void restoreComment(Long id);
}
