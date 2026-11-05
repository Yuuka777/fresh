package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车
 */
@Data
public class Cart implements Serializable {

    //主键
    private Long id;

    //商品 / 组合箱名称（快照）
    private String name;

    //图片（快照）
    private String image;

    //会员 id，取自登录上下文，前端不传
    private Long memberId;

    //商品 id（与 bundleId 二选一）
    private Long goodsId;

    //组合箱 id（与 goodsId 二选一）
    private Long bundleId;

    //规格文本快照，如「净重:约500g」
    private String specText;

    //数量，由服务端 +1 / -1，请求体不带
    private Integer number;

    //小计金额 = 单价 × 数量（不是单价），下单时直接累加各行
    private BigDecimal amount;

    //加入时间
    private LocalDateTime createTime;
}
