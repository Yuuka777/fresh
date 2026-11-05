package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细（《接口文档》2.8.1 的 orderItems 元素）
 */
@Data
public class OrderItemVO implements Serializable {

    //名称快照
    private String name;

    //图片快照，可为 null
    private String image;

    //规格快照
    private String specText;

    //数量
    private Integer number;

    //小计
    private BigDecimal amount;
}
