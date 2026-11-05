package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 主订单（表 order_main）
 */
@Data
public class OrderMain implements Serializable {

    //主键
    private Long id;

    //订单号，全局唯一（库中 uk_order_number 唯一索引）
    private String number;

    //订单状态：1 待付款 / 2 待接单 / 3 已接单 / 4 派送中 / 5 已完成 / 6 已取消
    private Integer status;

    //下单会员 id
    private Long memberId;

    //收货地址 id
    private Long addressId;

    //下单时间
    private LocalDateTime orderTime;

    //支付时间，未支付为空
    private LocalDateTime checkoutTime;

    //支付方式：1 微信
    private Integer payMethod;

    //支付状态：0 未支付 / 1 已支付 / 2 已退款
    private Integer payStatus;

    //实收金额 = 商品金额 + 运送费
    private BigDecimal amount;

    //备注
    private String remark;

    //收货手机号（下单时的快照，地址后来改了也不影响历史订单）
    private String phone;

    //完整收货地址（快照）
    private String address;

    //会员昵称（快照）
    private String userName;

    //收货人（快照）
    private String consignee;

    //取消原因
    private String cancelReason;

    //拒单原因
    private String rejectionReason;

    //取消时间
    private LocalDateTime cancelTime;

    //预计送达时间
    private LocalDateTime estimatedDeliveryTime;

    //配送时效：1 尽快送达 / 0 预约时段
    private Integer deliveryStatus;

    //送达时间
    private LocalDateTime deliveryTime;

    //运送费
    private BigDecimal deliveryFee;
}
