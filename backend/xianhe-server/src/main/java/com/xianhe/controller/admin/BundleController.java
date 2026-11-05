package com.xianhe.controller.admin;

import com.xianhe.dto.BundleDTO;
import com.xianhe.dto.BundlePageQueryDTO;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.BundleService;
import com.xianhe.vo.BundleVO;
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

/**
 * 组合箱管理端接口（《接口文档》2.7）
 */
@RestController
@RequestMapping("/admin/bundle")
@Api(tags = "管理端 · 组合箱")
@Slf4j
public class BundleController {

    @Autowired
    private BundleService bundleService;

    /**
     * 新增组合箱。内含商品随箱一并提交；新组合箱一律为「停售」状态
     * @param bundleDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增组合箱")
    public Result<Void> add(@RequestBody BundleDTO bundleDTO) {
        bundleService.add(bundleDTO);
        return Result.success();
    }

    /**
     * 分页查询。名称模糊匹配，分类与状态为精确筛选
     * @param queryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("组合箱分页查询")
    public Result<PageResult> page(BundlePageQueryDTO queryDTO) {
        return Result.success(bundleService.pageQuery(queryDTO));
    }

    /**
     * 批量删除，ids 为逗号分隔的字符串（契约 2.7.3）
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除组合箱")
    public Result<Void> delete(String ids) {
        bundleService.deleteBatch(GoodsController.parseIds(ids));
        return Result.success();
    }

    /**
     * 按 id 查询组合箱详情，含内含商品列表
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询组合箱")
    public Result<BundleVO> getById(@PathVariable Long id) {
        return Result.success(bundleService.getById(id));
    }

    /**
     * 修改组合箱。内含商品按全量覆盖，整箱温区重新推导
     * @param bundleDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改组合箱")
    public Result<Void> update(@RequestBody BundleDTO bundleDTO) {
        bundleService.update(bundleDTO);
        return Result.success();
    }

    /**
     * 起售 / 停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("组合箱起售或停售")
    public Result<Void> setStatus(@PathVariable Integer status, Long id) {
        bundleService.setStatus(status, id);
        return Result.success();
    }
}
