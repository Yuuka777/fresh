package com.xianhe.service;

import com.xianhe.dto.OrderCancelDTO;
import com.xianhe.dto.OrderPageQueryDTO;
import com.xianhe.dto.OrderPaymentDTO;
import com.xianhe.dto.OrderRejectionDTO;
import com.xianhe.dto.OrderSubmitDTO;
import com.xianhe.result.PageResult;
import com.xianhe.vo.OrderStatisticsVO;
import com.xianhe.vo.OrderSubmitVO;
import com.xianhe.vo.OrderVO;

/**
 * 订单业务接口（《接口文档-用户端》9）
 */
public interface OrderService {

    /**
     * 用户下单
     * @param orderSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrderSubmitDTO orderSubmitDTO);

    /**
     * 订单支付：本项目不接真实微信支付，调用即推进状态
     * @param orderPaymentDTO
     */
    void payment(OrderPaymentDTO orderPaymentDTO);

    /**
     * 历史订单分页查询，只查当前登录会员的
     * @param orderPageQueryDTO
     * @return
     */
    PageResult pageHistory(OrderPageQueryDTO orderPageQueryDTO);

    /**
     * 订单详情，别人的订单按「订单不存在」处理
     * @param id
     * @return
     */
    OrderVO detail(Long id);

    /**
     * 用户取消订单，仅未进入履约的订单可取消
     * @param id
     */
    void cancel(Long id);

    /**
     * 再来一单，把该订单的明细写回购物车
     * @param id
     */
    void repeat(Long id);

    /**
     * 客户催单
     * @param id
     */
    void remind(Long id);

    /**
     * 管理端订单条件搜索，查全部会员的订单
     * @param orderPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrderPageQueryDTO orderPageQueryDTO);

    /**
     * 各状态订单数量统计，管理端页签角标用
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 管理端订单详情，只看订单是否存在
     * @param id
     * @return
     */
    OrderVO adminDetail(Long id);

    /**
     * 接单，仅待接单可接
     * @param id
     */
    void confirm(Long id);

    /**
     * 拒单，仅待接单可拒
     * @param orderRejectionDTO
     */
    void reject(OrderRejectionDTO orderRejectionDTO);

    /**
     * 管理端取消订单
     * @param orderCancelDTO
     */
    void adminCancel(OrderCancelDTO orderCancelDTO);

    /**
     * 派送订单，仅已接单可派送
     * @param id
     */
    void deliver(Long id);

    /**
     * 完成订单，仅派送中可完成
     * @param id
     */
    void complete(Long id);
}
