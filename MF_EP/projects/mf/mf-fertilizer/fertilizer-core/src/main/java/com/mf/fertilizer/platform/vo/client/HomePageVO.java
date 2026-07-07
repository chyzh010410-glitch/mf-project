package com.mf.fertilizer.platform.vo.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomePageVO implements Serializable {
    private List<BannerItem> banners;
    private List<ProductCard> recommendedProducts;
    private List<ProductCard> newProducts;
    private List<ArticleCard> recommendedArticles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerItem implements Serializable {
        private Long targetId;
        private String image;
        private String title;
        private String targetType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCard implements Serializable {
        private Long id;
        private String name;
        private String productType;
        private String coverImage;
        private BigDecimal price;
        private Integer salesCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleCard implements Serializable {
        private Long id;
        private String title;
        private String coverImage;
        private String summary;
    }
}
