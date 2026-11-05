package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 接单入参（《接口文档》2.8.4）
 */
@Data
public class OrderConfirmDTO implements Serializable {

    //订单 id
    private Long id;
}
