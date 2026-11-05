package com.xianhe.service;

import com.xianhe.dto.BundleDTO;
import com.xianhe.dto.BundlePageQueryDTO;
import com.xianhe.result.PageResult;
import com.xianhe.vo.BundleVO;

import java.util.List;

/**
 * 组合箱业务接口（《接口文档》2.7，共 6 个接口）
 */
public interface BundleService {

    /**
     * 新增组合箱（契约 2.7.1）
     * @param bundleDTO
     */
    void add(BundleDTO bundleDTO);

    /**
     * 分页查询（契约 2.7.2），按更新时间倒序
     * @param queryDTO
     * @return
     */
    PageResult pageQuery(BundlePageQueryDTO queryDTO);

    /**
     * 批量删除（契约 2.7.3）
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 按 id 查询组合箱详情（契约 2.7.4），含内含商品列表
     * @param id
     * @return
     */
    BundleVO getById(Long id);

    /**
     * 修改组合箱（契约 2.7.5）
     * @param bundleDTO
     */
    void update(BundleDTO bundleDTO);

    /**
     * 起售 / 停售（契约 2.7.6）
     * @param status
     * @param id
     */
    void setStatus(Integer status, Long id);
}
