package com.mf.fertilizer.content.service;

import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.entity.Favorite;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;

import java.util.List;
import java.util.Map;

public interface ContentInteractionApplicationService {

    PageVO<Favorite> listFavorites(Long userId, PageDTO page, String targetType, String targetId);

    Favorite addFavorite(Long userId, Favorite favorite);

    void removeFavorite(Long userId, Long favoriteId);

    Map<String, Object> checkLike(Long userId, String targetType, String targetId);

    Map<String, Object> toggleLike(Long userId, Map<String, Object> body);

    PageVO<CommunityComment> listComments(PageDTO page, String targetType, String targetId);

    List<CommunityComment> listReplies(Long parentId);

    CommunityComment postComment(Long userId, Map<String, Object> body);
}
