package com.xianhe.controller.user;

import com.xianhe.dto.AddressDTO;
import com.xianhe.entity.Address;
import com.xianhe.result.Result;
import com.xianhe.service.AddressService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址用户端接口（《接口文档-用户端》8）
 */
@RestController("userAddressController")
@RequestMapping("/user/address")
@Api(tags = "用户端 · 收货地址")
@Slf4j
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 查询当前会员的全部地址，默认地址排在最前
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询地址列表")
    public Result<List<Address>> list() {
        return Result.success(addressService.list());
    }

    /**
     * 新增地址：该会员的第一条自动成为默认
     * @param addressDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result<Void> save(@RequestBody AddressDTO addressDTO) {
        addressService.save(addressDTO);
        return Result.success();
    }

    /**
     * 按 id 查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询地址")
    public Result<Address> getById(@PathVariable Long id) {
        return Result.success(addressService.getById(id));
    }

    /**
     * 修改地址
     * @param addressDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改地址")
    public Result<Void> update(@RequestBody AddressDTO addressDTO) {
        addressService.update(addressDTO);
        return Result.success();
    }

    /**
     * 设置默认地址：同一会员至多一条默认
     * @param addressDTO
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result<Void> setDefault(@RequestBody AddressDTO addressDTO) {
        addressService.setDefault(addressDTO.getId());
        return Result.success();
    }

    /**
     * 按 id 删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除地址")
    public Result<Void> deleteById(@RequestParam Long id) {
        addressService.deleteById(id);
        return Result.success();
    }

    /**
     * 查询默认地址，没有默认时返回「没有查询到默认地址」
     * @return
     */
    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<Address> getDefault() {
        return Result.success(addressService.getDefault());
    }
}
