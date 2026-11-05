package com.xianhe.service;

import com.xianhe.vo.GoodsVO;

import java.util.List;

/**
 * 用户端商品浏览业务接口（《接口文档-用户端》5）
 */
public interface UserGoodsService {

    /**
     * 按分类查上架商品，走热点缓存
     * @param categoryId
     * @return
     */
    List<GoodsVO> listByCategoryId(Long categoryId);

    /**
     * 按关键词搜上架商品，与分类无关，不走缓存
     * @param name
     * @return
     */
    List<GoodsVO> searchByName(String name);
}
