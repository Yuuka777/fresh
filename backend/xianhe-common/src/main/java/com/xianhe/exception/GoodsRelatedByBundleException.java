package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 商品仍被组合箱引用
 */
public class GoodsRelatedByBundleException extends BaseException {

    public GoodsRelatedByBundleException() {
        super(MessageConstant.GOODS_BE_RELATED_BY_BUNDLE);
    }
}
