package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端取消订单入参（《接口文档》2.8.6）
 */
@Data
public class OrderCancelDTO implements Serializable {

    //订单 id
    private Long id;

    //取消原因
    private String cancelReason;
}
