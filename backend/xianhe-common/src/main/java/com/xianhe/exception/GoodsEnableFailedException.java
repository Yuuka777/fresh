package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 上架商品时所属分类处于禁用状态
 */
public class GoodsEnableFailedException extends BaseException {

    public GoodsEnableFailedException() {
        super(MessageConstant.GOODS_ENABLE_FAILED);
    }
}
