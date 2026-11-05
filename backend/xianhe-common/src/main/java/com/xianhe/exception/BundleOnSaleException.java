package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 组合箱处于上架状态
 */
public class BundleOnSaleException extends BaseException {

    public BundleOnSaleException() {
        super(MessageConstant.BUNDLE_ON_SALE);
    }
}
