package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合箱内含商品入参（《接口文档》2.7.1 的 bundleGoods 元素）
 */
@Data
public class BundleGoodsDTO implements Serializable {

    //商品 id
    private Long goodsId;

    //商品名称（服务端按 goodsId 回查覆盖）
    private String name;

    //商品单价（服务端按 goodsId 回查覆盖）
    private BigDecimal price;

    //份数
    private Integer copies;
}
