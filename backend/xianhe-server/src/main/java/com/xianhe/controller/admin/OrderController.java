package com.xianhe.controller.admin;

import com.xianhe.dto.OrderCancelDTO;
import com.xianhe.dto.OrderConfirmDTO;
import com.xianhe.dto.OrderPageQueryDTO;
import com.xianhe.dto.OrderRejectionDTO;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.OrderService;
import com.xianhe.vo.OrderStatisticsVO;
import com.xianhe.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理端接口（《接口文档》2.8）
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端 · 订单")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单条件搜索
     * @param orderPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单条件搜索")
    public Result<PageResult> conditionSearch(OrderPageQueryDTO orderPageQueryDTO) {
        return Result.success(orderService.conditionSearch(orderPageQueryDTO));
    }

    /**
     * 各状态订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("各状态订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderService.statistics());
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        return Result.success(orderService.adminDetail(id));
    }

    /**
     * 接单
     * @param orderConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result<Void> confirm(@RequestBody OrderConfirmDTO orderConfirmDTO) {
        orderService.confirm(orderConfirmDTO.getId());
        return Result.success();
    }

    /**
     * 拒单
     * @param orderRejectionDTO
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result<Void> rejection(@RequestBody OrderRejectionDTO orderRejectionDTO) {
        orderService.reject(orderRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param orderCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<Void> cancel(@RequestBody OrderCancelDTO orderCancelDTO) {
        orderService.adminCancel(orderCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result<Void> delivery(@PathVariable Long id) {
        orderService.deliver(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result<Void> complete(@PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }
}
