package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 地址必填项校验未通过：收货人、手机号、详细地址三种原因共用一个异常类
 */
public class AddressRequiredFieldException extends BaseException {

    public AddressRequiredFieldException(String msg) {
        super(msg);
    }

    public static AddressRequiredFieldException consigneeRequired() {
        return new AddressRequiredFieldException(MessageConstant.ADDRESS_CONSIGNEE_REQUIRED);
    }

    public static AddressRequiredFieldException phoneInvalid() {
        return new AddressRequiredFieldException(MessageConstant.ADDRESS_PHONE_INVALID);
    }

    public static AddressRequiredFieldException detailRequired() {
        return new AddressRequiredFieldException(MessageConstant.ADDRESS_DETAIL_REQUIRED);
    }
}
