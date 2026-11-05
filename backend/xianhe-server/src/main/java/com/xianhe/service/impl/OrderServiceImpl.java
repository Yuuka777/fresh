package com.xianhe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xianhe.constant.DeliveryConstant;
import com.xianhe.constant.OrderConstant;
import com.xianhe.constant.OrderStatusConstant;
import com.xianhe.constant.PayMethodConstant;
import com.xianhe.constant.PayStatusConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.dto.OrderCancelDTO;
import com.xianhe.dto.OrderPageQueryDTO;
import com.xianhe.dto.OrderPaymentDTO;
import com.xianhe.dto.OrderRejectionDTO;
import com.xianhe.dto.OrderSubmitDTO;
import com.xianhe.entity.Address;
import com.xianhe.entity.Member;
import com.xianhe.entity.OrderItem;
import com.xianhe.entity.OrderMain;
import com.xianhe.exception.AddressEmptyException;
import com.xianhe.exception.CartItemOffShelfException;
import com.xianhe.exception.OrderNotFoundException;
import com.xianhe.exception.OrderStatusException;
import com.xianhe.exception.ShoppingCartEmptyException;
import com.xianhe.mapper.AddressMapper;
import com.xianhe.mapper.CartMapper;
import com.xianhe.mapper.MemberMapper;
import com.xianhe.mapper.OrderItemMapper;
import com.xianhe.mapper.OrderMapper;
import com.xianhe.result.PageResult;
import com.xianhe.service.CartService;
import com.xianhe.service.OrderService;
import com.xianhe.utils.JsonUtil;
import com.xianhe.vo.CartVO;
import com.xianhe.vo.OrderItemVO;
import com.xianhe.vo.OrderStatisticsVO;
import com.xianhe.vo.OrderSubmitVO;
import com.xianhe.vo.OrderVO;
import com.xianhe.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单业务实现
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    //订单号前缀用的时间格式
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrderSubmitDTO orderSubmitDTO) {
        Long memberId = BaseContext.getCurrentId();

        // 收货地址必须是自己名下的：查不到、或别人的地址，都按「收货地址为空」处理
        Address address = requireOwnAddress(orderSubmitDTO.getAddressBookId(), memberId);

        // 明细直接取购物车，不采信请求体里提交的 orderItems / amount
        List<CartVO> lines = cartService.list();
        if (lines.isEmpty()) {
            throw new ShoppingCartEmptyException();
        }
        // 有下架 / 停售的项就整单拒绝：与购物车列表、前端「去结算」是同一口径
        if (lines.stream().anyMatch(line -> !StatusConstant.ENABLE.equals(line.getStatus()))) {
            log.warn("下单被拒：购物车里有已下架的项，memberId={}", memberId);
            throw new CartItemOffShelfException();
        }

        // 明细金额 = 现值单价 × 数量（CartVO 的 amount 已是这个口径）
        List<OrderItem> items = lines.stream().map(this::toOrderItem).collect(Collectors.toList());
        BigDecimal goodsAmount = items.stream()
                .map(OrderItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveryFee = DeliveryConstant.FEE;

        Member member = memberMapper.getById(memberId);

        OrderMain order = new OrderMain();
        order.setNumber(generateNumber());
        // 下单与支付分离：这里只建单，状态停在待付款（契约 9.1）
        order.setStatus(OrderStatusConstant.PENDING_PAYMENT);
        order.setMemberId(memberId);
        order.setAddressId(address.getId());
        order.setOrderTime(LocalDateTime.now());
        order.setPayMethod(payMethodOf(orderSubmitDTO.getPayMethod()));
        order.setPayStatus(PayStatusConstant.UN_PAID);
        order.setAmount(goodsAmount.add(deliveryFee));
        order.setRemark(orderSubmitDTO.getRemark());
        // 收货信息按地址回查后落快照：地址以后改了不影响历史订单
        order.setPhone(address.getPhone());
        order.setAddress(fullAddress(address));
        order.setConsignee(address.getConsignee());
        order.setUserName(member == null ? null : member.getName());
        order.setEstimatedDeliveryTime(orderSubmitDTO.getEstimatedDeliveryTime());
        order.setDeliveryStatus(deliveryStatusOf(orderSubmitDTO.getDeliveryStatus()));
        order.setDeliveryFee(deliveryFee);

        orderMapper.insert(order);

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.insertBatch(items);

        // 清空购物车与建单在同一事务：订单写不进去就不该把车清掉
        cartMapper.deleteByMemberId(memberId);

        log.info("下单成功：memberId={}，orderNumber={}，明细 {} 项，实收 {} 元",
                memberId, order.getNumber(), items.size(), order.getAmount());

        OrderSubmitVO vo = new OrderSubmitVO();
        vo.setId(order.getId());
        vo.setOrderNumber(order.getNumber());
        vo.setOrderAmount(order.getAmount());
        vo.setOrderTime(order.getOrderTime());
        return vo;
    }

    @Override
    @Transactional
    public void payment(OrderPaymentDTO orderPaymentDTO) {
        Long memberId = BaseContext.getCurrentId();

        OrderMain order = orderMapper.getByNumber(orderPaymentDTO.getOrderNumber());
        // 只能支付自己的订单；查不到与不是自己的统一按「订单不存在」，不给外人用订单号探测
        if (order == null || !order.getMemberId().equals(memberId)) {
            log.warn("支付被拒：订单不存在或不属于当前会员，memberId={}，orderNumber={}",
                    memberId, orderPaymentDTO.getOrderNumber());
            throw new OrderNotFoundException();
        }
        // 只有待付款能支付：重复点支付、或订单已被取消超时，都会走到这里
        if (!OrderStatusConstant.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new OrderStatusException();
        }

        OrderMain update = new OrderMain();
        update.setId(order.getId());
        update.setStatus(OrderStatusConstant.TO_BE_CONFIRMED);
        update.setPayStatus(PayStatusConstant.PAID);
        update.setCheckoutTime(LocalDateTime.now());
        update.setPayMethod(payMethodOf(orderPaymentDTO.getPayMethod()));
        // 条件更新：只有库里仍是待付款才落地。两个支付请求并发、或超时任务同时收口时，只有一个能成功
        if (orderMapper.updateByIdAndStatus(update, OrderStatusConstant.PENDING_PAYMENT) == 0) {
            log.warn("支付失败：订单状态已被并发修改，orderId={}", order.getId());
            throw new OrderStatusException();
        }

        // 契约第 10 节：来单提醒只在支付成功这一刻推给管理端（提交订单、取消都不推）
        notifyAdminAfterCommit(1, order);

        log.info("支付成功：memberId={}，orderNumber={}，订单转入待接单", memberId, order.getNumber());
    }

    @Override
    public PageResult pageHistory(OrderPageQueryDTO orderPageQueryDTO) {
        // 只查自己的订单：会员 id 由令牌决定，不接受前端传入（契约 9.3）
        orderPageQueryDTO.setMemberId(BaseContext.getCurrentId());

        PageHelper.startPage(orderPageQueryDTO.getPage(), orderPageQueryDTO.getPageSize());
        List<OrderMain> list = orderMapper.pageQuery(orderPageQueryDTO);
        Page<OrderMain> page = (Page<OrderMain>) list;

        return new PageResult(page.getTotal(), toVoList(page.getResult()));
    }

    @Override
    public OrderVO detail(Long id) {
        OrderMain order = requireOwnOrder(id);

        OrderVO vo = toVo(order);
        vo.setOrderItems(toItemVoList(orderItemMapper.listByOrderId(id)));
        return vo;
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        Long memberId = BaseContext.getCurrentId();
        OrderMain order = requireOwnOrder(id);

        // 契约 9.5：只有还没进入履约的订单能取消（待付款、待接单）；已接单及之后要联系门店
        if (!OrderStatusConstant.PENDING_PAYMENT.equals(order.getStatus())
                && !OrderStatusConstant.TO_BE_CONFIRMED.equals(order.getStatus())) {
            log.warn("取消订单被拒：订单已进入履约，memberId={}，orderId={}，status={}",
                    memberId, id, order.getStatus());
            throw new OrderStatusException();
        }

        OrderMain update = new OrderMain();
        update.setId(id);
        update.setStatus(OrderStatusConstant.CANCELLED);
        update.setCancelReason(OrderConstant.CANCEL_REASON_USER);
        update.setCancelTime(LocalDateTime.now());
        // 已支付的（待接单）要退钱：本项目未接真实支付，退款用支付状态表达
        if (PayStatusConstant.PAID.equals(order.getPayStatus())) {
            update.setPayStatus(PayStatusConstant.REFUNDED);
        }
        orderMapper.update(update);

        log.info("用户取消订单：memberId={}，orderId={}，原状态={}，支付状态={}",
                memberId, id, order.getStatus(), order.getPayStatus());
    }

    @Override
    @Transactional
    public void repeat(Long id) {
        Long memberId = BaseContext.getCurrentId();
        requireOwnOrder(id);

        List<OrderItem> items = orderItemMapper.listByOrderId(id);
        if (items.isEmpty()) {
            // 正常订单至少有 1 条明细，走到这里说明数据被手改过
            log.warn("再来一单失败：订单没有明细，orderId={}", id);
            throw new OrderNotFoundException();
        }

        cartService.addFromOrderItems(items);

        log.info("再来一单：memberId={}，orderId={}，明细 {} 项写回购物车", memberId, id, items.size());
    }

    @Override
    public void remind(Long id) {
        Long memberId = BaseContext.getCurrentId();
        OrderMain order = requireOwnOrder(id);

        // 可催的三个状态：待接单（催接单）、已接单（催出餐）、派送中（催配送），与前端 canRemindOrder 一致。
        // 待付款还没有履约对象，已完成 / 已取消没必要催，都不放行。
        Integer status = order.getStatus();
        if (!OrderStatusConstant.TO_BE_CONFIRMED.equals(status)
                && !OrderStatusConstant.CONFIRMED.equals(status)
                && !OrderStatusConstant.DELIVERY_IN_PROGRESS.equals(status)) {
            log.warn("催单被拒：订单不在可催状态，memberId={}，orderId={}，status={}",
                    memberId, id, status);
            throw new OrderStatusException();
        }

        // 契约第 10 节：催单提醒推给管理端（type = 2）
        notifyAdminAfterCommit(2, order);

        log.info("用户催单：memberId={}，orderNumber={}，status={}", memberId, order.getNumber(), order.getStatus());
    }

    @Override
    public PageResult conditionSearch(OrderPageQueryDTO orderPageQueryDTO) {
        // 管理端不设会员条件，查的是全部会员的订单（契约 2.8.1）
        PageHelper.startPage(orderPageQueryDTO.getPage(), orderPageQueryDTO.getPageSize());
        List<OrderMain> list = orderMapper.pageQuery(orderPageQueryDTO);
        Page<OrderMain> page = (Page<OrderMain>) list;

        return new PageResult(page.getTotal(), toVoList(page.getResult()));
    }

    @Override
    public OrderStatisticsVO statistics() {
        // 一次 GROUP BY 拿全部数字：省 3 次往返，且四个数来自同一快照（不会出现一角标新、一角标旧）
        Map<Integer, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : orderMapper.countGroupByStatus(List.of(
                OrderStatusConstant.PENDING_PAYMENT,
                OrderStatusConstant.TO_BE_CONFIRMED,
                OrderStatusConstant.CONFIRMED,
                OrderStatusConstant.DELIVERY_IN_PROGRESS))) {
            counts.put(((Number) row.get("status")).intValue(), ((Number) row.get("cnt")).intValue());
        }

        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBePaid(counts.getOrDefault(OrderStatusConstant.PENDING_PAYMENT, 0));
        vo.setToBeConfirmed(counts.getOrDefault(OrderStatusConstant.TO_BE_CONFIRMED, 0));
        vo.setConfirmed(counts.getOrDefault(OrderStatusConstant.CONFIRMED, 0));
        vo.setDeliveryInProgress(counts.getOrDefault(OrderStatusConstant.DELIVERY_IN_PROGRESS, 0));
        return vo;
    }

    @Override
    public OrderVO adminDetail(Long id) {
        OrderMain order = requireOrder(id);

        OrderVO vo = toVo(order);
        vo.setOrderItems(toItemVoList(orderItemMapper.listByOrderId(id)));
        return vo;
    }

    @Override
    @Transactional
    public void confirm(Long id) {
        OrderMain order = requireOrder(id);

        // 契约 2.8.4：只有待接单能接
        if (!OrderStatusConstant.TO_BE_CONFIRMED.equals(order.getStatus())) {
            log.warn("接单被拒：订单不是待接单，orderId={}，status={}", id, order.getStatus());
            throw new OrderStatusException();
        }

        if (updateStatus(id, OrderStatusConstant.TO_BE_CONFIRMED, OrderStatusConstant.CONFIRMED) == 0) {
            log.warn("接单失败：订单状态已被并发修改，orderId={}", id);
            throw new OrderStatusException();
        }
        log.info("接单成功：orderNumber={}", order.getNumber());
    }

    @Override
    @Transactional
    public void reject(OrderRejectionDTO orderRejectionDTO) {
        OrderMain order = requireOrder(orderRejectionDTO.getId());

        // 契约 2.8.5：只有待接单能拒
        if (!OrderStatusConstant.TO_BE_CONFIRMED.equals(order.getStatus())) {
            log.warn("拒单被拒：订单不是待接单，orderId={}，status={}",
                    order.getId(), order.getStatus());
            throw new OrderStatusException();
        }

        OrderMain update = new OrderMain();
        update.setId(order.getId());
        update.setStatus(OrderStatusConstant.CANCELLED);
        update.setRejectionReason(orderRejectionDTO.getRejectionReason());
        update.setCancelTime(LocalDateTime.now());
        // 已支付的订单要退钱：本项目未接真实支付，退款用支付状态表达
        if (PayStatusConstant.PAID.equals(order.getPayStatus())) {
            update.setPayStatus(PayStatusConstant.REFUNDED);
        }
        if (orderMapper.updateByIdAndStatus(update, OrderStatusConstant.TO_BE_CONFIRMED) == 0) {
            log.warn("拒单失败：订单状态已被并发修改，orderId={}", order.getId());
            throw new OrderStatusException();
        }

        log.info("拒单成功：orderNumber={}，原因={}",
                order.getNumber(), orderRejectionDTO.getRejectionReason());
    }

    @Override
    @Transactional
    public void adminCancel(OrderCancelDTO orderCancelDTO) {
        OrderMain order = requireOrder(orderCancelDTO.getId());

        // 契约 2.8.6：已完成与已取消的订单不能再取消
        if (OrderStatusConstant.COMPLETED.equals(order.getStatus())
                || OrderStatusConstant.CANCELLED.equals(order.getStatus())) {
            log.warn("取消订单被拒：订单已完成或已取消，orderId={}，status={}",
                    order.getId(), order.getStatus());
            throw new OrderStatusException();
        }

        OrderMain update = new OrderMain();
        update.setId(order.getId());
        update.setStatus(OrderStatusConstant.CANCELLED);
        update.setCancelReason(orderCancelDTO.getCancelReason());
        update.setCancelTime(LocalDateTime.now());
        if (PayStatusConstant.PAID.equals(order.getPayStatus())) {
            update.setPayStatus(PayStatusConstant.REFUNDED);
        }
        if (orderMapper.updateByIdAndStatus(update, order.getStatus()) == 0) {
            log.warn("取消失败：订单状态已被并发修改，orderId={}", order.getId());
            throw new OrderStatusException();
        }

        log.info("管理端取消订单：orderNumber={}，原因={}",
                order.getNumber(), orderCancelDTO.getCancelReason());
    }

    @Override
    @Transactional
    public void deliver(Long id) {
        OrderMain order = requireOrder(id);

        // 契约 2.8.7：只有已接单能派送
        if (!OrderStatusConstant.CONFIRMED.equals(order.getStatus())) {
            log.warn("派送被拒：订单不是已接单，orderId={}，status={}", id, order.getStatus());
            throw new OrderStatusException();
        }

        if (updateStatus(id, OrderStatusConstant.CONFIRMED, OrderStatusConstant.DELIVERY_IN_PROGRESS) == 0) {
            log.warn("派送失败：订单状态已被并发修改，orderId={}", id);
            throw new OrderStatusException();
        }
        log.info("订单开始派送：orderNumber={}", order.getNumber());
    }

    @Override
    @Transactional
    public void complete(Long id) {
        OrderMain order = requireOrder(id);

        // 契约 2.8.8：只有派送中能完成
        if (!OrderStatusConstant.DELIVERY_IN_PROGRESS.equals(order.getStatus())) {
            log.warn("完成被拒：订单不是派送中，orderId={}，status={}", id, order.getStatus());
            throw new OrderStatusException();
        }

        OrderMain update = new OrderMain();
        update.setId(id);
        update.setStatus(OrderStatusConstant.COMPLETED);
        update.setDeliveryTime(LocalDateTime.now());
        if (orderMapper.updateByIdAndStatus(update, OrderStatusConstant.DELIVERY_IN_PROGRESS) == 0) {
            log.warn("完成失败：订单状态已被并发修改，orderId={}", id);
            throw new OrderStatusException();
        }

        log.info("订单已完成：orderNumber={}", order.getNumber());
    }

    // 收货地址归属校验：别人的地址一律按「收货地址为空」，不给外人用 id 探测
    private Address requireOwnAddress(Long addressId, Long memberId) {
        Address address = addressId == null ? null : addressMapper.getById(addressId);
        if (address == null || !address.getMemberId().equals(memberId)) {
            log.warn("下单被拒：收货地址不存在或不属于当前会员，memberId={}，addressId={}", memberId, addressId);
            throw new AddressEmptyException();
        }
        return address;
    }

    // 购物车行 → 订单明细：name / image / specText / number / amount 同名，直接拷
    private OrderItem toOrderItem(CartVO line) {
        OrderItem item = new OrderItem();
        BeanUtils.copyProperties(line, item);
        return item;
    }

    // 订单号：时间前缀便于排查，后 4 位随机避免同一秒并发下单撞上唯一索引
    private String generateNumber() {
        return NUMBER_TIME.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    // 地址快照拼成一行完整地址
    private String fullAddress(Address address) {
        return text(address.getProvinceName())
                + text(address.getCityName())
                + text(address.getDistrictName())
                + text(address.getDetail());
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private Integer payMethodOf(Integer payMethod) {
        return payMethod == null ? PayMethodConstant.WECHAT : payMethod;
    }

    // 不传时按「尽快送达」处理
    private Integer deliveryStatusOf(Integer deliveryStatus) {
        return deliveryStatus == null ? DeliveryConstant.STATUS_ASAP : deliveryStatus;
    }

    // 用户端的订单归属校验：查不到与不是自己的统一按「订单不存在」，不给外人用 id 探测
    private OrderMain requireOwnOrder(Long id) {
        Long memberId = BaseContext.getCurrentId();
        OrderMain order = id == null ? null : orderMapper.getById(id);
        if (order == null || !order.getMemberId().equals(memberId)) {
            log.warn("订单操作被拒：订单不存在或不属于当前会员，memberId={}，orderId={}", memberId, id);
            throw new OrderNotFoundException();
        }
        return order;
    }

    // 订单 → VO（不含明细）
    private OrderVO toVo(OrderMain order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    // 订单列表 → 带明细的列表：明细一次批量查回再按订单分组，不逐单查
    private List<OrderVO> toVoList(List<OrderMain> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(OrderMain::getId).collect(Collectors.toList());
        Map<Long, List<OrderItemVO>> grouped = orderItemMapper.listByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId,
                        Collectors.mapping(this::toItemVo, Collectors.toList())));

        return orders.stream().map(order -> {
            OrderVO vo = toVo(order);
            vo.setOrderItems(grouped.getOrDefault(order.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }

    private OrderItemVO toItemVo(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private List<OrderItemVO> toItemVoList(List<OrderItem> items) {
        return items.stream().map(this::toItemVo).collect(Collectors.toList());
    }

    // 管理端的订单校验：只看订单是否存在 —— 店员处理的是全部会员的订单，没有归属一说
    private OrderMain requireOrder(Long id) {
        OrderMain order = id == null ? null : orderMapper.getById(id);
        if (order == null) {
            log.warn("订单操作被拒：订单不存在，orderId={}", id);
            throw new OrderNotFoundException();
        }
        return order;
    }

    // 只改状态；带原状态条件，返回影响行数（0 = 状态已被别人改掉，本次不生效）
    private int updateStatus(Long id, Integer fromStatus, Integer toStatus) {
        OrderMain update = new OrderMain();
        update.setId(id);
        update.setStatus(toStatus);
        return orderMapper.updateByIdAndStatus(update, fromStatus);
    }

    // 向管理端推送提醒（契约第 10 节的消息体）：1 来单、2 催单
    /**
     * 事务提交之后再推送消息
     * @param type
     * @param order
     */
    private void notifyAdminAfterCommit(Integer type, OrderMain order) {
        // 管理端收到提醒会立刻拉一次订单统计，若在事务提交前推送，它读到的还是旧状态
        // （表现为「来了新订单，但待接单页签的角标不动」）。有事务就挂到 afterCommit，没有就直接推。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifyAdmin(type, order);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyAdmin(type, order);
            }
        });
    }

    private void notifyAdmin(Integer type, OrderMain order) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("orderId", order.getId());
        message.put("content", "订单号：" + order.getNumber());

        webSocketServer.sendToAllClient(JsonUtil.toJson(message));
    }
}
