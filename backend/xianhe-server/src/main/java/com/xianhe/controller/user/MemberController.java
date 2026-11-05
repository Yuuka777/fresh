package com.xianhe.controller.user;

import com.xianhe.dto.MemberLoginDTO;
import com.xianhe.result.Result;
import com.xianhe.service.MemberService;
import com.xianhe.vo.MemberLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员用户端接口（《接口文档-用户端》2）
 */
@RestController("userMemberController")
@RequestMapping("/user/member")
@Api(tags = "用户端 · 会员")
@Slf4j
public class MemberController {

    @Autowired
    private MemberService memberService;

    /**
     * 微信登录：小程序只传 code，openid 由服务端换取；新用户自动注册
     * @param memberLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<MemberLoginVO> login(@RequestBody MemberLoginDTO memberLoginDTO) {
        return Result.success(memberService.login(memberLoginDTO));
    }
}
