package com.xianhe.service;

import com.xianhe.dto.AddressDTO;
import com.xianhe.entity.Address;

import java.util.List;

/**
 * 收货地址业务接口
 */
public interface AddressService {

    /**
     * 查询当前会员的全部地址
     * @return
     */
    List<Address> list();

    /**
     * 新增地址
     * @param addressDTO
     */
    void save(AddressDTO addressDTO);

    /**
     * 按 id 查询地址，限当前会员
     * @param id
     * @return
     */
    Address getById(Long id);

    /**
     * 修改地址
     * @param addressDTO
     */
    void update(AddressDTO addressDTO);

    /**
     * 设为默认地址
     * @param id
     */
    void setDefault(Long id);

    /**
     * 按 id 删除地址
     * @param id
     */
    void deleteById(Long id);

    /**
     * 查询当前会员的默认地址
     * @return
     */
    Address getDefault();
}
