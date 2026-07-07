package com.mf.fertilizer.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.fertilizer.content.entity.CommunityComment;
import com.mf.fertilizer.content.entity.CommunityLike;
import com.mf.fertilizer.content.entity.Favorite;
import com.mf.fertilizer.content.service.CommunityCommentService;
import com.mf.fertilizer.content.service.CommunityLikeService;
import com.mf.fertilizer.content.service.ContentInteractionApplicationService;
import com.mf.fertilizer.content.service.FavoriteService;
import com.mf.fertilizer.dto.PageDTO;
import com.mf.fertilizer.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentInteractionApplicationServiceImpl implements ContentInteractionApplicationService {

    private final FavoriteService favoriteService;
    private final CommunityLikeService likeService;
    private final CommunityCommentService commentService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageVO<Favorite> listFavorites(Long userId, PageDTO page, String targetType, String targetId) {
        Long parsedTargetId = parseOptionalLong(targetId);
        var wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(targetType != null && !targetType.isEmpty(), Favorite::getTargetType, targetType)
                .eq(parsedTargetId != null, Favorite::getTargetId, parsedTargetId)
                .orderByDesc(Favorite::getCreateTime);
        var result = favoriteService.page(new Page<>(page.getPage(), page.getSize()), wrapper);
        return PageVO.of(page, result);
    }

    @Override
    public Favorite addFavorite(Long userId, Favorite favorite) {
        favorite.setUserId(userId);
        Favorite existing = favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, favorite.getTargetType())
                .eq(Favorite::getTargetId, favorite.getTargetId())
                .one();
        if (existing != null) {
            return existing;
        }

        Favorite restored = findDeletedFavorite(userId, favorite.getTargetType(), favorite.getTargetId());
        if (restored != null) {
            jdbcTemplate.update("UPDATE favorite SET deleted=0 WHERE id=?", restored.getId());
            restored.setDeleted(0);
            return restored;
        }

        favoriteService.save(favorite);
        return favorite;
    }

    @Override
    public void removeFavorite(Long userId, Long favoriteId) {
        favoriteService.lambdaUpdate()
                .eq(Favorite::getId, favoriteId)
                .eq(Favorite::getUserId, userId)
                .remove();
    }

    @Override
    public Map<String, Object> checkLike(Long userId, String targetType, String targetId) {
        Long parsedTargetId = parseRequiredLong(targetId);
        boolean liked = likeService.lambdaQuery()
                .eq(CommunityLike::getUserId, userId)
                .eq(CommunityLike::getTargetType, targetType)
                .eq(CommunityLike::getTargetId, parsedTargetId)
                .count() > 0;
        long count = likeService.lambdaQuery()
                .eq(CommunityLike::getTargetType, targetType)
                .eq(CommunityLike::getTargetId, parsedTargetId)
                .count();
        return Map.of("liked", liked, "count", count);
    }

    @Override
    public Map<String, Object> toggleLike(Long userId, Map<String, Object> body) {
        String targetType = (String) body.get("targetType");
        Long targetId = parseRequiredLong(body.get("targetId"));

        CommunityLike existing = likeService.lambdaQuery()
                .eq(CommunityLike::getUserId, userId)
                .eq(CommunityLike::getTargetType, targetType)
                .eq(CommunityLike::getTargetId, targetId)
                .one();
        if (existing != null) {
            likeService.removeById(existing.getId());
            return Map.of("liked", false);
        }

        CommunityLike restored = findDeletedLike(userId, targetType, targetId);
        if (restored != null) {
            jdbcTemplate.update("UPDATE community_like SET deleted=0 WHERE id=?", restored.getId());
        } else {
            CommunityLike like = new CommunityLike();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            likeService.save(like);
        }
        return Map.of("liked", true);
    }

    @Override
    public PageVO<CommunityComment> listComments(PageDTO page, String targetType, String targetId) {
        var result = commentService.lambdaQuery()
                .eq(CommunityComment::getTargetType, targetType)
                .eq(CommunityComment::getTargetId, parseRequiredLong(targetId))
                .eq(CommunityComment::getParentId, 0L)
                .eq(CommunityComment::getIsDeletedByAdmin, 0)
                .orderByDesc(CommunityComment::getCreateTime)
                .page(new Page<>(page.getPage(), page.getSize()));
        return PageVO.of(page, result);
    }

    @Override
    public List<CommunityComment> listReplies(Long parentId) {
        return commentService.lambdaQuery()
                .eq(CommunityComment::getParentId, parentId)
                .eq(CommunityComment::getIsDeletedByAdmin, 0)
                .orderByAsc(CommunityComment::getCreateTime)
                .list();
    }

    @Override
    public CommunityComment postComment(Long userId, Map<String, Object> body) {
        CommunityComment comment = new CommunityComment();
        comment.setUserId(userId);
        comment.setTargetType((String) body.get("targetType"));
        comment.setTargetId(parseRequiredLong(body.get("targetId")));
        comment.setContent((String) body.get("content"));
        if (body.get("parentId") != null) {
            comment.setParentId(parseRequiredLong(body.get("parentId")));
        }
        if (body.get("replyToUserId") != null) {
            comment.setReplyToUserId(parseRequiredLong(body.get("replyToUserId")));
        }
        comment.setIsDeletedByAdmin(0);
        comment.setLikeCount(0);
        commentService.save(comment);
        return comment;
    }

    private Favorite findDeletedFavorite(Long userId, String targetType, Long targetId) {
        List<Favorite> deletedList = jdbcTemplate.query(
                "SELECT * FROM favorite WHERE user_id = ? AND target_type = ? AND target_id = ?",
                (rs, rowNum) -> {
                    Favorite favorite = new Favorite();
                    favorite.setId(rs.getLong("id"));
                    favorite.setUserId(rs.getLong("user_id"));
                    favorite.setTargetType(rs.getString("target_type"));
                    favorite.setTargetId(rs.getLong("target_id"));
                    favorite.setDeleted(rs.getInt("deleted"));
                    return favorite;
                }, userId, targetType, targetId);
        return deletedList.isEmpty() ? null : deletedList.get(0);
    }

    private CommunityLike findDeletedLike(Long userId, String targetType, Long targetId) {
        List<CommunityLike> deletedList = jdbcTemplate.query(
                "SELECT * FROM community_like WHERE user_id = ? AND target_type = ? AND target_id = ?",
                (rs, rowNum) -> {
                    CommunityLike like = new CommunityLike();
                    like.setId(rs.getLong("id"));
                    like.setUserId(rs.getLong("user_id"));
                    like.setTargetType(rs.getString("target_type"));
                    like.setTargetId(rs.getLong("target_id"));
                    like.setDeleted(rs.getInt("deleted"));
                    return like;
                }, userId, targetType, targetId);
        return deletedList.isEmpty() ? null : deletedList.get(0);
    }

    private Long parseOptionalLong(String value) {
        return value != null && !value.isEmpty() && !"null".equals(value) ? Long.valueOf(value) : null;
    }

    private Long parseRequiredLong(Object value) {
        return Long.valueOf(value.toString());
    }
}
