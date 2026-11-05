package com.xianhe.service.impl;

import com.xianhe.constant.DefaultFlagConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.dto.AddressDTO;
import com.xianhe.entity.Address;
import com.xianhe.exception.AddressDefaultNotFoundException;
import com.xianhe.exception.AddressNotFoundException;
import com.xianhe.exception.AddressRequiredFieldException;
import com.xianhe.mapper.AddressMapper;
import com.xianhe.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 收货地址业务实现
 */
@Service
@Slf4j
public class AddressServiceImpl implements AddressService {

    //手机号：1 开头的 11 位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> list() {
        return addressMapper.listByMemberId(BaseContext.getCurrentId());
    }

    @Override
    @Transactional
    public void save(AddressDTO addressDTO) {
        Long memberId = BaseContext.getCurrentId();
        assertRequired(addressDTO.getConsignee(), addressDTO.getPhone(), addressDTO.getDetail());

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setMemberId(memberId);
        address.setIsDefault(DefaultFlagConstant.NO);

        if (addressMapper.countByMemberId(memberId) == 0) {
            // 第一条地址自动成为默认：否则新会员下第一单时没有可用的默认地址
            address.setIsDefault(DefaultFlagConstant.YES);
        } else if (DefaultFlagConstant.YES.equals(addressDTO.getIsDefault())) {
            // 默认地址互斥：同一会员至多一条（契约 8.2）
            address.setIsDefault(DefaultFlagConstant.YES);
            addressMapper.clearDefaultByMemberId(memberId);
        }

        addressMapper.insert(address);

        log.info("新增收货地址成功：memberId={}，addressId={}，isDefault={}",
                memberId, address.getId(), address.getIsDefault());
    }

    @Override
    public Address getById(Long id) {
        return requireOwned(id);
    }

    @Override
    @Transactional
    public void update(AddressDTO addressDTO) {
        Long memberId = BaseContext.getCurrentId();
        Address exists = requireOwned(addressDTO.getId());

        // 动态更新下允许只提交要改的字段：未提交的必填项按原值参与校验
        assertRequired(
                addressDTO.getConsignee() != null ? addressDTO.getConsignee() : exists.getConsignee(),
                addressDTO.getPhone() != null ? addressDTO.getPhone() : exists.getPhone(),
                addressDTO.getDetail() != null ? addressDTO.getDetail() : exists.getDetail());

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);

        // 提交 1 才做互斥（先把别的清掉）；提交 0 只取消这一条；不提交则不动（动态更新）
        if (DefaultFlagConstant.YES.equals(addressDTO.getIsDefault())) {
            addressMapper.clearDefaultByMemberId(memberId);
        }

        addressMapper.update(address);

        log.info("修改收货地址成功：memberId={}，addressId={}", memberId, address.getId());
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Long memberId = BaseContext.getCurrentId();
        requireOwned(id);

        // 先清后设，两步在同一事务里，中途不会出现两条默认
        addressMapper.clearDefaultByMemberId(memberId);
        addressMapper.updateIsDefaultById(id, DefaultFlagConstant.YES);

        log.info("设置默认地址：memberId={}，addressId={}", memberId, id);
    }

    @Override
    public void deleteById(Long id) {
        Long memberId = BaseContext.getCurrentId();
        requireOwned(id);
        addressMapper.deleteById(id);

        log.info("删除收货地址：memberId={}，addressId={}", memberId, id);
    }

    @Override
    public Address getDefault() {
        Address address = addressMapper.getDefaultByMemberId(BaseContext.getCurrentId());
        if (address == null) {
            // 契约 8.7：没有默认地址时给出明确文案，由前端提示去选一个
            throw new AddressDefaultNotFoundException();
        }
        return address;
    }

    // 归属校验：查不到、或查到的是别人的地址，都按「不存在」处理 —— 不给外人用 id 探测地址是否存在
    private Address requireOwned(Long id) {
        Address address = id == null ? null : addressMapper.getById(id);
        if (address == null || !address.getMemberId().equals(BaseContext.getCurrentId())) {
            log.warn("地址不存在或不属于当前会员：addressId={}，memberId={}", id, BaseContext.getCurrentId());
            throw new AddressNotFoundException();
        }
        return address;
    }

    // 三个必填项一次校验完：收货人、手机号、详细地址（前端也校验，这里是防绕过）
    private void assertRequired(String consignee, String phone, String detail) {
        if (!StringUtils.hasText(consignee)) {
            throw AddressRequiredFieldException.consigneeRequired();
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw AddressRequiredFieldException.phoneInvalid();
        }
        if (!StringUtils.hasText(detail)) {
            throw AddressRequiredFieldException.detailRequired();
        }
    }
}
