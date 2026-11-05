package com.xianhe.controller.user;

import com.xianhe.result.Result;
import com.xianhe.service.UserGoodsService;
import com.xianhe.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品用户端接口（《接口文档-用户端》5）
 */
@RestController("userGoodsController")
@RequestMapping("/user/goods")
@Api(tags = "用户端 · 商品")
@Slf4j
public class GoodsController {

    @Autowired
    private UserGoodsService userGoodsService;

    /**
     * 按分类查询上架商品（带规格，前端据此决定是否弹规格选择）
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("按分类查询商品")
    public Result<List<GoodsVO>> list(Long categoryId) {
        return Result.success(userGoodsService.listByCategoryId(categoryId));
    }

    /**
     * 按关键词搜索上架商品，与分类无关
     * @param name
     * @return
     */
    @GetMapping("/search")
    @ApiOperation("按关键词搜索商品")
    public Result<List<GoodsVO>> search(String name) {
        return Result.success(userGoodsService.searchByName(name));
    }
}
