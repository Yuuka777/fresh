package com.xianhe.controller.admin;

import com.xianhe.dto.GoodsCategoryDTO;
import com.xianhe.dto.GoodsCategoryPageQueryDTO;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.GoodsCategoryService;
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

import java.util.List;

/**
 * 商品分类管理端接口（《接口文档》2.2）
 */
@RestController
@RequestMapping("/admin/goodsCategory")
@Api(tags = "管理端 · 商品分类")
@Slf4j
public class GoodsCategoryController {

    @Autowired
    private GoodsCategoryService goodsCategoryService;

    /**
     * 新增分类。sort 为必填（库中该列 NOT NULL 且无默认值）
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<Void> add(@RequestBody GoodsCategoryDTO categoryDTO) {
        goodsCategoryService.add(categoryDTO);
        return Result.success();
    }

    /**
     * 分页查询分类
     * @param queryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(GoodsCategoryPageQueryDTO queryDTO) {
        // 不接收 status 参数：管理端必须能查到禁用中的分类，
        // 否则分类一旦被禁用就再也查不到、无法恢复。
        return Result.success(goodsCategoryService.pageQuery(queryDTO));
    }

    /**
     * 删除分类。分类下存在商品或组合箱时返回「分类下关联商品，无法删除」
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除分类")
    public Result<Void> delete(Long id) {
        goodsCategoryService.delete(id);
        return Result.success();
    }

    /**
     * 修改分类名称与排序值。不允许修改分类类型
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result<Void> update(@RequestBody GoodsCategoryDTO categoryDTO) {
        goodsCategoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用 / 禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用分类")
    public Result<Void> setStatus(@PathVariable Integer status, Long id) {
        goodsCategoryService.setStatus(id, status);
        return Result.success();
    }

    /**
     * 按类型查询全部分类，供商品 / 组合箱表单的分类下拉框使用
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("按类型查询分类列表")
    public Result<List<GoodsCategory>> list(Integer type) {
        // 按 sort 升序，不过滤 status 参数：分类被禁用后，编辑其下商品时下拉框仍要能回填出该分类，否则保存会丢掉分类。
        return Result.success(goodsCategoryService.listByType(type));
    }
}
