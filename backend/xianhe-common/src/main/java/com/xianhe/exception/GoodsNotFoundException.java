package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 按 id 查询商品时查不到记录
 */
public class GoodsNotFoundException extends BaseException {

    public GoodsNotFoundException() {
        super(MessageConstant.GOODS_NOT_FOUND);
    }
}
