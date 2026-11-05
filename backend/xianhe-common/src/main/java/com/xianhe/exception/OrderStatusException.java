package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 订单当前状态不允许该操作
 */
public class OrderStatusException extends BaseException {

    public OrderStatusException() {
        super(MessageConstant.ORDER_STATUS_ERROR);
    }
}
