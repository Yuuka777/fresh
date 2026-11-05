package com.xianhe.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianhe.constant.JwtClaimsConstant;
import com.xianhe.constant.MessageConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.properties.JwtProperties;
import com.xianhe.result.Result;
import com.xianhe.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户端 JWT 校验拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 非 Controller 方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(jwtProperties.getUserTokenName());
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long memberId = Long.valueOf(claims.get(JwtClaimsConstant.MEMBER_ID).toString());
            BaseContext.setCurrentId(memberId);
            return true;
        } catch (Exception ex) {
            // 与管理端一致：返回 200 + code 0，让小程序走「清本地登录态并重新登录」的分支
            log.warn("用户端令牌校验未通过，请求路径：{}", request.getRequestURI());
            writeLoginStateInvalid(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        // 线程会被容器复用，必须清理，否则下一次请求可能读到上一个人的 id
        BaseContext.removeCurrentId();
    }

    private void writeLoginStateInvalid(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                OBJECT_MAPPER.writeValueAsString(Result.error(MessageConstant.LOGIN_STATE_INVALID)));
    }
}
