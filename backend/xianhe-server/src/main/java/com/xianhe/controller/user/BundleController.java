package com.xianhe.controller.user;

import com.xianhe.entity.BundleGoods;
import com.xianhe.result.Result;
import com.xianhe.service.UserBundleService;
import com.xianhe.vo.BundleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组合箱用户端接口（《接口文档-用户端》6）
 */
@RestController("userBundleController")
@RequestMapping("/user/bundle")
@Api(tags = "用户端 · 组合箱")
@Slf4j
public class BundleController {

    @Autowired
    private UserBundleService userBundleService;

    /**
     * 按分类查询起售组合箱，列表已带内含商品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("按分类查询组合箱")
    public Result<List<BundleVO>> list(Long categoryId) {
        return Result.success(userBundleService.listByCategoryId(categoryId));
    }

    /**
     * 查询组合箱内的商品列表（小程序组合箱详情弹层）
     * @param id
     * @return
     */
    @GetMapping("/goods/{id}")
    @ApiOperation("查询组合箱内商品列表")
    public Result<List<BundleGoods>> goods(@PathVariable Long id) {
        return Result.success(userBundleService.listGoodsByBundleId(id));
    }
}
