package com.xianhe.task;

import com.xianhe.constant.OrderConstant;
import com.xianhe.constant.OrderStatusConstant;
import com.xianhe.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {

    //超时未支付的判定时长（分钟），与小程序支付页倒计时同一口径
    private static final int PAY_TIMEOUT_MINUTES = 15;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每分钟扫一次，把超过 15 分钟仍未支付的订单置为已取消（契约 2.8.9）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusMinutes(PAY_TIMEOUT_MINUTES);

        // 一条 SQL 完成，WHERE 里带 status = 1：用户此刻正在支付的订单状态已变成 2，不会被误取消。
        // 原来是「先查出全部、再逐条 update」，既多 N 次往返，也存在与支付并发覆盖状态的风险。
        int cancelled = orderMapper.cancelExpired(
                OrderStatusConstant.PENDING_PAYMENT,
                OrderStatusConstant.CANCELLED,
                deadline,
                OrderConstant.CANCEL_REASON_TIMEOUT,
                now);

        if (cancelled > 0) {
            log.info("超时未支付订单已自动取消 {} 单", cancelled);
        }
    }
}
