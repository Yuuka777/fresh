package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 加购请求既没带商品也没带组合箱
 */
public class CartTargetRequiredException extends BaseException {

    public CartTargetRequiredException() {
        super(MessageConstant.CART_TARGET_REQUIRED);
    }
}
