package com.xianhe.controller.user;

import com.xianhe.dto.CartDTO;
import com.xianhe.result.Result;
import com.xianhe.service.CartService;
import com.xianhe.vo.CartVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车用户端接口（《接口文档-用户端》7）
 */
@RestController("userCartController")
@RequestMapping("/user/cart")
@Api(tags = "用户端 · 购物车")
@Slf4j
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车：请求体不带数量，服务端把同款的数量 +1
     * @param cartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result<Void> add(@RequestBody CartDTO cartDTO) {
        cartService.add(cartDTO);
        return Result.success();
    }

    /**
     * 查看购物车：会员 id 取自令牌，价格与上下架状态按现值回查
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<CartVO>> list() {
        return Result.success(cartService.list());
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result<Void> clean() {
        cartService.clean();
        return Result.success();
    }

    /**
     * 减少一件：减到 0 时删除该行
     * @param cartDTO
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("减少一件商品")
    public Result<Void> sub(@RequestBody CartDTO cartDTO) {
        cartService.sub(cartDTO);
        return Result.success();
    }

    /**
     * 移除一行：清掉不想要的项（含已下架的商品）
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("移除购物车中的一行")
    public Result<Void> remove(@RequestParam Long id) {
        cartService.remove(id);
        return Result.success();
    }
}
