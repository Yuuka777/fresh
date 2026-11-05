package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 内含商品份数不是正数
 */
public class BundleGoodsCopiesException extends BaseException {

    public BundleGoodsCopiesException() {
        super(MessageConstant.BUNDLE_GOODS_COPIES_INVALID);
    }
}
