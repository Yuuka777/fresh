package com.xianhe.service;

import com.xianhe.dto.PasswordEditDTO;
import com.xianhe.dto.StaffDTO;
import com.xianhe.dto.StaffLoginDTO;
import com.xianhe.dto.StaffPageQueryDTO;
import com.xianhe.entity.Staff;
import com.xianhe.result.PageResult;
import com.xianhe.vo.StaffLoginVO;

/**
 * 店员业务接口（《接口文档》2.1）
 */
public interface StaffService {

    /**
     * 登录：校验账号、密码、启用状态，通过后签发管理端令牌
     * @param loginDTO
     * @return
     */
    StaffLoginVO login(StaffLoginDTO loginDTO);

    /**
     * 新增店员，初始密码由服务端固定写入
     * @param staffDTO
     */
    void add(StaffDTO staffDTO);

    /**
     * 分页查询店员，按创建时间倒序，不返回密码
     * @param queryDTO
     * @return
     */
    PageResult pageQuery(StaffPageQueryDTO queryDTO);

    /**
     * 按 id 查询店员，不返回密码
     * @param id
     * @return
     */
    Staff getById(Long id);

    /**
     * 编辑店员信息（不含密码、状态）
     * @param staffDTO
     */
    void update(StaffDTO staffDTO);

    /**
     * 修改密码：校验原密码后写入新密码
     * @param passwordEditDTO
     */
    void editPassword(PasswordEditDTO passwordEditDTO);

    /**
     * 启用 / 禁用账号
     * @param id
     * @param status
     */
    void setStatus(Long id, Integer status);

    /**
     * 删除店员（物理删除）
     * @param id
     */
    void delete(Long id);
}
