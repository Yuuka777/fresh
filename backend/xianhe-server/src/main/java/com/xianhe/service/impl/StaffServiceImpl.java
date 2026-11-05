package com.xianhe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xianhe.constant.JwtClaimsConstant;
import com.xianhe.constant.PasswordConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.dto.PasswordEditDTO;
import com.xianhe.dto.StaffDTO;
import com.xianhe.dto.StaffLoginDTO;
import com.xianhe.dto.StaffPageQueryDTO;
import com.xianhe.entity.Staff;
import com.xianhe.exception.AccountLockedException;
import com.xianhe.exception.AccountNotFoundException;
import com.xianhe.exception.PasswordEditFailedException;
import com.xianhe.exception.PasswordErrorException;
import com.xianhe.exception.SelfDeleteException;
import com.xianhe.exception.StaffEnabledDeleteException;
import com.xianhe.mapper.StaffMapper;
import com.xianhe.properties.JwtProperties;
import com.xianhe.result.PageResult;
import com.xianhe.service.StaffService;
import com.xianhe.utils.JwtUtil;
import com.xianhe.utils.PasswordUtil;
import com.xianhe.vo.StaffLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店员业务实现
 */
@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public StaffLoginVO login(StaffLoginDTO loginDTO) {
        // 1. 查账号。查不到与密码错误分开提示，是契约 1.4 明确要求的（便于运营自助排查）
        Staff staff = staffMapper.getByUsername(loginDTO.getUsername());
        if (staff == null) {
            throw new AccountNotFoundException();
        }

        // 2. 比对密码。库中存 PBKDF2 哈希；老数据仍是 MD5，先兼容，登录成功后立即升级
        if (!PasswordUtil.matches(loginDTO.getPassword(), staff.getPassword())) {
            throw new PasswordErrorException();
        }
        if (!PasswordUtil.isModern(staff.getPassword())) {
            Staff upgrade = new Staff();
            upgrade.setId(staff.getId());
            upgrade.setPassword(PasswordUtil.encode(loginDTO.getPassword()));
            staffMapper.updatePassword(upgrade);
            log.info("口令哈希已升级为 PBKDF2：staffId={}", staff.getId());
        }

        // 3. 校验启用状态。放在密码校验之后：密码不对的人不应该知道账号是否被禁用
        if (StatusConstant.DISABLE.equals(staff.getStatus())) {
            throw new AccountLockedException();
        }

        // 4. 签发管理端令牌。载荷只放 id 与用户名，其余信息业务层需要时回库查
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.STAFF_ID, staff.getId());
        claims.put(JwtClaimsConstant.USERNAME, staff.getUsername());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        log.info("店员登录成功：id={}，username={}", staff.getId(), staff.getUsername());
        return new StaffLoginVO(staff.getId(), staff.getUsername(), staff.getName(), token);
    }

    @Override
    public void add(StaffDTO staffDTO) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDTO, staff);

        // 初始密码由服务端固定，前端表单不提供该字段（契约 2.1.3）
        staff.setPassword(PasswordUtil.encode(PasswordConstant.DEFAULT_PASSWORD));
        // 带状态的数据新增后一律禁用（2026-09-24 约定）：先存下来，
        // 由使用者在列表中核对无误后再启用，避免「新建即生效」带来的误操作。
        // 因此这里不接受 DTO 传入的状态，StaffDTO 也不再有 status 字段。
        staff.setStatus(StatusConstant.DISABLE);

        // 用户名重复由 staff 表的唯一索引兜住，GlobalExceptionHandler 会转成「xxx已存在」
        staffMapper.insert(staff);

        log.info("新增店员成功（初始为禁用）：id={}，username={}", staff.getId(), staff.getUsername());
    }

    @Override
    public PageResult pageQuery(StaffPageQueryDTO queryDTO) {
        // PageHelper 会拦截紧随其后的第一条查询，自动改写为分页语句并统计总数
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        List<Staff> list = staffMapper.pageQuery(queryDTO);
        Page<Staff> page = (Page<Staff>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public Staff getById(Long id) {
        // 查不到返回 null，由前端决定展示方式（个人主页会回退为「—」）
        return staffMapper.getById(id);
    }

    @Override
    public void update(StaffDTO staffDTO) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDTO, staff);
        // Mapper 中为动态更新，未提交的字段（状态、密码）不会被覆盖
        staffMapper.update(staff);

        log.info("编辑店员成功：id={}", staffDTO.getId());
    }

    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        String stored = staffMapper.selectPasswordById(passwordEditDTO.getEmpId());
        if (stored == null) {
            throw new AccountNotFoundException();
        }
        if (!PasswordUtil.matches(passwordEditDTO.getOldPassword(), stored)) {
            throw new PasswordEditFailedException();
        }

        Staff staff = new Staff();
        staff.setId(passwordEditDTO.getEmpId());
        staff.setPassword(PasswordUtil.encode(passwordEditDTO.getNewPassword()));
        staffMapper.updatePassword(staff);

        log.info("店员修改密码成功：id={}", passwordEditDTO.getEmpId());
    }

    @Override
    public void setStatus(Long id, Integer status) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setStatus(status);
        staffMapper.updateStatus(staff);

        log.info("店员状态已变更：id={}，status={}", id, status);
    }

    /**
     * 删除店员（物理删除）
     * @param id
     */
    @Override
    public void delete(Long id) {
        // 两项校验的顺序是有意的：先判「是不是自己」，再判「启用中」。
        // 若反过来，操作人删自己时会先看到「启用中的店员不能删除」，
        // 而被这个提示引去禁用自己再删 —— 那时他已经无法登录了。
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null && currentId.equals(id)) {
            throw new SelfDeleteException();
        }

        Staff staff = staffMapper.getById(id);
        if (staff == null) {
            throw new AccountNotFoundException();
        }
        if (StatusConstant.ENABLE.equals(staff.getStatus())) {
            throw new StaffEnabledDeleteException();
        }

        staffMapper.deleteById(id);
        log.info("删除店员成功：id={}，username={}", id, staff.getUsername());
    }

}
