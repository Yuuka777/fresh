package com.xianhe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xianhe.constant.StatusConstant;
import com.xianhe.dto.GoodsCategoryDTO;
import com.xianhe.dto.GoodsCategoryPageQueryDTO;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.exception.CategoryBeRelatedException;
import com.xianhe.mapper.BundleMapper;
import com.xianhe.mapper.GoodsCategoryMapper;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.result.PageResult;
import com.xianhe.service.GoodsCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类业务实现
 */
@Service
@Slf4j
public class GoodsCategoryServiceImpl implements GoodsCategoryService {

    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private BundleMapper bundleMapper;

    @Override
    public void add(GoodsCategoryDTO categoryDTO) {
        GoodsCategory category = new GoodsCategory();
        BeanUtils.copyProperties(categoryDTO, category);
        // 带状态的数据新增后一律禁用（2026-09-24 约定，与店员新增一致）：
        // 契约 2.2.1 的入参本就不含 status，这里明确固定为禁用值。
        category.setStatus(StatusConstant.DISABLE);

        // 名称重复由 goods_category 的唯一索引兜住，转成「xxx已存在」
        goodsCategoryMapper.insert(category);

        log.info("新增分类成功：id={}，type={}，name={}，sort={}",
                category.getId(), category.getType(), category.getName(), category.getSort());
    }

    @Override
    public PageResult pageQuery(GoodsCategoryPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        List<GoodsCategory> list = goodsCategoryMapper.pageQuery(queryDTO);
        Page<GoodsCategory> page = (Page<GoodsCategory>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除分类
     * @param id
     */
    @Override
    @Transactional
    public void delete(Long id) {
        // 加事务是让「关联校验 + 删除」落在同一个事务里。注意 MySQL 默认隔离级别下
        // 这里的 count 是不加锁的快照读，极端并发下仍可能在校验通过后有人插入商品；
        // 要彻底堵住需要 SELECT ... FOR UPDATE，本项目的数据规模下不做这层加码。
        int goodsCount = goodsMapper.countByCategoryId(id);
        int bundleCount = bundleMapper.countByCategoryId(id);
        if (goodsCount > 0 || bundleCount > 0) {
            log.warn("分类删除被拒：id={}，关联商品 {} 个，关联组合箱 {} 个", id, goodsCount, bundleCount);
            throw new CategoryBeRelatedException();
        }

        goodsCategoryMapper.deleteById(id);
        log.info("删除分类成功：id={}", id);
    }

    @Override
    public void update(GoodsCategoryDTO categoryDTO) {
        GoodsCategory category = new GoodsCategory();
        BeanUtils.copyProperties(categoryDTO, category);
        // Mapper 中为动态更新：type 与 status 不会被本次修改触及
        goodsCategoryMapper.update(category);

        log.info("修改分类成功：id={}，name={}，sort={}",
                categoryDTO.getId(), categoryDTO.getName(), categoryDTO.getSort());
    }

    @Override
    public void setStatus(Long id, Integer status) {
        GoodsCategory category = new GoodsCategory();
        category.setId(id);
        category.setStatus(status);
        goodsCategoryMapper.updateStatus(category);

        log.info("分类状态已变更：id={}，status={}", id, status);
    }

    @Override
    public List<GoodsCategory> listByType(Integer type) {
        return goodsCategoryMapper.listByType(type);
    }

    @Override
    public List<GoodsCategory> listEnabledByType(Integer type) {
        return goodsCategoryMapper.listEnabledByType(type);
    }
}
