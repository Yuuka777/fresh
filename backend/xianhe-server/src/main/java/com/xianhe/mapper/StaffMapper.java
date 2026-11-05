package com.xianhe.mapper;

import com.xianhe.annotation.AutoFill;
import com.xianhe.dto.StaffPageQueryDTO;
import com.xianhe.entity.Staff;
import com.xianhe.enumeration.OperationType;

import java.util.List;

/**
 * 店员持久层（表 staff）
 */
public interface StaffMapper {

    /**
     * 按用户名查询，**含密码**，仅登录流程使用
     * @param username
     * @return
     */
    Staff getByUsername(String username);

    /**
     * 按主键查询，不含密码
     * @param id
     * @return
     */
    Staff getById(Long id);

    /**
     * 按主键取出密码密文，仅修改密码流程使用
     * @param id
     * @return
     */
    String selectPasswordById(Long id);

    /**
     * 分页查询，配合 PageHelper 使用（调用方在此之前调用 startPage）
     * @param query
     * @return
     */
    List<Staff> pageQuery(StaffPageQueryDTO query);

    /**
     * 新增店员
     * @param staff
     */
    @AutoFill(OperationType.INSERT)
    void insert(Staff staff);

    /**
     * 编辑店员：只更新入参中非空的字段
     * @param staff
     */
    @AutoFill(OperationType.UPDATE)
    void update(Staff staff);

    /**
     * 修改密码
     * @param staff
     */
    @AutoFill(OperationType.UPDATE)
    void updatePassword(Staff staff);

    /**
     * 启用 / 禁用账号
     * @param staff
     */
    @AutoFill(OperationType.UPDATE)
    void updateStatus(Staff staff);

    /**
     * 按主键物理删除
     * @param id
     */
    void deleteById(Long id);
}
