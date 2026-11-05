package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品总览（《接口文档》2.5.3）
 */
@Data
public class GoodsOverViewVO implements Serializable {

    //已上架数量（status = 1）
    private Integer sold;

    //已下架数量（status = 0）
    private Integer discontinued;
}
