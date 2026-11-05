package com.xianhe.service;

/**
 * 店铺业务接口（《接口文档》2.4）
 */
public interface ShopService {

    /**
     * 设置营业状态
     * @param status
     */
    void setStatus(Integer status);

    /**
     * 读取营业状态
     * @return
     */
    Integer getStatus();
}
