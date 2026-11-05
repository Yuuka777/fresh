package com.xianhe.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianhe.constant.JwtClaimsConstant;
import com.xianhe.dto.MemberLoginDTO;
import com.xianhe.entity.Member;
import com.xianhe.exception.WxLoginFailedException;
import com.xianhe.json.JacksonObjectMapper;
import com.xianhe.mapper.MemberMapper;
import com.xianhe.properties.JwtProperties;
import com.xianhe.properties.WeChatProperties;
import com.xianhe.service.MemberService;
import com.xianhe.utils.HttpClientUtil;
import com.xianhe.utils.JwtUtil;
import com.xianhe.vo.MemberLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 会员业务实现
 */
@Service
@Slf4j
public class MemberServiceImpl implements MemberService {

    private static final ObjectMapper MAPPER = new JacksonObjectMapper();

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public MemberLoginVO login(MemberLoginDTO memberLoginDTO) {
        String openid = getOpenid(memberLoginDTO.getCode());
        if (openid == null) {
            throw new WxLoginFailedException();
        }

        // 老会员直接取，新会员自动注册（小程序侧不做任何本地账号兜底）
        Member member = memberMapper.getByOpenid(openid);
        if (member == null) {
            member = new Member();
            member.setOpenid(openid);
            member.setCreateTime(LocalDateTime.now());
            memberMapper.insert(member);
            log.info("新会员自动注册：id={}", member.getId());
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.MEMBER_ID, member.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        MemberLoginVO vo = new MemberLoginVO();
        vo.setId(member.getId());
        vo.setOpenid(member.getOpenid());
        vo.setToken(token);

        log.info("会员登录成功：id={}", member.getId());
        return vo;
    }

    // code 换 openid。微信侧失败（code 过期、AppID 不符）与网络不可达都归为「登录失败」
    private String getOpenid(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            String url = weChatProperties.getJscode2sessionUrl()
                    + "?appid=" + weChatProperties.getAppid()
                    + "&secret=" + weChatProperties.getSecret()
                    + "&js_code=" + HttpClientUtil.encode(code)
                    + "&grant_type=authorization_code";

            String json = HttpClientUtil.doGet(url);
            JsonNode node = MAPPER.readTree(json);

            // 成功时返回 {"openid":"...","session_key":"..."}，失败时返回 errcode/errmsg
            if (node.hasNonNull("openid")) {
                return node.get("openid").asText();
            }
            log.warn("微信换取 openid 失败：{}", json);
            return null;
        } catch (Exception ex) {
            log.error("调用微信接口异常", ex);
            return null;
        }
    }
}
