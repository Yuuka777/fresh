package com.xianhe.mapper;

import com.xianhe.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细持久层
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入订单明细
     * <p>
     * 前置条件：{@code items} 非空 —— 明细来自购物车，购物车为空时下单会先被拦下。
     *
     * @param items 明细列表，不可为空
     */
    void insertBatch(@Param("items") List<OrderItem> items);

    /**
     * 按订单 id 查明细
     * @param orderId
     * @return
     */
    List<OrderItem> listByOrderId(Long orderId);

    /**
     * 按一批订单 id 查明细，供列表接口一次取回（不逐单查，避免 N+1）
     * <p>
     * 前置条件：{@code orderIds} 非空 —— 调用方在列表为空时提前返回。
     *
     * @param orderIds 订单 id 集合，不可为空
     * @return
     */
    List<OrderItem> listByOrderIds(@Param("orderIds") List<Long> orderIds);
}
