package com.xianhe.mapper;

import com.xianhe.annotation.AutoFill;
import com.xianhe.dto.GoodsPageQueryDTO;
import com.xianhe.entity.Goods;
import com.xianhe.enumeration.OperationType;
import com.xianhe.vo.GoodsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品持久层（表 goods）
 */
public interface GoodsMapper {

    /**
     * 统计某分类下的商品数量
     * @param categoryId
     * @return
     */
    int countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 按主键查询商品（含 status、温区等业务字段，供上架/下架与组合箱回填使用）
     * @param id
     * @return
     */
    Goods getById(Long id);

    /**
     * 按主键查询商品出参，联表带出分类名称
     * @param id
     * @return
     */
    GoodsVO getVoById(Long id);

    /**
     * 分页查询，配合 PageHelper 使用（调用方在此之前调用 startPage）
     * @param query
     * @return
     */
    List<GoodsVO> pageQuery(GoodsPageQueryDTO query);

    /**
     * 按分类查询商品，供管理端「新增/修改组合箱」的商品选择弹层使用
     * @param categoryId
     * @return
     */
    List<Goods> listByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 按主键批量查询商品
     * @param ids
     * @return
     */
    List<Goods> listByIds(@Param("ids") List<Long> ids);

    /**
     * 统计这批商品中处于**上架**状态的数量，用于删除前的校验
     * @param ids
     * @return
     */
    int countOnSaleByIds(@Param("ids") List<Long> ids);

    /**
     * 新增商品，回填自增主键（组合箱保存时需要立刻拿到商品主键做校验）
     * @param goods
     */
    @AutoFill(OperationType.INSERT)
    void insert(Goods goods);

    /**
     * 修改商品，动态更新（只写提交的字段）
     * @param goods
     */
    @AutoFill(OperationType.UPDATE)
    void update(Goods goods);

    /**
     * 上架 / 下架
     * @param goods
     */
    @AutoFill(OperationType.UPDATE)
    void updateStatus(Goods goods);

    /**
     * 按主键批量物理删除
     * @param ids
     * @return
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    //—— 用户端（只看得上架商品）——

    /**
     * 查某分类下的上架商品
     * @param categoryId
     * @return
     */
    List<Goods> listOnSaleByCategoryId(Long categoryId);

    /**
     * 按名称模糊查上架商品，与分类无关
     * @param name
     * @return
     */
    List<Goods> searchOnSaleByName(String name);

    /**
     * 按状态统计商品数量，工作台商品总览用
     * @param status
     * @return
     */
    Integer countByStatus(@Param("status") Integer status);
}
