package com.xianhe.mapper;

import com.xianhe.annotation.AutoFill;
import com.xianhe.dto.BundlePageQueryDTO;
import com.xianhe.entity.Bundle;
import com.xianhe.enumeration.OperationType;
import com.xianhe.vo.BundleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组合箱持久层（表 bundle）
 */
public interface BundleMapper {

    /**
     * 统计某分类下的组合箱数量
     * @param categoryId
     * @return
     */
    int countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 按主键查询组合箱（含 status）
     * @param id
     * @return
     */
    Bundle getById(Long id);

    /**
     * 按主键查询组合箱出参，联表带出分类名称
     * @param id
     * @return
     */
    BundleVO getVoById(Long id);

    /**
     * 分页查询，配合 PageHelper 使用。不查内含商品（契约 2.7.2）
     * @param query
     * @return
     */
    List<BundleVO> pageQuery(BundlePageQueryDTO query);

    /**
     * 按主键批量查询组合箱
     * @param ids
     * @return
     */
    List<Bundle> listByIds(@Param("ids") List<Long> ids);

    /**
     * 统计这批组合箱中处于**起售**状态的数量，用于删除前的校验
     * @param ids
     * @return
     */
    int countOnSaleByIds(@Param("ids") List<Long> ids);

    /**
     * 新增组合箱，回填自增主键（紧接着要写入内含商品）
     * @param bundle
     */
    @AutoFill(OperationType.INSERT)
    void insert(Bundle bundle);

    /**
     * 修改组合箱，动态更新（只写提交的字段）
     * @param bundle
     */
    @AutoFill(OperationType.UPDATE)
    void update(Bundle bundle);

    /**
     * 起售 / 停售
     * @param bundle
     */
    @AutoFill(OperationType.UPDATE)
    void updateStatus(Bundle bundle);

    /**
     * 按主键批量物理删除
     * @param ids
     * @return
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 用户端：查某分类下的起售组合箱
     * @param categoryId
     * @return
     */
    List<Bundle> listOnSaleByCategoryId(Long categoryId);

    /**
     * 按状态统计组合箱数量，工作台组合箱总览用
     * @param status
     * @return
     */
    Integer countByStatus(@Param("status") Integer status);
}
