package com.xianhe.controller.user;

import com.xianhe.result.Result;
import com.xianhe.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺用户端接口（《接口文档-用户端》3）
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "用户端 · 店铺")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 查询营业状态，未登录也能查（打烊时小程序仍要能展示门店状态）
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("查询营业状态")
    public Result<Integer> getStatus() {
        return Result.success(shopService.getStatus());
    }
}
