package com.xianhe.service;

import com.xianhe.dto.CartDTO;
import com.xianhe.entity.OrderItem;
import com.xianhe.vo.CartVO;

import java.util.List;

/**
 * 购物车业务接口（《接口文档-用户端》7）
 */
public interface CartService {

    /**
     * 加一件：同款数量 +1，没有则新建一条
     * @param cartDTO
     */
    void add(CartDTO cartDTO);

    /**
     * 查当前会员的购物车，价格与状态按现值回查
     * @return
     */
    List<CartVO> list();

    /**
     * 清空当前会员的购物车
     */
    void clean();

    /**
     * 减一件：减到 0 时删除该行
     * @param cartDTO
     */
    void sub(CartDTO cartDTO);

    /**
     * 移除一行，用于清掉不想要的项（含已下架的商品）
     * @param id
     */
    void remove(Long id);

    /**
     * 把一批订单明细写回当前会员的购物车，同款累加（「再来一单」用）
     * @param items
     */
    void addFromOrderItems(List<OrderItem> items);
}
