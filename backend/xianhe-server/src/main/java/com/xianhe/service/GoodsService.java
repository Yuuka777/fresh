package com.xianhe.service;

import com.xianhe.dto.GoodsDTO;
import com.xianhe.dto.GoodsPageQueryDTO;
import com.xianhe.entity.Goods;
import com.xianhe.result.PageResult;
import com.xianhe.vo.GoodsVO;

import java.util.List;

/**
 * 商品业务接口（《接口文档》2.6，共 7 个接口）
 */
public interface GoodsService {

    /**
     * 新增商品（契约 2.6.1）
     * @param goodsDTO
     */
    void add(GoodsDTO goodsDTO);

    /**
     * 分页查询（契约 2.6.2），按更新时间倒序
     * @param queryDTO
     * @return
     */
    PageResult pageQuery(GoodsPageQueryDTO queryDTO);

    /**
     * 批量删除（契约 2.6.3）
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 按 id 查询商品详情（契约 2.6.4），含规格列表
     * @param id
     * @return
     */
    GoodsVO getById(Long id);

    /**
     * 修改商品（契约 2.6.5）
     * @param goodsDTO
     */
    void update(GoodsDTO goodsDTO);

    /**
     * 按分类查询商品列表（契约 2.6.6），供组合箱的商品选择弹层使用
     * @param categoryId
     * @return
     */
    List<Goods> listByCategoryId(Long categoryId);

    /**
     * 上架 / 下架（契约 2.6.7）
     * @param status
     * @param id
     */
    void setStatus(Integer status, Long id);
}
