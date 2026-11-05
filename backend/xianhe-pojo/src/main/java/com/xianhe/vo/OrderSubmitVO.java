package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单结果（《接口文档-用户端》9.1）
 */
@Data
public class OrderSubmitVO implements Serializable {

    //订单 id
    private Long id;

    //订单号
    private String orderNumber;

    //订单总金额（商品金额 + 运送费）
    private BigDecimal orderAmount;

    //下单时间
    private LocalDateTime orderTime;
}
