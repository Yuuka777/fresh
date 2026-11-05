package com.xianhe.controller.admin;

import com.xianhe.result.Result;
import com.xianhe.service.WorkspaceService;
import com.xianhe.vo.BundleOverViewVO;
import com.xianhe.vo.BusinessDataVO;
import com.xianhe.vo.GoodsOverViewVO;
import com.xianhe.vo.OrderOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 工作台管理端接口（《接口文档》2.5）
 */
@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "管理端 · 工作台")
@Slf4j
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 今日运营数据。时间区间在接口层取「今天」，服务层只认区间，便于以后按别的区间复用
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("今日运营数据")
    public Result<BusinessDataVO> businessData() {
        return Result.success(workspaceService.getBusinessData(
                LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX)));
    }

    /**
     * 订单总览
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("订单总览")
    public Result<OrderOverViewVO> overviewOrders() {
        return Result.success(workspaceService.getOrderOverView());
    }

    /**
     * 商品总览
     * @return
     */
    @GetMapping("/overviewGoods")
    @ApiOperation("商品总览")
    public Result<GoodsOverViewVO> overviewGoods() {
        return Result.success(workspaceService.getGoodsOverView());
    }

    /**
     * 组合箱总览
     * @return
     */
    @GetMapping("/overviewBundles")
    @ApiOperation("组合箱总览")
    public Result<BundleOverViewVO> overviewBundles() {
        return Result.success(workspaceService.getBundleOverView());
    }
}
