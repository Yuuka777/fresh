package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 下单时购物车里存在已下架 / 已停售的项
 */
public class CartItemOffShelfException extends BaseException {

    public CartItemOffShelfException() {
        super(MessageConstant.CART_ITEM_OFF_SHELF);
    }
}
