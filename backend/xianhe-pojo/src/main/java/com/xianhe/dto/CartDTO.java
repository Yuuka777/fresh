package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 购物车增删入参
 */
@Data
public class CartDTO implements Serializable {

    //商品 id（与 bundleId 二选一）
    private Long goodsId;

    //组合箱 id（与 goodsId 二选一）
    private Long bundleId;

    //规格文本快照，如「净重:约500g」；无规格时不传
    private String specText;
}
