package com.mf.fertilizer.platform.service.impl;

import com.mf.fertilizer.content.entity.EncyclopediaArticle;
import com.mf.fertilizer.content.service.EncyclopediaArticleService;
import com.mf.fertilizer.platform.dto.client.FeedbackSubmitDTO;
import com.mf.fertilizer.platform.entity.ActivityEntity;
import com.mf.fertilizer.platform.entity.Faq;
import com.mf.fertilizer.platform.entity.Feedback;
import com.mf.fertilizer.platform.service.ActivityEntityService;
import com.mf.fertilizer.platform.service.FaqService;
import com.mf.fertilizer.platform.service.FeedbackService;
import com.mf.fertilizer.platform.service.PlatformConfigService;
import com.mf.fertilizer.platform.service.PlatformPortalApplicationService;
import com.mf.fertilizer.platform.vo.client.HomePageVO;
import com.mf.fertilizer.product.entity.Product;
import com.mf.fertilizer.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PlatformPortalApplicationServiceImpl implements PlatformPortalApplicationService {

    private final ActivityEntityService activityService;
    private final FaqService faqService;
    private final FeedbackService feedbackService;
    private final ProductService productService;
    private final EncyclopediaArticleService articleService;
    private final PlatformConfigService configService;

    @Override
    public List<ActivityEntity> listActiveActivities() {
        return activityService.lambdaQuery()
                .eq(ActivityEntity::getStatus, "active")
                .orderByDesc(ActivityEntity::getSortOrder)
                .list();
    }

    @Override
    public ActivityEntity getActivityDetail(Long id) {
        return activityService.getById(id);
    }

    @Override
    public List<Faq> listPublishedFaqs(String category) {
        return faqService.lambdaQuery()
                .eq(Faq::getIsPublished, 1)
                .eq(category != null, Faq::getCategory, category)
                .orderByAsc(Faq::getSortOrder)
                .list();
    }

    @Override
    public void submitFeedback(Long userId, FeedbackSubmitDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(dto.getType());
        feedback.setContent(dto.getContent());
        feedback.setContact(dto.getContact());
        feedback.setStatus("pending");
        feedbackService.save(feedback);
    }

    @Override
    public HomePageVO getHomePage() {
        HomePageVO vo = new HomePageVO();

        List<HomePageVO.BannerItem> banners = new ArrayList<>();
        if (configService.getBoolean("activity_banner_enabled", true)) {
            List<ActivityEntity> activities = activityService.lambdaQuery()
                    .eq(ActivityEntity::getStatus, "active")
                    .eq(ActivityEntity::getIsBanner, 1)
                    .orderByAsc(ActivityEntity::getSortOrder)
                    .list();
            for (ActivityEntity activity : activities) {
                banners.add(new HomePageVO.BannerItem(activity.getId(), activity.getCoverImage(), activity.getTitle(), "activity"));
            }
        }
        vo.setBanners(banners);

        int recommendProductLimit = configLimit("home_recommend_product_limit", 8);
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getStatus, 1)
                .gt(Product::getStock, 0)
                .eq(Product::getIsRecommend, 1)
                .orderByAsc(Product::getId)
                .list();
        vo.setRecommendedProducts(toProductCards(dailyRotate(products, recommendProductLimit)));

        int newProductLimit = configLimit("home_new_product_limit", 8);
        List<Product> newProducts = productService.lambdaQuery()
                .eq(Product::getStatus, 1)
                .gt(Product::getStock, 0)
                .orderByDesc(Product::getCreateTime)
                .last("limit " + newProductLimit)
                .list();
        vo.setNewProducts(toProductCards(newProducts));

        int recommendArticleLimit = configLimit("home_recommend_article_limit", 4);
        List<EncyclopediaArticle> articles = articleService.lambdaQuery()
                .eq(EncyclopediaArticle::getIsPublished, 1)
                .eq(EncyclopediaArticle::getIsRecommend, 1)
                .orderByDesc(EncyclopediaArticle::getCreateTime)
                .last("limit " + recommendArticleLimit)
                .list();
        List<HomePageVO.ArticleCard> articleCards = new ArrayList<>();
        for (EncyclopediaArticle article : articles) {
            articleCards.add(new HomePageVO.ArticleCard(article.getId(), article.getTitle(), article.getCoverImage(), article.getSummary()));
        }
        vo.setRecommendedArticles(articleCards);

        return vo;
    }

    private List<HomePageVO.ProductCard> toProductCards(List<Product> products) {
        List<HomePageVO.ProductCard> cards = new ArrayList<>();
        for (Product product : products) {
            cards.add(new HomePageVO.ProductCard(
                    product.getId(),
                    product.getName(),
                    product.getProductType(),
                    product.getCoverImage(),
                    product.getPrice(),
                    product.getSalesCount()
            ));
        }
        return cards;
    }

    private List<Product> dailyRotate(List<Product> products, int limit) {
        if (products.size() <= limit) {
            return products;
        }
        List<Product> rotated = new ArrayList<>(products);
        Collections.shuffle(rotated, new Random(LocalDate.now().toEpochDay()));
        return rotated.subList(0, limit);
    }

    private int configLimit(String key, int defaultValue) {
        int value = configService.getInt(key, defaultValue);
        if (value < 1) {
            return defaultValue;
        }
        return Math.min(value, 20);
    }
}
