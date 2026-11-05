package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 下架商品时该商品被起售中的组合箱引用
 */
public class GoodsDisableFailedException extends BaseException {

    public GoodsDisableFailedException() {
        super(MessageConstant.GOODS_DISABLE_FAILED);
    }
}
