package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 减少购物车数量时该记录已不在购物车中
 */
public class CartEmptyException extends BaseException {

    public CartEmptyException() {
        super(MessageConstant.CART_EMPTY);
    }
}
