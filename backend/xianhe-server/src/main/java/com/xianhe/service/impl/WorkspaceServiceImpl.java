package com.xianhe.service.impl;

import com.xianhe.constant.OrderStatusConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.mapper.BundleMapper;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.mapper.MemberMapper;
import com.xianhe.mapper.OrderMapper;
import com.xianhe.service.WorkspaceService;
import com.xianhe.vo.BundleOverViewVO;
import com.xianhe.vo.BusinessDataVO;
import com.xianhe.vo.GoodsOverViewVO;
import com.xianhe.vo.OrderOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作台服务实现
 */
@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 按时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        //总订单数是完成率的分母，只按时间区间统计、不带状态
        Integer totalOrderCount = orderMapper.countByCondition(begin, end, null);
        //营业额与有效订单数都只算「已完成」：被取消、退了款的单不是收入
        BigDecimal turnover = orderMapper.sumAmountByCondition(begin, end, OrderStatusConstant.COMPLETED);
        Integer validOrderCount = orderMapper.countByCondition(begin, end, OrderStatusConstant.COMPLETED);
        Integer newMembers = memberMapper.countByCreateTimeBetween(begin, end);

        //完成率与客单价都以有效订单数为分母；没有有效订单时保持 0，避免除零后得到 null
        double orderCompletionRate = 0.0;
        double unitPrice = 0.0;
        if (validOrderCount != 0) {
            orderCompletionRate = BigDecimal.valueOf(validOrderCount)
                    .divide(BigDecimal.valueOf(totalOrderCount), 4, RoundingMode.HALF_UP)
                    .doubleValue();
            //客单价是金额，保留 2 位，避免 JSON 里出现 89.88000000000001 这种尾巴
            unitPrice = turnover.divide(BigDecimal.valueOf(validOrderCount), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BusinessDataVO vo = new BusinessDataVO();
        vo.setTurnover(turnover.doubleValue());
        vo.setValidOrderCount(validOrderCount);
        vo.setOrderCompletionRate(orderCompletionRate);
        vo.setUnitPrice(unitPrice);
        vo.setNewMembers(newMembers);
        return vo;
    }

    /**
     * 订单总览
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        //今天的区间只用于「已完成 / 已取消 / 全部」这三项：它们答的是「今天做了多少」
        LocalDateTime todayBegin = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        OrderOverViewVO vo = new OrderOverViewVO();
        //待接单、待派送是「手上还有多少活」，不限时间：否则昨天接单还没派的单会从工作台上消失，
        //数字也会与订单页那两个页签对不上
        vo.setWaitingOrders(orderMapper.countByCondition(null, null, OrderStatusConstant.TO_BE_CONFIRMED));
        vo.setDeliveredOrders(orderMapper.countByCondition(null, null, OrderStatusConstant.CONFIRMED));
        vo.setCompletedOrders(orderMapper.countByCondition(todayBegin, todayEnd, OrderStatusConstant.COMPLETED));
        vo.setCancelledOrders(orderMapper.countByCondition(todayBegin, todayEnd, OrderStatusConstant.CANCELLED));
        vo.setAllOrders(orderMapper.countByCondition(todayBegin, todayEnd, null));
        return vo;
    }

    /**
     * 商品总览
     * @return
     */
    @Override
    public GoodsOverViewVO getGoodsOverView() {
        GoodsOverViewVO vo = new GoodsOverViewVO();
        vo.setSold(goodsMapper.countByStatus(StatusConstant.ENABLE));
        vo.setDiscontinued(goodsMapper.countByStatus(StatusConstant.DISABLE));
        return vo;
    }

    /**
     * 组合箱总览
     * @return
     */
    @Override
    public BundleOverViewVO getBundleOverView() {
        BundleOverViewVO vo = new BundleOverViewVO();
        vo.setSold(bundleMapper.countByStatus(StatusConstant.ENABLE));
        vo.setDiscontinued(bundleMapper.countByStatus(StatusConstant.DISABLE));
        return vo;
    }
}
