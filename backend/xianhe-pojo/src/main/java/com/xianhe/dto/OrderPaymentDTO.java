package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单支付入参（《接口文档-用户端》9.2）
 */
@Data
public class OrderPaymentDTO implements Serializable {

    //订单号
    private String orderNumber;

    //支付方式，固定 1 微信
    private Integer payMethod;
}
