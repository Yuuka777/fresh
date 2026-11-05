package com.xianhe.constant;

/**
 * Redis key 常量
 */
public class RedisKeyConstant {

    //门店营业状态（Redis 中存 0/1），见《接口文档》2.4
    public static final String SHOP_STATUS = "xh:shop:status";

    // 用户端「某分类下的商品列表」缓存前缀，完整 key 为 xh:goods:category:{分类id}，见《接口文档》2.6.7
    public static final String GOODS_BY_CATEGORY_PREFIX = "xh:goods:category:";

    //用户端组合箱列表缓存前缀，完整 key 为 xh:bundle:category:{分类id}（契约 2.7.6）
    //与商品同理按分类分片：一个组合箱只属于一个分类，管理端改状态时能精准清除对应的那一个键
    public static final String BUNDLE_BY_CATEGORY_PREFIX = "xh:bundle:category:";

    private RedisKeyConstant() {
    }
}
