package com.xianhe.vo;

import com.xianhe.entity.OrderMain;
import lombok.Data;

import java.util.List;

/**
 * 订单（《接口文档》2.8.1，管理端与用户端共用）
 */
@Data
public class OrderVO extends OrderMain {

    //订单明细，按下单顺序
    private List<OrderItemVO> orderItems;
}
