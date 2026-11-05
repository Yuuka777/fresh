package com.xianhe.controller.user;

import com.xianhe.entity.GoodsCategory;
import com.xianhe.result.Result;
import com.xianhe.service.GoodsCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类用户端接口（《接口文档-用户端》4）
 */
@RestController("userGoodsCategoryController")
@RequestMapping("/user/goodsCategory")
@Api(tags = "用户端 · 商品分类")
@Slf4j
public class GoodsCategoryController {

    @Autowired
    private GoodsCategoryService goodsCategoryService;

    /**
     * 查询分类列表，只返回启用中的分类（小程序首页左侧分类栏）
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类列表")
    public Result<List<GoodsCategory>> list(Integer type) {
        return Result.success(goodsCategoryService.listEnabledByType(type));
    }
}
