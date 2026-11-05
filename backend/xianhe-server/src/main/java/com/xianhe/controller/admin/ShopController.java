package com.xianhe.controller.admin;

import com.xianhe.result.Result;
import com.xianhe.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺管理端接口（《接口文档》2.4）
 */
@RestController
@RequestMapping("/admin/shop")
@Api(tags = "管理端 · 店铺")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 设置营业状态。打烊后用户端不能下单（由用户端下单接口校验）
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result<Void> setStatus(@PathVariable Integer status) {
        shopService.setStatus(status);
        return Result.success();
    }

    /**
     * 获取营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getStatus() {
        return Result.success(shopService.getStatus());
    }
}
