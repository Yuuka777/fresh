package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品规格（表 goods_spec）
 */
@Data
public class GoodsSpec implements Serializable {

    //主键
    private Long id;

    //所属商品 id
    private Long goodsId;

    //规格类型名称，如「净重」
    private String name;

    //候选值数组的 JSON 文本，如 {@code ["约500g","约1kg"]}
    private String value;
}
