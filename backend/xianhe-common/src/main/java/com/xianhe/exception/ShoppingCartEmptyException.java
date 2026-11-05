package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 购物车为空
 */
public class ShoppingCartEmptyException extends BaseException {

    public ShoppingCartEmptyException() {
        super(MessageConstant.SHOPPING_CART_IS_NULL);
    }
}
