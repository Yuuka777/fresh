package com.xianhe.mapper;

import com.xianhe.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收货地址持久层
 */
@Mapper
public interface AddressMapper {

    /**
     * 查询某会员的全部地址，默认地址排在最前
     * @param memberId
     * @return
     */
    List<Address> listByMemberId(Long memberId);

    /**
     * 按主键查询，不区分归属（归属校验在 Service 层做）
     * @param id
     * @return
     */
    Address getById(Long id);

    /**
     * 查询某会员的默认地址
     * @param memberId
     * @return
     */
    Address getDefaultByMemberId(Long memberId);

    /**
     * 统计某会员的地址条数，用于判断是不是第一条（第一条自动置默认）
     * @param memberId
     * @return
     */
    int countByMemberId(Long memberId);

    /**
     * 新增
     * @param address
     */
    void insert(Address address);

    /**
     * 修改，动态更新（只写提交的字段）
     * @param address
     */
    void update(Address address);

    /**
     * 设置某条地址的默认标记
     * @param id
     * @param isDefault
     */
    void updateIsDefaultById(@Param("id") Long id, @Param("isDefault") Integer isDefault);

    /**
     * 把某会员名下所有地址的默认标记清零，设默认前的互斥处理
     * @param memberId
     */
    void clearDefaultByMemberId(Long memberId);

    /**
     * 按主键删除
     * @param id
     */
    void deleteById(Long id);
}
