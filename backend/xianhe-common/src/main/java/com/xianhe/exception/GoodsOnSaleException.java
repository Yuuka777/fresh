package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 商品处于上架状态
 */
public class GoodsOnSaleException extends BaseException {

    public GoodsOnSaleException() {
        super(MessageConstant.GOODS_ON_SALE);
    }
}
