package com.mf.fertilizer.content.service;

import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.entity.EncyclopediaEntry;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;

public interface ContentCatalogApplicationService {

    PageVO<EncyclopediaArticle> listArticles(PageDTO page, Long categoryId, Integer isRecommend);

    EncyclopediaArticle getArticleDetail(Long id);

    PageVO<EncyclopediaEntry> listEntries(PageDTO page, String keyword, Long categoryId);

    EncyclopediaEntry getEntryDetail(Long id);
}
