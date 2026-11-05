package com.xianhe.controller.admin;

import com.xianhe.dto.GoodsDTO;
import com.xianhe.dto.GoodsPageQueryDTO;
import com.xianhe.entity.Goods;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.GoodsService;
import com.xianhe.vo.GoodsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 商品管理端接口（《接口文档》2.6）
 */
@RestController
@RequestMapping("/admin/goods")
@Api(tags = "管理端 · 商品")
@Slf4j
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 新增商品。规格随商品一并提交；新商品一律为「下架」状态
     * @param goodsDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增商品")
    public Result<Void> add(@RequestBody GoodsDTO goodsDTO) {
        goodsService.add(goodsDTO);
        return Result.success();
    }

    /**
     * 分页查询。名称模糊匹配，分类与状态为精确筛选，三者可任意组合
     * @param queryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("商品分页查询")
    public Result<PageResult> page(GoodsPageQueryDTO queryDTO) {
        return Result.success(goodsService.pageQuery(queryDTO));
    }

    /**
     * 批量删除，ids 为逗号分隔的字符串（契约 2.6.3）
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除商品")
    public Result<Void> delete(String ids) {
        goodsService.deleteBatch(parseIds(ids));
        return Result.success();
    }

    /**
     * 按 id 查询商品详情，含规格列表
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询商品")
    public Result<GoodsVO> getById(@PathVariable Long id) {
        return Result.success(goodsService.getById(id));
    }

    /**
     * 修改商品。规格按全量覆盖，状态不在本接口内变更
     * @param goodsDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改商品")
    public Result<Void> update(@RequestBody GoodsDTO goodsDTO) {
        goodsService.update(goodsDTO);
        return Result.success();
    }

    /**
     * 按分类查询商品列表，供「新增/修改组合箱」的商品选择弹层使用
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("按分类查询商品列表")
    public Result<List<Goods>> list(Long categoryId) {
        // 不过滤状态：已下架的商品也要能出现在弹层里并标注出来，
        // 否则编辑一个含下架商品的组合箱时，弹层里看不到它、一保存就被移除。
        return Result.success(goodsService.listByCategoryId(categoryId));
    }

    /**
     * 上架 / 下架
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("商品上架或下架")
    public Result<Void> setStatus(@PathVariable Integer status, Long id) {
        goodsService.setStatus(status, id);
        return Result.success();
    }

    /**
     * 把 "1,2,3" 解析成 id 集合
     * @param ids
     * @return
     */
    static List<Long> parseIds(String ids) {
        // 契约把批量删除的入参定义成逗号分隔字符串，这里统一做解析与容错：
        // 空白项与非法项丢弃，不做「解析失败即整单报错」的处理 —— 批量删除是幂等操作，
        // 少删一个无法解析的 id 比整单失败更容易让运营理解。
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
