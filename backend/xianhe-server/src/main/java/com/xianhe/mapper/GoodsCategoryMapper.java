package com.xianhe.mapper;

import com.xianhe.annotation.AutoFill;
import com.xianhe.dto.GoodsCategoryPageQueryDTO;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.enumeration.OperationType;

import java.util.List;

/**
 * 商品分类持久层（表 goods_category）
 */
public interface GoodsCategoryMapper {

    /**
     * 按主键查询
     * @param id
     * @return
     */
    GoodsCategory getById(Long id);

    /**
     * 分页查询，配合 PageHelper 使用
     * @param query
     * @return
     */
    List<GoodsCategory> pageQuery(GoodsCategoryPageQueryDTO query);

    /**
     * 按类型查询全部分类，按 sort 升序，不过滤 status（契约 2.2.6）
     * @param type
     * @return
     */
    List<GoodsCategory> listByType(Integer type);

    /**
     * 新增分类
     * @param category
     */
    @AutoFill(OperationType.INSERT)
    void insert(GoodsCategory category);

    /**
     * 修改分类，只更新入参中非空的字段
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void update(GoodsCategory category);

    /**
     * 启用 / 禁用分类
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void updateStatus(GoodsCategory category);

    /**
     * 按主键删除
     * @param id
     */
    void deleteById(Long id);

    /**
     * 用户端：只查启用中的分类，按 sort 升序
     * @param type
     * @return
     */
    List<GoodsCategory> listEnabledByType(Integer type);
}
