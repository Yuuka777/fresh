package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 分类被下级数据关联
 */
public class CategoryBeRelatedException extends BaseException {

    public CategoryBeRelatedException() {
        super(MessageConstant.CATEGORY_BE_RELATED);
    }
}
