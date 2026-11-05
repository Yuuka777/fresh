package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 地址不存在：按 id 查不到，或该地址不属于当前会员
 */
public class AddressNotFoundException extends BaseException {

    public AddressNotFoundException() {
        super(MessageConstant.ADDRESS_NOT_FOUND);
    }
}
