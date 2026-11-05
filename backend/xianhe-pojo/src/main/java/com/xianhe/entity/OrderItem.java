package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细（表 order_item）
 */
@Data
public class OrderItem implements Serializable {

    //主键
    private Long id;

    //名称（下单时的快照）
    private String name;

    //图片（快照），可为空
    private String image;

    //所属订单 id
    private Long orderId;

    //商品 id
    private Long goodsId;

    //组合箱 id
    private Long bundleId;

    //规格文本（快照）
    private String specText;

    //数量
    private Integer number;

    //小计 = 下单时的单价 × 数量
    private BigDecimal amount;
}
