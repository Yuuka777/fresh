package com.xianhe.service;

import com.xianhe.dto.GoodsCategoryDTO;
import com.xianhe.dto.GoodsCategoryPageQueryDTO;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.result.PageResult;

import java.util.List;

/**
 * 商品分类业务接口（《接口文档》2.2）
 */
public interface GoodsCategoryService {

    /**
     * 新增分类，状态默认为启用
     * @param categoryDTO
     */
    void add(GoodsCategoryDTO categoryDTO);

    /**
     * 分页查询分类，不过滤状态
     * @param queryDTO
     * @return
     */
    PageResult pageQuery(GoodsCategoryPageQueryDTO queryDTO);

    /**
     * 删除分类。分类下存在商品或组合箱时拒绝
     * @param id
     */
    void delete(Long id);

    /**
     * 修改分类的名称与排序值
     * @param categoryDTO
     */
    void update(GoodsCategoryDTO categoryDTO);

    /**
     * 启用 / 禁用分类
     * @param id
     * @param status
     */
    void setStatus(Long id, Integer status);

    /**
     * 按类型查询全部分类（按 sort 升序，不过滤状态），供表单下拉框使用
     * @param type
     * @return
     */
    List<GoodsCategory> listByType(Integer type);

    /**
     * 用户端：只查启用中的分类（按 sort 升序）
     * @param type
     * @return
     */
    List<GoodsCategory> listEnabledByType(Integer type);
}
