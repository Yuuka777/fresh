package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合箱内含商品出参（《接口文档》2.7.4 的 bundleGoods 元素）
 */
@Data
public class BundleGoodsVO implements Serializable {

    //记录 id
    private Long id;

    //商品 id
    private Long goodsId;

    //商品名称（快照）
    private String name;

    //商品单价（快照）
    private BigDecimal price;

    //份数
    private Integer copies;
}
