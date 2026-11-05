package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 收货地址为空
 */
public class AddressEmptyException extends BaseException {

    public AddressEmptyException() {
        super(MessageConstant.ADDRESS_IS_NULL);
    }
}
