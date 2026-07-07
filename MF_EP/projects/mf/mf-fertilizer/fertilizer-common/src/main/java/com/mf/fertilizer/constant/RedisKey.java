package com.mf.fertilizer.constant;

public interface RedisKey {

    String LOGIN_TOKEN = "login:token:";
    String CLIENT_TOKEN = "client:token:";
    String MERCHANT_TOKEN = "merchant:token:";
    String VERIFICATION_CODE = "verification:code:";
    String PRODUCT_STOCK = "product:stock:";
    String ORDER_TIMEOUT_TASK_LOCK = "order:timeout:task:lock";

    String TREE_LIST = "cache:tree:list";
    String FERTILIZER_LIST = "cache:fertilizer:list";
    String RECOMMEND = "cache:recommend:";
    String STATS = "cache:stats";

    /** 令牌过期时间(天) */
    int TOKEN_EXPIRE_DAYS = 7;

    /** 列表缓存过期时间(分钟) */
    int LIST_CACHE_MINUTES = 30;

    /** 推荐缓存过期时间(小时) */
    int RECOMMEND_CACHE_HOURS = 24;

    static String loginToken(String token) {
        return LOGIN_TOKEN + token;
    }

    static String clientToken(String token) {
        return CLIENT_TOKEN + token;
    }

    static String merchantToken(String token) {
        return MERCHANT_TOKEN + token;
    }

    static String verificationCode(String target, String type) {
        return VERIFICATION_CODE + target + ":" + type;
    }

    static String productStock(Long productId) {
        return PRODUCT_STOCK + productId;
    }
}
