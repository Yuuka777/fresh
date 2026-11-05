package com.xianhe.service;

import com.xianhe.entity.BundleGoods;
import com.xianhe.vo.BundleVO;

import java.util.List;

/**
 * 用户端组合箱浏览业务接口（《接口文档-用户端》6）
 */
public interface UserBundleService {

    /**
     * 按分类查起售组合箱，走热点缓存
     * @param categoryId
     * @return
     */
    List<BundleVO> listByCategoryId(Long categoryId);

    /**
     * 查组合箱内的商品列表
     * @param bundleId
     * @return
     */
    List<BundleGoods> listGoodsByBundleId(Long bundleId);
}
