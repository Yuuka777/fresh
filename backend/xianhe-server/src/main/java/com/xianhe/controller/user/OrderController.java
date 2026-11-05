package com.xianhe.controller.user;

import com.xianhe.dto.OrderPageQueryDTO;
import com.xianhe.dto.OrderPaymentDTO;
import com.xianhe.dto.OrderSubmitDTO;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.OrderService;
import com.xianhe.vo.OrderSubmitVO;
import com.xianhe.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单用户端接口（《接口文档-用户端》9）
 */
@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端 · 订单")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单：只创建订单，不产生支付
     * @param orderSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        return Result.success(orderService.submit(orderSubmitDTO));
    }

    /**
     * 订单支付：点击即成功（未接入真实微信支付）
     * @param orderPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<Void> payment(@RequestBody OrderPaymentDTO orderPaymentDTO) {
        orderService.payment(orderPaymentDTO);
        return Result.success();
    }

    /**
     * 历史订单分页查询，只返回当前登录会员的
     * @param orderPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单分页查询")
    public Result<PageResult> historyOrders(OrderPageQueryDTO orderPageQueryDTO) {
        return Result.success(orderService.pageHistory(orderPageQueryDTO));
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /**
     * 用户取消订单：仅待付款与待接单可取消
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 再来一单：把该订单的明细写回购物车
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result<Void> repetition(@PathVariable Long id) {
        orderService.repeat(id);
        return Result.success();
    }

    /**
     * 客户催单：仅已接单与派送中可催
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result<Void> reminder(@PathVariable Long id) {
        orderService.remind(id);
        return Result.success();
    }
}
