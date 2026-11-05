package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 该会员没有默认地址（契约 8.7）
 */
public class AddressDefaultNotFoundException extends BaseException {

    public AddressDefaultNotFoundException() {
        super(MessageConstant.ADDRESS_DEFAULT_NOT_FOUND);
    }
}
