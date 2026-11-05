package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 拒单入参（《接口文档》2.8.5）
 */
@Data
public class OrderRejectionDTO implements Serializable {

    //订单 id
    private Long id;

    //拒单原因
    private String rejectionReason;
}
