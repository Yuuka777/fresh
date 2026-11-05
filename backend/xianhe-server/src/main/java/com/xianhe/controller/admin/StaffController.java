package com.xianhe.controller.admin;

import com.xianhe.dto.PasswordEditDTO;
import com.xianhe.dto.StaffDTO;
import com.xianhe.dto.StaffLoginDTO;
import com.xianhe.dto.StaffPageQueryDTO;
import com.xianhe.entity.Staff;
import com.xianhe.result.PageResult;
import com.xianhe.result.Result;
import com.xianhe.service.StaffService;
import com.xianhe.vo.StaffLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店员管理端接口（《接口文档》2.1）
 */
@RestController
@RequestMapping("/admin/staff")
@Api(tags = "管理端 · 店员")
@Slf4j
public class StaffController {

    @Autowired
    private StaffService staffService;

    /**
     * 店员登录。放行白名单接口
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("店员登录")
    public Result<StaffLoginVO> login(@RequestBody StaffLoginDTO loginDTO) {
        return Result.success(staffService.login(loginDTO));
    }

    /**
     * 店员退出
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("店员退出")
    public Result<Void> logout() {
        // 令牌是无状态的，服务端不维护会话，因此这里不需要做任何事；
        // 前端收到成功响应后清掉本地 Cookie 即可。保留该接口是为了让前端有明确的
        // 「退出」语义，也便于将来加入令牌黑名单。
        return Result.success();
    }

    /**
     * 新增店员
     * @param staffDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增店员")
    public Result<Void> add(@RequestBody StaffDTO staffDTO) {
        // 初始密码由服务端固定为 123456，账号状态固定为「禁用」——
        // 需在列表中手动启用后该账号才能登录。两项都由服务端决定，前端不必也不应传。
        staffService.add(staffDTO);
        return Result.success();
    }

    /**
     * 分页查询店员，按创建时间倒序，不返回密码
     * @param queryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("店员分页查询")
    public Result<PageResult> page(StaffPageQueryDTO queryDTO) {
        return Result.success(staffService.pageQuery(queryDTO));
    }

    /**
     * 启用 / 禁用账号。id 由查询参数传入（契约 2.1.5）
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用店员账号")
    public Result<Void> setStatus(@PathVariable Integer status, Long id) {
        staffService.setStatus(id, status);
        return Result.success();
    }

    /**
     * 按 id 查询店员，「个人主页」也复用本接口
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询店员")
    public Result<Staff> getById(@PathVariable Long id) {
        // 注意本映射与上面的 /page 同层，Spring 会优先匹配字面量路径，
        // 不会把 "page" 当成 id 解析。
        return Result.success(staffService.getById(id));
    }

    /**
     * 编辑店员信息
     * @param staffDTO
     * @return
     */
    @PutMapping
    @ApiOperation("编辑店员")
    public Result<Void> update(@RequestBody StaffDTO staffDTO) {
        staffService.update(staffDTO);
        return Result.success();
    }

    /**
     * 修改密码，原密码不匹配时返回「原密码错误」
     * @param passwordEditDTO
     * @return
     */
    @PutMapping("/editPassword")
    @ApiOperation("修改密码")
    public Result<Void> editPassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        staffService.editPassword(passwordEditDTO);
        return Result.success();
    }

    /**
     * 删除店员（物理删除）
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除店员")
    public Result<Void> delete(Long id) {
        // 拒绝两种情况：删自己、删仍在启用中的账号。
        staffService.delete(id);
        return Result.success();
    }
}
