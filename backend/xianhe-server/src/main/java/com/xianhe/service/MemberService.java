package com.xianhe.service;

import com.xianhe.dto.MemberLoginDTO;
import com.xianhe.vo.MemberLoginVO;

/**
 * 会员业务接口（《接口文档-用户端》2）
 */
public interface MemberService {

    /**
     * 微信登录：code 换 openid，没有则自动注册，最后签发用户端令牌
     * @param memberLoginDTO
     * @return
     */
    MemberLoginVO login(MemberLoginDTO memberLoginDTO);
}
