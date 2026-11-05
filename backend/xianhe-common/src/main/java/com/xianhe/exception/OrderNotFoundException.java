package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 按 id 查不到订单
 */
public class OrderNotFoundException extends BaseException {

    public OrderNotFoundException() {
        super(MessageConstant.ORDER_NOT_FOUND);
    }
}
